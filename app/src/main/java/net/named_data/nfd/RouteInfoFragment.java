package net.named_data.nfd;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.RibEntry;
import com.intel.jndn.management.types.Route;

import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;
import net.named_data.nfd.utils.G;
import net.named_data.nfd.utils.Nfdc;

import java.util.HashMap;
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
    m_faceListAsyncTask = new FaceListAsyncTask();
    m_faceListAsyncTask.execute();
  }

  @Override
  public void onPause()
  {
    super.onPause();
    if (m_faceListAsyncTask != null) {
      m_faceListAsyncTask.cancel(false);
      m_faceListAsyncTask = null;
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
      return position;
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
        faceInfo += " (" + status.getUri() + ")";
      }

      holder.m_title.setText(faceInfo);
      holder.m_value.setText("origin: " + String.valueOf(r.getOrigin()) + " " +
                               "cost: " + String.valueOf(r.getCost()) + " " +
                               (r.getFlags().getChildInherit() ? "ChildInherit " : "") +
                               (r.getFlags().getCapture() ? "Capture " : "")
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
    }

    @Override
    protected Pair<List<FaceStatus>, Exception>
    doInBackground(Void... params) {
      Exception returnException = null;
      Nfdc nfdc = new Nfdc();
      List<FaceStatus> faceStatusList = null;
      try {
        faceStatusList = nfdc.faceList();
      } catch (Exception e) {
        returnException = e;
      }
      nfdc.shutdown();
      return new Pair<>(faceStatusList, returnException);
    }

    @Override
    protected void
    onCancelled() {
      // Nothing to do here; No change in UI.
    }

    @Override
    protected void
    onPostExecute(Pair<List<FaceStatus>, Exception> result) {
      if (result.second != null) {
        Toast.makeText(getActivity(),
                       "Error communicating with NFD (" + result.second.getMessage() + ")",
                       Toast.LENGTH_LONG).show();
      }

      updateFaceList(result.first);
    }
  }


  /////////////////////////////////////////////////////////////////////////////

  /** Bundle argument key for face information byte array */
  private static final String ROUTE_INFORMATION = "net.named_data.nfd.route_information";

  private RibEntry m_ribEntry;

  /** Callback handler of the hosting activity */
  private FaceListFragment.Callbacks m_callbacks;

  /** Reference to the most recent AsyncTask that was created for listing faces */
  private FaceListAsyncTask m_faceListAsyncTask;

  private RouteFaceListAdapter m_routeFaceListAdapter;
}
