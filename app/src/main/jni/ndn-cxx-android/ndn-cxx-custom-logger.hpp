/* -*- Mode:C++; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2013-2017 Regents of the University of California.
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

#ifndef NFD_ANDROID_NDN_CXX_ANDROID_NDN_CXX_CUSTOM_LOGGER_HPP
#define NFD_ANDROID_NDN_CXX_ANDROID_NDN_CXX_CUSTOM_LOGGER_HPP

#include "common.hpp"
#include <sstream>
#include <atomic>
#include <android/log.h>

namespace ndn {
namespace util {

/** \brief indicates the severity level of a log message
 */
enum class LogLevel {
  FATAL   = -1,   ///< fatal (will be logged unconditionally)
  NONE    = 0,    ///< no messages
  ERROR   = 1,    ///< serious error messages
  WARN    = 2,    ///< warning messages
  INFO    = 3,    ///< informational messages
  DEBUG   = 4,    ///< debug messages
  TRACE   = 5,    ///< trace messages (most verbose)
  ALL     = 255   ///< all messages
};

/** \brief output LogLevel as string
 *  \throw std::invalid_argument unknown \p level
 */
std::ostream&
operator<<(std::ostream& os, LogLevel level);

/** \brief parse LogLevel from string
 *  \throw std::invalid_argument unknown level name
 */
LogLevel
parseLogLevel(const std::string& s);

/** \brief represents a logger in logging facility
 *  \note User should declare a new logger with \p NDN_LOG_INIT macro.
 */
class Logger
{
public:
  explicit
  Logger(const std::string& name);

  const std::string&
  getModuleName() const
  {
    return m_moduleName;
  }

  bool
  isLevelEnabled(LogLevel level) const
  {
    return m_currentLevel.load(std::memory_order_relaxed) >= level;
  }

  void
  setLevel(LogLevel level)
  {
    m_currentLevel.store(level, std::memory_order_relaxed);
  }

private:
  const std::string m_moduleName;
  std::atomic<LogLevel> m_currentLevel;
};

/** \brief declare a log module
 */
#define NDN_LOG_INIT(name) \
  namespace { \
    inline ::ndn::util::Logger& getNdnCxxLogger() \
    { \
      static ::ndn::util::Logger logger(BOOST_STRINGIZE(name)); \
      return logger; \
    } \
  } \
  struct ndn_cxx__allow_trailing_semicolon

#define NDN_LOG(level, androidLevel, msg, expression)   \
  do { \
      if (getNdnCxxLogger().isLevelEnabled(::ndn::util::LogLevel::level)) {           \
      std::ostringstream os;                                              \
      os << expression;                                                   \
      __android_log_print(ANDROID_LOG_##androidLevel,                     \
                          getNdnCxxLogger().getModuleName().c_str(), "%s", os.str().c_str()); \
    }                                                                     \
  } while (false)

#define NDN_LOG_TRACE(expression) NDN_LOG(TRACE, VERBOSE, TRACE, expression)
#define NDN_LOG_DEBUG(expression) NDN_LOG(DEBUG, DEBUG, DEBUG,   expression)
#define NDN_LOG_INFO(expression)  NDN_LOG(INFO,  INFO,  INFO,    expression)
#define NDN_LOG_WARN(expression)  NDN_LOG(WARN,  WARN,  WARNING, expression)
#define NDN_LOG_ERROR(expression) NDN_LOG(ERROR, ERROR, ERROR,   expression)
#define NDN_LOG_FATAL(expression) NDN_LOG(FATAL, FATAL, FATAL,   expression)

} // namespace util
} // namespace ndn

#endif // NFD_ANDROID_NDN_CXX_ANDROID_NDN_CXX_CUSTOM_LOGGER_HPP
