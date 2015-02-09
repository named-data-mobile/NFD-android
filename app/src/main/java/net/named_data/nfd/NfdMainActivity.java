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

package net.named_data.nfd;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import net.named_data.nfd.service.NfdService;
import net.named_data.nfd.wrappers.NfdWrapper;

public class NfdMainActivity extends ActionBarActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public void startNFD(View view) {
    G.Log("Starting NFD ...");

    Intent intent = new Intent(this, NfdService.class);
    startService(intent);
  }

  public void stopNFD(View view) {
    G.Log("Stopping NFD ...");

    Intent intent = new Intent(this, NfdService.class);
    stopService(intent);
  }

  public void startNFDexplicit(View view) {
    G.Log("Starting NFD explicitly ...");

    Intent intent = new Intent("net.named_data.nfd.NfdService");
    startService(intent);
  }

  public void startNfd(View view) {
    G.Log("Starting NFD through JNI ...");

    NfdWrapper.startNfd();
  }

  public void stopNfd(View view) {
    G.Log("Stopping NFD through JNI ...");

    NfdWrapper.stopNfd();
  }
}
