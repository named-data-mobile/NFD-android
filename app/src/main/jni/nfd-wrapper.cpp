/* -*- Mode:C++; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2015 Regents of the University of California
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
#include "android-logger-streambuf.hpp"

extern "C" {
int
main(int argc, char** argv);
}

#include <iostream>

JNIEXPORT void JNICALL
Java_net_named_1data_nfd_wrappers_NfdWrapper_startNfd(JNIEnv *, jclass)
{

}

JNIEXPORT void JNICALL
Java_net_named_1data_nfd_wrappers_NfdWrapper_stopNfd(JNIEnv *, jclass)
{

}
