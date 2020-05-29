/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/*
 * Copyright (c) 2015-2020 Regents of the University of California
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.RibEntry;

import net.named_data.jndn.encoding.Tlv0_3WireFormat;
import net.named_data.jndn.encoding.WireFormat;
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
    WireFormat.setDefaultWireFormat(Tlv0_3WireFormat.get());

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

    int drawItem = getIntent().getIntExtra(INTENT_KEY_FRAGMENT_TAG, R.id.nav_general);
    if (drawItem == R.id.nav_general) {
      onDrawerItemSelected(R.id.nav_general,
          getApplicationContext().getResources().getString(R.string.drawer_item_general));
    }
  }

  @FunctionalInterface
  public interface FragmentCreator {
    Fragment makeFragment();
  }

  private void
  showFragment(String fragmentTag, boolean addBackStack, FragmentCreator fragmentCreator) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    Fragment fragment = fragmentManager.findFragmentByTag(fragmentTag);

    if (fragment == null) {
      fragment = fragmentCreator.makeFragment();
      if (fragment == null) {
        G.Log("Requested to show " + fragmentTag + ", but fragment cannot be instantiated");
        return;
      }

      FragmentTransaction transaction = fragmentManager.beginTransaction();
      transaction.add(R.id.main_fragment_container, fragment, fragmentTag);
      if (addBackStack) {
        G.Log("Adding with backstack: " + lastFragmentTag);
        transaction.addToBackStack(lastFragmentTag);
      }
      transaction.commit();
    }
    else {
      FragmentTransaction transaction = fragmentManager.beginTransaction();
      transaction.show(fragment);
      if (addBackStack) {
        G.Log("Showing with backstack: " + lastFragmentTag);
        transaction.addToBackStack(lastFragmentTag);
      }
      transaction.commit();
    }

    if (lastFragment != null && lastFragment != fragment) {
      fragmentManager.beginTransaction()
        .hide(lastFragment)
        .commit();
    }

    lastFragment = fragment;
    lastFragmentTag = fragmentTag;
  }

  @Override
  public void onBackPressed()
  {
    FragmentManager fragmentManager = getSupportFragmentManager();

    if (fragmentManager.getBackStackEntryCount() > 0) {
      String name = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName();
      Fragment fragment = fragmentManager.findFragmentByTag(name);

      G.Log("Returning to " + name + " , " + fragment);
      if (fragment != null) {
        fragmentManager.beginTransaction()
          .show(fragment)
          .commit();

        lastFragment = fragment;
        lastFragmentTag = name;
      }
    }
    super.onBackPressed();
  }

  //////////////////////////////////////////////////////////////////////////////

  @Override
  public void
  onDrawerItemSelected(int itemCode, CharSequence itemTitle) {
    G.Log("onDrawerItemSelected: " + itemTitle);
    String fragmentTag = "net.named-data.nfd.content-" + String.valueOf(itemCode);

    showFragment(fragmentTag, false, () -> {
      switch (itemCode) {
        case R.id.nav_general:
          return MainFragment.newInstance();
        case R.id.nav_faces:
          return FaceListFragment.newInstance();
        case R.id.nav_routes:
          return RouteListFragment.newInstance();
        case R.id.nav_ping:
          return PingClientFragment.newInstance();
        //    case DRAWER_ITEM_STRATEGIES:
        //      return TODO: Placeholders; Fill these in when their fragments have been created
        case R.id.nav_wifidirect:
          return WiFiDirectFragment.newInstance();
        default:
          // Invalid; Nothing else needs to be done
          return null;
      }
    });

    ActionBar actionBar = getSupportActionBar();
    actionBar.setTitle(itemTitle);
  }

  @Override
  public void onFaceItemSelected(FaceStatus faceStatus) {
    showFragment(FaceStatusFragment.class.toString(), true,
                 () -> FaceStatusFragment.newInstance(faceStatus));
  }

  @Override
  public void onRouteItemSelected(RibEntry ribEntry) {
    showFragment(RouteInfoFragment.class.toString(), true,
                 () -> RouteInfoFragment.newInstance(ribEntry));
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Reference to drawer fragment */
  private DrawerFragment m_drawerFragment;

  /** Record the last fragment */
  private Fragment lastFragment = null;

  private String lastFragmentTag = "";

  /** Indent key for jump to a fragment */
  public static final String INTENT_KEY_FRAGMENT_TAG = "fragmentTag";
}
