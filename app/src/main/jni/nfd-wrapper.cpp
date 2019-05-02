/* -*- Mode:C++; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2015-2019 Regents of the University of California
 *
 * This file is part of NFD (Named Data Networking Forwarding Daemon) Android.
 * See AUTHORS.md for complete list of NFD Android authors and contributors.
 *
 * NFD Android is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * NFD Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * NFD Android, e.g., in COPYING.md file.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "nfd-wrapper.hpp"

#include <daemon/common/config-file.hpp>
#include <daemon/common/global.hpp>
#include <daemon/common/logger.hpp>
#include <daemon/common/privilege-helper.hpp>
#include <daemon/nfd.hpp>
#include <daemon/rib/service.hpp>

#include <ndn-cxx/util/exception.hpp>
#include <ndn-cxx/util/logging.hpp>

#include <boost/property_tree/info_parser.hpp>

#include <thread>
#include <mutex>

NFD_LOG_INIT(NfdWrapper);

namespace nfd {


class Runner
{
public:
  Runner()
    : m_io(nullptr)
  {
    std::string initialConfig = R"CONF(
      log
      {
        default_level ALL
      }
      tables
      {
        cs_max_packets 100
        strategy_choice
        {
          /               /localhost/nfd/strategy/best-route
          /localhost      /localhost/nfd/strategy/multicast
          /localhost/nfd  /localhost/nfd/strategy/best-route
          /ndn/broadcast  /localhost/nfd/strategy/multicast
          /ndn/multicast  /localhost/nfd/strategy/multicast
          /sl             /localhost/nfd/strategy/self-learning
        }
      }
      face_system
      {
        tcp
        udp
        websocket
      }
      authorizations
      {
        authorize
        {
          certfile any
          privileges
          {
            faces
            fib
            cs
            strategy-choice
          }
        }
      }
      rib
      {
        localhost_security
        {
          trust-anchor
          {
            type any
          }
        }
        localhop_security
        {
          trust-anchor
          {
            type any
          }
        }
        auto_prefix_propagate
        {
          refresh_interval 300
        }
      }
    )CONF";

    std::istringstream input(initialConfig);
    boost::property_tree::read_info(input, m_config);

    // now, the start procedure can update config if needed
  }

  void
  finishInit()
  {
    m_nfd.reset(new Nfd(m_config, m_keyChain));
    m_ribService.reset(new rib::Service(m_config, m_keyChain));

    m_nfd->initialize();
  }

  void
  run()
  {
    {
      std::unique_lock<std::mutex> lock(m_pointerMutex);
      m_io = &getGlobalIoService();
    }

    setMainIoService(m_io);
    setRibIoService(m_io);

    m_io->run();
    m_io->reset();

    m_ribService.reset();
    m_nfd.reset();

    m_io = nullptr;
  }

  void
  stop()
  {
    std::unique_lock<std::mutex> lock(m_pointerMutex);

    if (m_io != nullptr) {
      m_io->stop();
    }
  }

  nfd::ConfigSection&
  getConfig()
  {
    return m_config;
  }

private:
  std::mutex m_pointerMutex;
  boost::asio::io_service* m_io;
  ndn::KeyChain m_keyChain;
  unique_ptr<Nfd> m_nfd;
  unique_ptr<rib::Service> m_ribService;

  nfd::ConfigSection m_config;
};

static unique_ptr<Runner> g_runner;
static std::thread g_thread;
static std::map<std::string, std::string> g_params;

} // namespace nfd


std::map<std::string, std::string>
getParams(JNIEnv* env, jobject jParams)
{
  std::map<std::string, std::string> params;

  jclass jcMap = env->GetObjectClass(jParams);
  jclass jcSet = env->FindClass("java/util/Set");
  jclass jcIterator = env->FindClass("java/util/Iterator");
  jclass jcMapEntry = env->FindClass("java/util/Map$Entry");

  jmethodID jcMapEntrySet      = env->GetMethodID(jcMap,      "entrySet", "()Ljava/util/Set;");
  jmethodID jcSetIterator      = env->GetMethodID(jcSet,      "iterator", "()Ljava/util/Iterator;");
  jmethodID jcIteratorHasNext  = env->GetMethodID(jcIterator, "hasNext",  "()Z");
  jmethodID jcIteratorNext     = env->GetMethodID(jcIterator, "next",     "()Ljava/lang/Object;");
  jmethodID jcMapEntryGetKey   = env->GetMethodID(jcMapEntry, "getKey",   "()Ljava/lang/Object;");
  jmethodID jcMapEntryGetValue = env->GetMethodID(jcMapEntry, "getValue", "()Ljava/lang/Object;");

  jobject jParamsEntrySet = env->CallObjectMethod(jParams, jcMapEntrySet);
  jobject jParamsIterator = env->CallObjectMethod(jParamsEntrySet, jcSetIterator);
  jboolean bHasNext = env->CallBooleanMethod(jParamsIterator, jcIteratorHasNext);
  while (bHasNext) {
    jobject entry = env->CallObjectMethod(jParamsIterator, jcIteratorNext);

    jstring jKey = (jstring)env->CallObjectMethod(entry, jcMapEntryGetKey);
    jstring jValue = (jstring)env->CallObjectMethod(entry, jcMapEntryGetValue);

    const char* cKey = env->GetStringUTFChars(jKey, nullptr);
    const char* cValue = env->GetStringUTFChars(jValue, nullptr);

    params.insert(std::make_pair(cKey, cValue));

    env->ReleaseStringUTFChars(jKey, cKey);
    env->ReleaseStringUTFChars(jValue, cValue);

    bHasNext = env->CallBooleanMethod(jParamsIterator, jcIteratorHasNext);
  }

  return params;
}


JNIEXPORT void JNICALL
Java_net_named_1data_nfd_service_NfdService_startNfd(JNIEnv* env, jclass, jobject jParams)
{
  if (nfd::g_runner.get() == nullptr) {
    nfd::g_params = getParams(env, jParams);

    // set/update HOME environment variable
    ::setenv("HOME", nfd::g_params["homePath"].c_str(), true);
    NFD_LOG_INFO("Use [" << nfd::g_params["homePath"] << "] as a security storage");

    nfd::g_thread = std::thread([] {
        nfd::resetGlobalIoService();

        NFD_LOG_INFO("Starting NFD...");
        try {
          nfd::g_runner.reset(new nfd::Runner());
          // update config
          for (const auto& pair : nfd::g_params) {
            if (pair.first == "homePath")
              continue;

            nfd::g_runner->getConfig().put(pair.first, pair.second);
          }
          nfd::g_runner->finishInit();

          nfd::g_runner->run();
        }
        catch (const std::exception& e) {
          NFD_LOG_FATAL(boost::diagnostic_information(e));
        }
        catch (const nfd::PrivilegeHelper::Error& e) {
          NFD_LOG_FATAL("PrivilegeHelper: " << e.what());
        }
        catch (...) {
          NFD_LOG_FATAL("Unknown fatal error");
        }

        nfd::g_runner.reset();
        nfd::resetGlobalIoService();
        NFD_LOG_INFO("NFD stopped");
      });
  }
}

JNIEXPORT void JNICALL
Java_net_named_1data_nfd_service_NfdService_stopNfd(JNIEnv*, jclass)
{
  if (nfd::g_runner.get() != nullptr) {
    NFD_LOG_INFO("Stopping NFD...");
    nfd::g_runner->stop();
    // do not block anything
  }
}

JNIEXPORT jboolean JNICALL
Java_net_named_1data_nfd_service_NfdService_isNfdRunning(JNIEnv*, jclass)
{
  return nfd::g_runner.get() != nullptr;
}
