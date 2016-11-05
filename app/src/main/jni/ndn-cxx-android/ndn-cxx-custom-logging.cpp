/* -*- Mode:C++; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2013-2016 Regents of the University of California.
 *
 * This file is part of ndn-cxx library (NDN C++ library with eXperimental eXtensions).
 *
 * ndn-cxx library is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ndn-cxx library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received copies of the GNU General Public License and GNU Lesser
 * General Public License along with ndn-cxx, e.g., in COPYING.md file.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * See AUTHORS.md for complete list of ndn-cxx authors and contributors.
 */

#include "ndn-cxx-custom-logging.hpp"
#include "ndn-cxx-custom-logger.hpp"

#include <cstdlib>
#include <sstream>

namespace ndn {
namespace util {

static const LogLevel INITIAL_DEFAULT_LEVEL = LogLevel::NONE;

Logging&
Logging::get()
{
  // Initialization of block-scope variables with static storage duration is thread-safe.
  // See ISO C++ standard [stmt.dcl]/4
  static Logging instance;
  return instance;
}

Logging::Logging()
{
}

void
Logging::addLoggerImpl(Logger& logger)
{
  std::lock_guard<std::mutex> lock(m_mutex);

  const std::string& moduleName = logger.getModuleName();
  m_loggers.insert({moduleName, &logger});

  auto levelIt = m_enabledLevel.find(moduleName);
  if (levelIt == m_enabledLevel.end()) {
    levelIt = m_enabledLevel.find("*");
  }
  LogLevel level = levelIt == m_enabledLevel.end() ? INITIAL_DEFAULT_LEVEL : levelIt->second;
  logger.setLevel(level);
}

void
Logging::setLevelImpl(const std::string& moduleName, LogLevel level)
{
  std::lock_guard<std::mutex> lock(m_mutex);

  if (moduleName == "*") {
    this->setDefaultLevel(level);
    return;
  }

  m_enabledLevel[moduleName] = level;
  auto range = m_loggers.equal_range(moduleName);
  for (auto i = range.first; i != range.second; ++i) {
    i->second->setLevel(level);
  }
}

void
Logging::setDefaultLevel(LogLevel level)
{
  m_enabledLevel.clear();
  m_enabledLevel["*"] = level;

  for (auto i = m_loggers.begin(); i != m_loggers.end(); ++i) {
    i->second->setLevel(level);
  }
}

void
Logging::setLevelImpl(const std::string& config)
{
  std::stringstream ss(config);
  std::string configModule;
  while (std::getline(ss, configModule, ':')) {
    size_t ind = configModule.find('=');
    if (ind == std::string::npos) {
      BOOST_THROW_EXCEPTION(std::invalid_argument("malformed logging config: '=' is missing"));
    }

    std::string moduleName = configModule.substr(0, ind);
    LogLevel level = parseLogLevel(configModule.substr(ind+1));

    this->setLevelImpl(moduleName, level);
  }
}

} // namespace util
} // namespace ndn
