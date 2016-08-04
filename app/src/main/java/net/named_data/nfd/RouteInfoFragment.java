package net.named_data.nfd;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.intel.jndn.management.enums.RouteFlags;
import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.RibEntry;
import com.intel.jndn.management.types.Route;

import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn_xx.util.FaceUri;
import net.named_data.nfd.utils.G;
import net.named_data.nfd.utils.NfdcHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class RouteInfoFragment extends ListFragment {

  public static RouteInfoFragment
  newInstance(RibEntry ribEntry) {
    Bundle args = new Bundle();
    args.putByteArray(ROUTE_INFORMATION, ribEntry.wireEncode().getImmutableArray());

    RouteInfoFragment fragment = new RouteInfoFragment();
    fragment.setArguments(args);

    return fragment;
  }

  @Override
  public void onAttach(Activity activity)
  {
    super.onAttach(activity);
    try {
      m_callbacks = (FaceListFragment.Callbacks)activity;
    } catch (Exception e) {
      G.Log("Hosting activity must implement FaceListFragment.Callbacks: " + e);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    m_ribEntry = new RibEntry();
    try {
      m_ribEntry.wireDecode(new Blob(getArguments().getByteArray(ROUTE_INFORMATION)).buf());
    }
    catch (EncodingException e) {
      G.Log("ROUTE_INFORMATION: EncodingException: " + e);
    }
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);
    View v = getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_route_detail_list_header, null);
    getListView().addHeaderView(v, null, false);
    getListView().setDivider(getResources().getDrawable(R.drawable.list_item_divider));

    TextView prefix = (TextView)v.findViewById(R.id.route_detail_prefix);
    prefix.setText(m_ribEntry.getName().toUri());

    // Get progress bar spinner view
    m_reloadingListProgressBar = (ProgressBar)v.findViewById(R.id.route_detail_list_reloading_list_progress_bar);

    ListView listView = getListView();
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
      @Override
      public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater menuInflater = mode.getMenuInflater();
        menuInflater.inflate(R.menu.menu_face_list_multiple_modal_menu, menu);
        return true;
      }

      @Override
      public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
      }

      @Override
      public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
          case R.id.menu_item_delete_face_item:
            G.Log("Requesting to delete " + String.valueOf(m_routeFacesToDelete));
            removeRouteFace(m_ribEntry.getName(), m_routeFacesToDelete);
            m_routeFacesToDelete = new HashSet<>();
            mode.finish();
            return true;
          default:
            return false;
        }
      }

      @Override
      public void onDestroyActionMode(ActionMode mode) {

      }

      @Override
      public void
      onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        if (checked && id < 256) {
          getListView().setItemChecked(position, false);
          return;
        }
        if (checked)
          m_routeFacesToDelete.add((int)id);
        else
          m_routeFacesToDelete.remove((int)id);
      }

      private HashSet<Integer> m_routeFacesToDelete = new HashSet<>();

    });
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);
    if (m_routeFaceListAdapter == null) {
      m_routeFaceListAdapter = new RouteFaceListAdapter(getActivity(), m_ribEntry);
    }
    // setListAdapter must be called after addHeaderView.  Otherwise, there is an exception on some platforms.
    // http://stackoverflow.com/a/8141537/2150331
    setListAdapter(m_routeFaceListAdapter);
  }

  @Override
  public void onDestroyView()
  {
    super.onDestroyView();
    setListAdapter(null);
  }

  @Override
  public void onResume()
  {
    super.onResume();
    startRouteFaceListRetrievalTask();
  }

  @Override
  public void onPause()
  {
    super.onPause();
    stopRouteFaceListRetrievalTask();

    if (m_routeFaceRemoveAsyncTask != null) {
      m_routeFaceRemoveAsyncTask.cancel(false);
      m_routeFaceRemoveAsyncTask = null;
    }
  }

  @Override
  public void
  onListItemClick(ListView l, View v, int position, long id)
  {
    if (m_callbacks != null) {
      RouteFaceListAdapter ra = (RouteFaceListAdapter)((HeaderViewListAdapter)l.getAdapter()).getWrappedAdapter();
      if (ra.m_faces == null)
        return;

      Route route = (Route)l.getAdapter().getItem(position);
      FaceStatus faceStatus = ra.m_faces.get(route.getFaceId());
      m_callbacks.onFaceItemSelected(faceStatus);
    }
  }

  /////////////////////////////////////////////////////////////////////////////

  private void removeRouteFace(Name prefix, HashSet<Integer> faceIds)
  {
    m_routeFaceRemoveAsyncTask = new RouteFaceRemoveAsyncTask(prefix, faceIds);
    m_routeFaceRemoveAsyncTask.execute();
  }

  private void retrieveRouteFaceList() {
    // Stop if running; before starting the new Task
    if (m_routeListAsyncTask != null) {
      m_routeListAsyncTask.cancel(false);
      m_routeListAsyncTask = null;
    }
    m_routeListAsyncTask = new RouteListAsyncTask();
    m_routeListAsyncTask.execute();

    stopRouteFaceListRetrievalTask();
    startRouteFaceListRetrievalTask();
  }

  /**
   * Create a new AsyncTask for face list information retrieval.
   */
  private void startRouteFaceListRetrievalTask() {
    m_faceListAsyncTask = new FaceListAsyncTask();
    m_faceListAsyncTask.execute();
  }

  /**
   * Stops a previously started face retrieval AsyncTask.
   */
  private void stopRouteFaceListRetrievalTask() {
    if (m_faceListAsyncTask != null) {
      m_faceListAsyncTask.cancel(false);
      m_faceListAsyncTask = null;
    }
  }

  /**
   * Updates the underlying adapter with the given list of FaceStatus.
   *
   * Note: This method should only be called from the UI thread.
   *
   * @param list Update ListView with the given List&lt;FaceStatus&gt;
   */
  private void updateFaceList(List<FaceStatus> list) {
    ((RouteFaceListAdapter)getListAdapter()).updateFaceList(list);
  }


  private static class RouteFaceListAdapter extends BaseAdapter {

    public RouteFaceListAdapter(Context context, RibEntry ribEntry)
    {
      this.m_layoutInflater = LayoutInflater.from(context);
      this.m_ribEntry = ribEntry;
    }

    private void
    updateFaceList(List<FaceStatus> faces)
    {
      m_faces = new HashMap<>();
      for (FaceStatus faceStatus : faces) {
        m_faces.put(faceStatus.getFaceId(), faceStatus);
      }
      notifyDataSetChanged();
    }

    @Override
    public int
    getCount()
    {
      return m_ribEntry.getRoutes().size();
    }

    @Override
    public Route
    getItem(int position)
    {
      return m_ribEntry.getRoutes().get(position);
    }

    @Override
    public long
    getItemId(int position)
    {
      return m_ribEntry.getRoutes().get(position).getFaceId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ListItemHolder holder;
      if (convertView == null) {
        holder = new ListItemHolder();

        convertView = m_layoutInflater.inflate(R.layout.list_item_route_info_item, null);
        convertView.setTag(holder);

        holder.m_title = (TextView)convertView.findViewById(R.id.list_item_route_info_title);
        holder.m_value = (TextView)convertView.findViewById(R.id.list_item_route_info_value);
      } else {
        holder = (ListItemHolder)convertView.getTag();
      }

      Route r = getItem(position);
      String faceInfo = String.valueOf(r.getFaceId());

      if (m_faces != null) {
        FaceStatus status = m_faces.get(r.getFaceId());
        faceInfo += " (" + status.getRemoteUri() + ")";
      }

      holder.m_title.setText(faceInfo);
      holder.m_value.setText("origin: " + String.valueOf(r.getOrigin()) + " " +
                               "cost: " + String.valueOf(r.getCost()) + " " +
                               ((r.getFlags() & RouteFlags.CHILD_INHERIT.toInteger()) > 0 ? "ChildInherit " : "") +
                               ((r.getFlags() & RouteFlags.CAPTURE.toInteger()) > 0 ? "Capture " : "")
                             //            +
                             //          (r.getExpirationPeriod() > 0 ? "Expires in " + PeriodFormat.getDefault().print(new Period((int)(1000*r.getExpirationPeriod()))) : "")
      );

      return convertView;
    }

    private static class ListItemHolder {
      private TextView m_title;
      private TextView m_value;
    }

    /////////////////////////////////////////////////////////////////////////
    private LayoutInflater m_layoutInflater;
    private Map<Integer, FaceStatus> m_faces;
    private RibEntry m_ribEntry;
  }

  /**
   * AsyncTask that gets the list of faces from the running NFD.
   */
  private class FaceListAsyncTask extends AsyncTask<Void, Void, Pair<List<FaceStatus>, Exception>> {
    @Override
    protected void
    onPreExecute() {
      // Display progress bar
      m_reloadingListProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected Pair<List<FaceStatus>, Exception>
    doInBackground(Void... params) {
      Exception returnException = null;
      NfdcHelper nfdcHelper = new NfdcHelper();
      List<FaceStatus> faceStatusList = null;
      try {
        faceStatusList = nfdcHelper.faceList();
      } catch (Exception e) {
        returnException = e;
      }
      nfdcHelper.shutdown();
      return new Pair<>(faceStatusList, returnException);
    }

    @Override
    protected void
    onCancelled() {
      // Remove progress bar
      m_reloadingListProgressBar.setVisibility(View.GONE);
    }

    @Override
    protected void
    onPostExecute(Pair<List<FaceStatus>, Exception> result) {
      // Remove progress bar
      m_reloadingListProgressBar.setVisibility(View.GONE);

      if (result.second != null) {
        Toast.makeText(getActivity(),
                       "Error communicating with NFD (" + result.second.getMessage() + ")",
                       Toast.LENGTH_LONG).show();
        return;
      }

      updateFaceList(result.first);
    }
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
      NfdcHelper nfdcHelper = new NfdcHelper();
      Exception returnException = null;
      List<RibEntry> routes = null;
      try {
        routes = nfdcHelper.ribList();
      }
      catch (Exception e) {
        returnException = e;
      }
      nfdcHelper.shutdown();
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
        return;
      }

      updateRoute(result.first);
    }
  }

  private void updateRoute(List<RibEntry> ribList) {
    for (RibEntry rib : ribList) {
      if ((rib.getName().toUri()).equals(m_ribEntry.getName().toUri())) {
        m_ribEntry = rib;
        m_routeFaceListAdapter = new RouteFaceListAdapter(getActivity(), m_ribEntry);
        setListAdapter(m_routeFaceListAdapter);
        return;
      }
    }

    // After removal of the last next hops, jump back to the route list fragment
    Fragment fragment = RouteListFragment.newInstance();
    getActivity().getSupportFragmentManager()
              .beginTransaction()
              .replace(R.id.main_fragment_container, fragment)
              .commit();
  }

  /**
   * AsyncTask that removes next hops that are passed in as a list of NextHopInfo.
   */
  private class RouteFaceRemoveAsyncTask extends AsyncTask<Void, Void, String> {
    public
    RouteFaceRemoveAsyncTask(Name prefix, HashSet<Integer> routeFaceIds)
    {
      m_prefix = prefix;
      m_routeFaceList = routeFaceIds;
    }

    @Override
    protected String
    doInBackground(Void... params)
    {
      NfdcHelper nfdcHelper = new NfdcHelper();
      try {
        for (int routeFaceId : m_routeFaceList) {
          nfdcHelper.ribUnregisterPrefix(m_prefix, routeFaceId);
        }

        nfdcHelper.shutdown();
        return "OK";
      }
      catch (FaceUri.CanonizeError e) {
        return "Error Destroying dace (" + e.getMessage() + ")";
      }
      catch (FaceUri.Error e) {
        return "Error destroying face (" + e.getMessage() + ")";
      }
      catch (Exception e) {
        return "Error communicating with NFD (" + e.getMessage() + ")";
      }
      finally {
        nfdcHelper.shutdown();
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

      retrieveRouteFaceList();
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
    private HashSet<Integer> m_routeFaceList;
  }


  /////////////////////////////////////////////////////////////////////////////

  /** Bundle argument key for face information byte array */
  private static final String ROUTE_INFORMATION = "net.named_data.nfd.route_information";

  private RibEntry m_ribEntry;

  /** Progress bar spinner to display to user when destroying faces */
  private ProgressBar m_reloadingListProgressBar;

  /** Callback handler of the hosting activity */
  private FaceListFragment.Callbacks m_callbacks;

  /** Reference to the most recent AsyncTask that was created for removing a next hop for a route */
  private RouteFaceRemoveAsyncTask m_routeFaceRemoveAsyncTask;

  /** Reference to the most recent AsyncTask that was created for listing faces */
  private FaceListAsyncTask m_faceListAsyncTask;

  private RouteListAsyncTask m_routeListAsyncTask;

  private RouteFaceListAdapter m_routeFaceListAdapter;
}
