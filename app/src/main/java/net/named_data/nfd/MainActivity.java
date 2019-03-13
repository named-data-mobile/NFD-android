/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/*
 * Copyright (c) 2015-2019 Regents of the University of California
 * <p/>
 * This file is part of NFD (Named Data Networking Forwarding Daemon) Android.
 * See AUTHORS.md for complete list of NFD Android authors and contributors.
 * <p/>
 * NFD Android is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * <p/>
 * NFD Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * NFD Android, e.g., in COPYING.md file.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.named_data.nfd;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.RibEntry;

import net.named_data.nfd.utils.G;
import net.named_data.nfd.wifidirect.utils.NDNController;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * Main activity that is loaded for the NFD app.
 */
public class MainActivity extends AppCompatActivity
    implements DrawerFragment.DrawerCallbacks,
               FaceListFragment.Callbacks,
               RouteListFragment.Callbacks
{

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    FragmentManager fragmentManager = getSupportFragmentManager();

    if (savedInstanceState != null) {
      m_drawerFragment = (DrawerFragment)fragmentManager.findFragmentByTag(DrawerFragment.class.toString());
    }

    if (m_drawerFragment == null) {
      // TODO here we are preloading the NDNController singleton to avoid UI slowdown
      // it is due to building a test keychain: See NDNController.getInstance()
      NDNController.getInstance();

      m_drawerFragment = DrawerFragment.newInstance();

      fragmentManager
        .beginTransaction()
        .replace(R.id.navigation_drawer, m_drawerFragment, DrawerFragment.class.toString())
        .commit();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (!m_drawerFragment.shouldHideOptionsMenu()) {
      return super.onCreateOptionsMenu(menu);
    }
    else
      return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return super.onOptionsItemSelected(item);
  }

  //////////////////////////////////////////////////////////////////////////////

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    FragmentManager fragmentManager = getSupportFragmentManager();

    int drawItem = getIntent().getIntExtra(INTENT_KEY_FRAGMENT_TAG, R.id.nav_general);
    if (drawItem == R.id.nav_general) {
      MainFragment mainFragment =  MainFragment.newInstance();
      fragmentManager
          .beginTransaction()
          .replace(R.id.main_fragment_container, mainFragment)
          .commit();
    }
  }

  /**
   * Convenience method that replaces the main fragment container with the
   * new fragment and adding the current transaction to the backstack.
   *
   * @param fragment Fragment to be displayed in the main fragment container.
   */
  private void replaceContentFragmentWithBackstack(Fragment fragment) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.beginTransaction()
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .replace(R.id.main_fragment_container, fragment)
        .addToBackStack(null)
        .commit();
  }

  //////////////////////////////////////////////////////////////////////////////

  @Override
  public void
  onDrawerItemSelected(int itemCode, CharSequence itemTitle) {
    G.Log("onDrawerItemSelected: " + itemTitle);
    String fragmentTag = "net.named-data.nfd.content-" + String.valueOf(itemCode);
    FragmentManager fragmentManager = getSupportFragmentManager();

    // Create fragment according to user's selection
    Fragment fragment = fragmentManager.findFragmentByTag(fragmentTag);
    if (fragment == null) {
      switch (itemCode) {
        case R.id.nav_general:
          fragment = MainFragment.newInstance();
          break;
        case R.id.nav_faces:
          fragment = FaceListFragment.newInstance();
          break;
        case R.id.nav_routes:
          fragment = RouteListFragment.newInstance();
          break;
        case R.id.nav_ping:
          fragment = PingClientFragment.newInstance();
          break;
        // TODO: Placeholders; Fill these in when their fragments have been created
        //    case DRAWER_ITEM_STRATEGIES:
        //      break;
        case R.id.nav_wifidirect:
          fragment = WiFiDirectFragment.newInstance();
          break;
        default:
          // Invalid; Nothing else needs to be done
          return;
      }
    }

    ActionBar actionBar = getSupportActionBar();
    actionBar.setTitle(itemTitle);

    fragmentManager.beginTransaction()
      .replace(R.id.main_fragment_container, fragment, fragmentTag)
      .commit();
  }

  @Override
  public void onFaceItemSelected(FaceStatus faceStatus) {
    replaceContentFragmentWithBackstack(FaceStatusFragment.newInstance(faceStatus));
  }

  @Override
  public void onRouteItemSelected(RibEntry ribEntry)
  {
    replaceContentFragmentWithBackstack(RouteInfoFragment.newInstance(ribEntry));
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Reference to drawer fragment */
  private DrawerFragment m_drawerFragment;

  /** Indent key for jump to a fragment */
  public static final String INTENT_KEY_FRAGMENT_TAG = "fragmentTag";
}
