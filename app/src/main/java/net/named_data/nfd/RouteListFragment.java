/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
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

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.intel.jndn.management.types.RibEntry;
import com.intel.jndn.management.types.Route;

import net.named_data.jndn.Name;
import net.named_data.jndn_xx.util.FaceUri;
import net.named_data.nfd.utils.G;
import net.named_data.nfd.utils.Nfdc;

import java.util.ArrayList;
import java.util.List;

public class RouteListFragment extends ListFragment implements RouteCreateDialogFragment.OnRouteCreateRequested {

  public static RouteListFragment
  newInstance() {
    return new RouteListFragment();
  }

  public interface Callbacks {
    /**
     * This method is called when a route is selected and more
     * information about it should be presented to the user.
     *
     * @param ribEntry RibEntry instance with information about the selected route
     */
    public void onRouteItemSelected(RibEntry ribEntry);
  }

  @Override
  public void onAttach(Activity activity)
  {
    super.onAttach(activity);
    try {
      m_callbacks = (Callbacks)activity;
    } catch (Exception e) {
      G.Log("Hosting activity must implement this fragment's callbacks: " + e);
    }
  }

  @Override
  public void
  onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);
    View v = getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_route_list_list_header, null);
    getListView().addHeaderView(v, null, false);
    getListView().setDivider(getResources().getDrawable(R.drawable.list_item_divider));

    m_routeListInfoUnavailableView = v.findViewById(R.id.route_list_info_unavailable);

    // Get progress bar spinner view
    m_reloadingListProgressBar = (ProgressBar)v.findViewById(R.id.route_list_reloading_list_progress_bar);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);
    if (m_routeListAdapter == null) {
      m_routeListAdapter = new RouteListAdapter(getActivity());
    }
    // setListAdapter must be called after addHeaderView.  Otherwise, there is an exception on some platforms.
    // http://stackoverflow.com/a/8141537/2150331
    setListAdapter(m_routeListAdapter);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
  {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_route_list, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId()) {
      case R.id.route_list_refresh:
        retrieveRouteList();
        return true;
      case R.id.route_list_add:
        RouteCreateDialogFragment dialog = RouteCreateDialogFragment.newInstance();
        dialog.setTargetFragment(RouteListFragment.this, 0);
        dialog.show(getFragmentManager(), "RouteCreateFragment");
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void
  onResume() {
    super.onResume();
    startRouteListInfoRetrievalTask();
  }

  @Override
  public void
  onPause() {
    super.onPause();
    stopRouteListInfoRetrievalTask();

    if (m_routeCreateAsyncTask != null) {
      m_routeCreateAsyncTask.cancel(false);
      m_routeCreateAsyncTask = null;
    }
  }

  @Override
  public void onDestroyView()
  {
    super.onDestroyView();
    setListAdapter(null);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id)
  {
    if (m_callbacks != null) {
      RibEntry ribEntry = (RibEntry)l.getAdapter().getItem(position);
      m_callbacks.onRouteItemSelected(ribEntry);
    }
  }

  @Override
  public void
  createRoute(Name prefix, String faceUri)
  {
    m_routeCreateAsyncTask = new RouteCreateAsyncTask(prefix, faceUri);
    m_routeCreateAsyncTask.execute();
  }


  /////////////////////////////////////////////////////////////////////////

  /**
   * Updates the underlying adapter with the given list of RibEntry.
   *
   * Note: This method should only be called from the UI thread.
   *
   * @param list Update ListView with the given List&lt;RibEntry&gt;
   */
  private void updateRouteList(List<RibEntry> list) {
    if (list == null) {
      m_routeListInfoUnavailableView.setVisibility(View.VISIBLE);
      return;
    }

    ((RouteListAdapter)getListAdapter()).updateList(list);
  }

  /**
   * Convenience method that starts the AsyncTask that retrieves the
   * list of available routes.
   */
  private void retrieveRouteList() {
    // Update UI
    m_routeListInfoUnavailableView.setVisibility(View.GONE);

    // Stop if running; before starting the new Task
    stopRouteListInfoRetrievalTask();
    startRouteListInfoRetrievalTask();
  }

  /**
   * Create a new AsynTask for route list information retrieval.
   */
  private void startRouteListInfoRetrievalTask() {
    m_routeListAsyncTask = new RouteListAsyncTask();
    m_routeListAsyncTask.execute();
  }

  /**
   * Stops a previously started AsyncTask.
   */
  private void stopRouteListInfoRetrievalTask() {
    if (m_routeListAsyncTask != null) {
      m_routeListAsyncTask.cancel(false);
      m_routeListAsyncTask = null;
    }
  }

  /////////////////////////////////////////////////////////////////////////

  private static class RouteListAdapter extends BaseAdapter {

    public RouteListAdapter(Context context) {
      m_layoutInflater = LayoutInflater.from(context);
    }

    public void
    updateList(List<RibEntry> ribEntries) {
      m_ribEntries = ribEntries;
      notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
      return (m_ribEntries == null) ? 0 : m_ribEntries.size();
    }

    @Override
    public RibEntry
    getItem(int i)
    {
      assert m_ribEntries != null;
      return m_ribEntries.get(i);
    }

    @Override
    public long
    getItemId(int i)
    {
      return i;
    }

    @Override
    public View
    getView(int position, View convertView, ViewGroup parent) {
      RouteItemHolder holder;

      if (convertView == null) {
        holder = new RouteItemHolder();

        convertView = m_layoutInflater.inflate(R.layout.list_item_route_item, null);
        convertView.setTag(holder);

        holder.m_uri = (TextView) convertView.findViewById(R.id.list_item_route_uri);
        holder.m_faceList = (TextView) convertView.findViewById(R.id.list_item_face_list);
      } else {
        holder = (RouteItemHolder) convertView.getTag();
      }

      RibEntry entry = getItem(position);

      // Prefix
      holder.m_uri.setText(entry.getName().toUri());

      // List of faces
      List<String> faceList = new ArrayList<>();
      for (Route r : entry.getRoutes()) {
        faceList.add(String.valueOf(r.getFaceId()));
      }
      holder.m_faceList.setText(TextUtils.join(", ", faceList));

      return convertView;
    }

    private static class RouteItemHolder {
      private TextView m_uri;
      private TextView m_faceList;
    }

    private final LayoutInflater m_layoutInflater;
    private List<RibEntry> m_ribEntries;
  }

  private class RouteListAsyncTask extends AsyncTask<Void, Void, Pair<List<RibEntry>, Exception>> {
    @Override
    protected void
    onPreExecute() {
      // Display progress bar
      m_reloadingListProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected Pair<List<RibEntry>, Exception>
    doInBackground(Void... params) {
      Nfdc nfdc = new Nfdc();
      Exception returnException = null;
      List<RibEntry> routes = null;
      try {
        routes = nfdc.ribList();
      }
      catch (Exception e) {
        returnException = e;
      }
      nfdc.shutdown();
      return new Pair<>(routes, returnException);
    }

    @Override
    protected void onCancelled() {
      // Remove progress bar
      m_reloadingListProgressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onPostExecute(Pair<List<RibEntry>, Exception> result) {
      // Remove progress bar
      m_reloadingListProgressBar.setVisibility(View.GONE);

      if (result.second != null) {
        Toast.makeText(getActivity(),
                       "Error communicating with NFD (" + result.second.getMessage() + ")",
                       Toast.LENGTH_LONG).show();
      }

      updateRouteList(result.first);
    }
  }


  private class RouteCreateAsyncTask extends AsyncTask<Void, Void, String> {
    public
    RouteCreateAsyncTask(Name prefix, String faceUri)
    {
      m_prefix = prefix;
      m_faceUri = faceUri;
    }

    @Override
    protected String
    doInBackground(Void... params)
    {
      Nfdc nfdc = new Nfdc();
      try {
        int faceId = nfdc.faceCreate(m_faceUri);
        nfdc.ribRegisterPrefix(new Name(m_prefix), faceId, 10, true, false);
        nfdc.shutdown();
        return "OK";
      }
      catch (FaceUri.CanonizeError e) {
        return "Error creating face (" + e.getMessage() + ")";
      }
      catch (FaceUri.Error e) {
        return "Error creating face (" + e.getMessage() + ")";
      }
      catch (Exception e) {
        return "Error communicating with NFD (" + e.getMessage() + ")";
      }
      finally {
        nfdc.shutdown();
      }
    }

    @Override
    protected void
    onPreExecute()
    {
      // Display progress bar
      m_reloadingListProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void
    onPostExecute(String status)
    {
      // Display progress bar
      m_reloadingListProgressBar.setVisibility(View.VISIBLE);
      Toast.makeText(getActivity(), status, Toast.LENGTH_LONG).show();

      retrieveRouteList();
    }

    @Override
    protected void
    onCancelled()
    {
      // Remove progress bar
      m_reloadingListProgressBar.setVisibility(View.GONE);
    }

    ///////////////////////////////////////////////////////////////////////////

    private Name m_prefix;
    private String m_faceUri;
  }

  /////////////////////////////////////////////////////////////////////////////

  /** Callback handler of the hosting activity */
  private Callbacks m_callbacks;

  /** Reference to the most recent AsyncTask that was created for listing routes */
  private RouteListAsyncTask m_routeListAsyncTask;

  /** Reference to the view to be displayed when no information is available */
  private View m_routeListInfoUnavailableView;

  /** Progress bar spinner to display to user when destroying faces */
  private ProgressBar m_reloadingListProgressBar;

  /** Reference to the most recent AsyncTask that was created for creating a route */
  private RouteCreateAsyncTask m_routeCreateAsyncTask;

  private RouteListAdapter m_routeListAdapter;
}