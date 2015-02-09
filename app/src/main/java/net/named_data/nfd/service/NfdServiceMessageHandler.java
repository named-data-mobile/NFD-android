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

package net.named_data.nfd.service;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;

/**
 * Message handler for the the NFD Service.
 */
class NfdServiceMessageHandler extends Handler {

  private NfdService mNfdService;

  NfdServiceMessageHandler(NfdService nfdService) {
    mNfdService = nfdService;
  }

  @Override
  public void handleMessage(Message message) {
    switch (message.what) {
      case NfdServiceMessageConstants.MESSAGE_START_NFD_SERVICE:
        break;
      default:
        super.handleMessage(message);
        break;
    }
  }
}
