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

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import net.named_data.nfd.utils.G;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.customview.widget.ViewDragHelper;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

/**
 * DrawerFragment that provides navigation for MainActivity.
 */
public class DrawerFragment extends Fragment {

  public static DrawerFragment
  newInstance() {
    return new DrawerFragment();
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      // Register callback
      m_callbacks = (DrawerCallbacks)activity;
    } catch (ClassCastException e) {
      throw new ClassCastException("Host activity must implement DrawerFragment.DrawerCallbacks.");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    m_callbacks = null;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      m_drawerSelectedPosition = savedInstanceState.getInt(DRAWER_SELECTED_POSITION_BUNDLE_KEY);
    }

    G.Log("DrawerFragment: onCreate");
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    // Fragment influences action bar
    setHasOptionsMenu(true);

    G.Log("DrawerFragment: onActivityCreated");

    // Initialize and set up the navigation drawer UI
    initializeDrawerFragment(getActivity().findViewById(R.id.navigation_drawer),
                             getActivity().findViewById(R.id.drawer_layout),
                             getActivity().findViewById(R.id.bottom_navigation_view));

    if (savedInstanceState == null) {
      // when restoring (e.g., after rotation), rely on system to restore previous state of
      // fragments
      MenuItem item = m_drawerFragmentViewContainer.getMenu().findItem(m_drawerSelectedPosition);
      if (item != null) {
        item.setChecked(true);
        m_callbacks.onDrawerItemSelected(item.getItemId(), item.getTitle());
      }
      else {
        G.Log("Logic problem: there should always be some menu item found");
      }

      item = m_bottomNav.getMenu().findItem(m_drawerSelectedPosition);
      if (item != null) {
        item.setChecked(true);
      }
      // if not found, nothing to worry about
    }
  }

  /**
   * Initialize drawer fragment after being attached to the host activity.
   *
   * @param drawerFragmentViewContainer View container that holds the navigation drawer
   * @param drawerLayout DrawerLayout of the drawer in the host Activity
   */
  private void initializeDrawerFragment(NavigationView drawerFragmentViewContainer,
                                        DrawerLayout drawerLayout,
                                        BottomNavigationView bottomNavigationView)
  {
    m_drawerFragmentViewContainer = drawerFragmentViewContainer;
    m_drawerLayout = drawerLayout;
    m_bottomNav = bottomNavigationView;

    m_drawerFragmentViewContainer.setNavigationItemSelectedListener(
      new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item)
        {
          item.setChecked(true);
          m_drawerLayout.closeDrawers();

          m_callbacks.onDrawerItemSelected(item.getItemId(), item.getTitle());
          m_drawerSelectedPosition = item.getItemId();

          MenuItem bottomItem = m_bottomNav.getMenu().findItem(item.getItemId());
          if (bottomItem != null) {
            bottomItem.setChecked(true);
          }
          else {
            MenuItem selectedBottomItem = m_bottomNav.getMenu().getItem(m_bottomNav.getSelectedItemId());
            if (selectedBottomItem != null) {
              selectedBottomItem.setChecked(false);
            }
          }

          return true;
        }
      }
    );

    m_bottomNav.setOnNavigationItemSelectedListener(
      new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item)
        {
          m_callbacks.onDrawerItemSelected(item.getItemId(), item.getTitle());
          m_drawerSelectedPosition = item.getItemId();

          m_drawerFragmentViewContainer.setCheckedItem(item.getItemId());

          return true;
        }
      }
    );

    // Setup drawer and action bar
    ActionBar actionBar = getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);

    m_drawerToggle = new ActionBarDrawerToggle(
        getActivity(),
        m_drawerLayout,
        R.string.accessibility_open_drawer,
        R.string.accessibility_close_drawer)
    {
      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);

        // Allow update calls to onCreateOptionsMenu() and
        // onPrepareOptionsMenu() to update Menu UI.
        m_shouldHideOptionsMenu = false;
        getActivity().invalidateOptionsMenu();
      }

      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);

        // Allow update calls to onCreateOptionsMenu() and
        // onPrepareOptionsMenu() to update Menu UI
        m_shouldHideOptionsMenu = true;
        getActivity().invalidateOptionsMenu();
      }

      @Override
      public void onDrawerStateChanged(int newState) {
        super.onDrawerStateChanged(newState);
        if (newState != ViewDragHelper.STATE_IDLE) {
          // opened/closed is handled by onDrawerOpened and onDrawerClosed callbacks
          m_shouldHideOptionsMenu = true;
        } else if (!isDrawerOpen()) {
          // This condition takes care of the case of displaying the option menu
          // items when the drawer is retracted prematurely.
          m_shouldHideOptionsMenu = false;
        }
      }
    };

    // Post to drawer's handler to update UI State
    m_drawerLayout.post(new Runnable() {
      @Override
      public void run() {
        m_drawerToggle.syncState();
      }
    });

    m_drawerLayout.addDrawerListener(m_drawerToggle);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(DRAWER_SELECTED_POSITION_BUNDLE_KEY, m_drawerSelectedPosition);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // Forward the new configuration the drawer toggle component.
    m_drawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    // Remove option menu items when drawer is sliding out
    if (m_shouldHideOptionsMenu) {
      for (int i = 0; i < menu.size(); i++) {
        menu.getItem(i).setVisible(false);
      }
    }

  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle drawer selection events
    if (m_drawerToggle.onOptionsItemSelected(item)) {
      return true;
    }

    // Handle other menu items
    switch (item.getItemId()) {
    // Handle activity menu item here (if any)
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  public boolean
  shouldHideOptionsMenu() {
    return m_shouldHideOptionsMenu;
  }

  /**
   * Safe convenience method to determine if drawer is open.
   *
   * @return True if drawer is present and in an open state; false otherwise
   */
  private boolean
  isDrawerOpen() {
    return m_drawerLayout != null && m_drawerLayout.isDrawerOpen(m_drawerFragmentViewContainer);
  }

  /**
   * Convenience method to get host activity's ActionBar. This makes for easy updates
   * in a single location when upgrading to use >= HONEYCOMB (API 11) ActionBar.
   *
   * @return Host activity's ActionBar.
   */
  private ActionBar getActionBar() {
    return ((AppCompatActivity)getActivity()).getSupportActionBar();
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Callback that host activity must implement */
  public interface DrawerCallbacks {
    /** Callback to host activity when a drawer item is selected */
    void onDrawerItemSelected(int itemCode, CharSequence itemTitle);
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Bundle key used to (re)store position of selected drawer item */
  private static final String DRAWER_SELECTED_POSITION_BUNDLE_KEY
      = "DRAWER_SELECTED_POSITION";

  /** Callback to parent activity */
  private DrawerCallbacks m_callbacks;

  /** DrawerToggle for interacting with drawer and action bar app icon */
  private ActionBarDrawerToggle m_drawerToggle;

  /** Reference to DrawerLayout fragment in host activity */
  private DrawerLayout m_drawerLayout;

  /** Drawer's fragment container in the host activity */
  private NavigationView m_drawerFragmentViewContainer;

  private BottomNavigationView m_bottomNav;

  /** Current position of the Drawer's selection */
  private int m_drawerSelectedPosition = R.id.nav_general;

  /** Flag that marks if drawer is sliding outwards and being displayed */
  private boolean m_shouldHideOptionsMenu = false;
}
