/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2015-2017 Regents of the University of California
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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.RibEntry;

import net.named_data.nfd.utils.G;
import net.named_data.nfd.wifidirect.utils.NDNController;

import java.util.ArrayList;

/**
 * Main activity that is loaded for the NFD app.
 */
public class MainActivity extends AppCompatActivity
    implements DrawerFragment.DrawerCallbacks,
               LogcatFragment.Callbacks,
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
      ArrayList<DrawerFragment.DrawerItem> items = new ArrayList<DrawerFragment.DrawerItem>();

      items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_general, 0,
                                              DRAWER_ITEM_GENERAL));
      items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_faces, 0,
                                              DRAWER_ITEM_FACES));
      items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_routes, 0,
                                              DRAWER_ITEM_ROUTES));
      items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_ping, 0,
                                              DRAWER_ITEM_PING));
      //    items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_strategies, 0,
      //                                            DRAWER_ITEM_STRATEGIES));
      items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_logcat, 0,
                                              DRAWER_ITEM_LOGCAT));
      items.add(new DrawerFragment.DrawerItem(R.string.drawer_item_wifidirect, 0, DRAWER_ITEM_WIFIDIRECT));

      // TODO here we are preloading the NDNController singleton to avoid UI slowdown
      // it is due to building a test keychain: See NDNController.getInstance()
      NDNController.getInstance();

      m_drawerFragment = DrawerFragment.newInstance(items);

      fragmentManager
        .beginTransaction()
        .replace(R.id.navigation_drawer, m_drawerFragment, DrawerFragment.class.toString())
        .commit();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    G.Log("onCreateOptionsMenu" + String.valueOf(m_drawerFragment.shouldHideOptionsMenu()));
    if (!m_drawerFragment.shouldHideOptionsMenu()) {
      updateActionBar();
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

  /**
   * Convenience method that updates and display the current title in the Action Bar
   */
  @SuppressWarnings("deprecation")
  private void updateActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    if (m_actionBarTitleId != -1) {
      actionBar.setTitle(m_actionBarTitleId);
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
  onDrawerItemSelected(int itemCode, int itemNameId) {

    String fragmentTag = "net.named-data.nfd.content-" + String.valueOf(itemCode);
    FragmentManager fragmentManager = getSupportFragmentManager();

    // Create fragment according to user's selection
    Fragment fragment = fragmentManager.findFragmentByTag(fragmentTag);
    if (fragment == null) {
      switch (itemCode) {
        case DRAWER_ITEM_GENERAL:
          fragment = MainFragment.newInstance();
          break;
        case DRAWER_ITEM_FACES:
          fragment = FaceListFragment.newInstance();
          break;
        case DRAWER_ITEM_ROUTES:
          fragment = RouteListFragment.newInstance();
          break;
        case DRAWER_ITEM_PING:
          fragment = PingClientFragment.newInstance();
          break;
        // TODO: Placeholders; Fill these in when their fragments have been created
        //    case DRAWER_ITEM_STRATEGIES:
        //      break;
        case DRAWER_ITEM_LOGCAT:
          fragment = LogcatFragment.newInstance();
          break;
        case DRAWER_ITEM_WIFIDIRECT:
          fragment = WiFiDirectFragment.newInstance();
          break;
        default:
          // Invalid; Nothing else needs to be done
          return;
      }
    }

    // Update ActionBar title
    m_actionBarTitleId = itemNameId;

    fragmentManager.beginTransaction()
      .replace(R.id.main_fragment_container, fragment, fragmentTag)
      .commit();
  }

  @Override
  public void onDisplayLogcatSettings() {
    replaceContentFragmentWithBackstack(LogcatSettingsFragment.newInstance());
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

  /** Title that is to be displayed in the ActionBar */
  private int m_actionBarTitleId = -1;

  /** Item code for drawer items: For use in onDrawerItemSelected() callback */
  public static final int DRAWER_ITEM_GENERAL = 1;
  public static final int DRAWER_ITEM_FACES = 2;
  public static final int DRAWER_ITEM_ROUTES = 3;
  public static final int DRAWER_ITEM_PING = 4;
  //public static final int DRAWER_ITEM_STRATEGIES = 4;
  public static final int DRAWER_ITEM_LOGCAT = 5;
  public static final int DRAWER_ITEM_WIFIDIRECT = 6;
}
