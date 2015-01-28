/* -*- Mode:C++; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2014-2015,  Regents of the University of California,
 *                           Arizona Board of Regents,
 *                           Colorado State University,
 *                           University Pierre & Marie Curie, Sorbonne University,
 *                           Washington University in St. Louis,
 *                           Beijing Institute of Technology,
 *                           The University of Memphis.
 *
 * This file is part of NFD (Named Data Networking Forwarding Daemon).
 * See AUTHORS.md for complete list of NFD authors and contributors.
 *
 * NFD is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * NFD is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * NFD, e.g., in COPYING.md file.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "android-logger-streambuf.hpp"

#include <android/log.h>

namespace nfd_android {

AndroidLoggerStreambuf::AndroidLoggerStreambuf()
{
  this->setp(buffer, buffer + MAX_BUF_SIZE - 1);
}

int
AndroidLoggerStreambuf::overflow(int c)
{
  if (c == traits_type::eof()) {
    *this->pptr() = traits_type::to_char_type(c);
    this->sbumpc();
  }
  return this->sync()? traits_type::eof(): traits_type::not_eof(c);
}

int
AndroidLoggerStreambuf::sync()
{
  int rc = 0;
  if (this->pbase() != this->pptr()) {
    __android_log_print(ANDROID_LOG_INFO, "Native", "%s",
                        std::string(this->pbase(), this->pptr() - this->pbase()).c_str());
    rc = 0;
    this->setp(buffer, buffer + MAX_BUF_SIZE - 1);
  }
  return rc;
}

} // namespace nfd_android
