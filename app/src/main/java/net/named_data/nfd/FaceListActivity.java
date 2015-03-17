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
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.intel.jndn.management.types.FacePersistency;
import com.intel.jndn.management.types.FaceScope;
import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.LinkType;

import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;
import net.named_data.nfd.utils.NfdcAsyncTask;
import net.named_data.nfd.utils.Nfdc;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.w3c.dom.Text;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class FaceListActivity extends Activity {
  @Override
  public void
  onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    FragmentManager fm = getFragmentManager();
    FragmentTransaction ft = fm.beginTransaction();
    if (fm.findFragmentById(android.R.id.content) == null) {
      ft.add(android.R.id.content, m_faceListFragment);
    } else {
      ft.replace(android.R.id.content, m_faceListFragment);
    }
    ft.commit();
  }

  public static class FaceListFragment extends ListFragment
    implements AdapterView.OnItemLongClickListener,
               AdapterView.OnItemClickListener {
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
      super.onActivityCreated(savedInstanceState);

      setListAdapter(new FaceListAdapter(getActivity()));
      getListView().setLongClickable(true);

      getListView().setOnItemLongClickListener(this);
      getListView().setOnItemClickListener(this);

    }

    @Override
    public boolean
    onItemLongClick(final AdapterView<?> parent, View view, final int position, long id)
    {
      final int faceId = (int)id;
      if (faceId < 256) {
        return false;
      }

      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
      alertDialogBuilder
        .setMessage("Delete face " + String.valueOf(faceId) + "?")
        .setCancelable(false)
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id)
          {
            new NfdcAsyncTask(getActivity(),
                              new NfdcAsyncTask.Task() {
                                public String
                                runTask() throws Exception
                                {
                                  Nfdc nfdc = new Nfdc();
                                  nfdc.faceDestroy(faceId);
                                  nfdc.shutdown();

                                  getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run()
                                    {
                                      ((FaceListAdapter) parent.getAdapter()).updateFaceList();
                                    }
                                  });
                                  return null;
                                }
                              }).execute();
          }
        })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id)
          {
            dialog.cancel();
          }
        });

      AlertDialog alertDialog = alertDialogBuilder.create();
      alertDialog.show();

      return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
      FaceStatus s = (FaceStatus)getListAdapter().getItem(position);
      FaceStatusFragment fragment = new FaceStatusFragment();
      Bundle bundle = new Bundle();
      bundle.putByteArray("faceStatus", s.wireEncode().getImmutableArray());
      fragment.setArguments(bundle);

      getFragmentManager()
        .beginTransaction()
        .addToBackStack("FaceStatus")
        .replace(android.R.id.content, fragment)
        .commit();
    }

    private class FaceListAdapter extends BaseAdapter {
      public FaceListAdapter(Context context)
      {
        this.m_inflater = LayoutInflater.from(context);
        this.m_context = context;

        updateFaceList();
      }

      public void
      updateFaceList()
      {
        new NfdcAsyncTask(m_context,
                          new NfdcAsyncTask.Task() {
                            public String
                            runTask() throws Exception
                            {
                              synchronized (m_facesLock) {
                                Nfdc nfdc = new Nfdc();
                                m_faces = nfdc.faceList();
                                nfdc.shutdown();
                              }

                              getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run()
                                {
                                  notifyDataSetChanged();
                                }
                              });
                              return null;
                            }
                          }).execute();
      }

      @Override
      public int
      getCount()
      {
        synchronized (m_facesLock) {
          if (m_faces == null)
            return 0;
          else
            return m_faces.size();
        }
      }

      @Override
      public Object
      getItem(int position)
      {
        synchronized (m_facesLock) {
          assert m_faces != null && position < m_faces.size();
          return m_faces.get(position);
        }
      }

      @Override
      public long
      getItemId(int position)
      {
        synchronized (m_facesLock) {
          assert m_faces != null && position < m_faces.size();
          return m_faces.get(position).getFaceId();
        }
      }

      @Override
      public View
      getView(int position, View convertView, ViewGroup parent)
      {
        if (convertView == null) {
          convertView = this.m_inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
        }

        FaceStatus s;
        synchronized(m_facesLock) {
          s = m_faces.get(position);
        }

        ((TextView)convertView.findViewById(android.R.id.text2)).setText(String.valueOf(s.getFaceId()));
        ((TextView)convertView.findViewById(android.R.id.text1)).setText(s.getUri());

        return convertView;
      }

      /////////////////////////////////////////////////////////////////////////
      private LayoutInflater m_inflater;
      private Context m_context;
      private List<FaceStatus> m_faces;
      private final Object m_facesLock = new Object();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  public static class FaceStatusFragment extends ListFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
      super.onActivityCreated(savedInstanceState);

      FaceStatus s = null;
      try {
        s = new FaceStatus();
        s.wireDecode(new Blob(getArguments().getByteArray("faceStatus")).buf());
      }
      catch (EncodingException e) {
        assert false;
      }

      m_faceStatus.add(new Item("Face ID",          String.valueOf(s.getFaceId())));
      m_faceStatus.add(new Item("Local FaceUri",    s.getLocalUri()));
      m_faceStatus.add(new Item("Remote FaceUri",   s.getUri()));
      m_faceStatus.add(new Item("Expires in",       s.getExpirationPeriod() < 0 ?
        "never" :
        PeriodFormat.getDefault().print(new Period(s.getExpirationPeriod()))));
      m_faceStatus.add(new Item("Face scope",       getScope(s.getFaceScope())));
      m_faceStatus.add(new Item("Face persistency", getPersistency(s.getFacePersistency())));
      m_faceStatus.add(new Item("Link type",        getLinkType(s.getLinkType())));
      m_faceStatus.add(new Item("In interests",     String.valueOf(s.getInInterests())));
      m_faceStatus.add(new Item("In data",          String.valueOf(s.getInDatas())));
      m_faceStatus.add(new Item("Out interests",    String.valueOf(s.getOutInterests())));
      m_faceStatus.add(new Item("Out data",         String.valueOf(s.getOutDatas())));
      m_faceStatus.add(new Item("In bytes",         String.valueOf(s.getInBytes())));
      m_faceStatus.add(new Item("Out bytes",        String.valueOf(s.getOutBytes())));

      setListAdapter(new FaceStatusAdapter(getActivity()));
    }

    private class FaceStatusAdapter extends BaseAdapter {
      public FaceStatusAdapter(Context context)
      {
        this.m_inflater = LayoutInflater.from(context);
      }

      @Override
      public int getCount()
      {
        return m_faceStatus.size();
      }

      @Override
      public Object getItem(int position)
      {
        return m_faceStatus.get(position);
      }

      @Override
      public long getItemId(int position)
      {
        return position;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent)
      {
        if (convertView == null) {
          convertView = m_inflater.inflate(R.layout.face_status_item, parent, false);
        }

        Item i = (Item)getItem(position);
        ((TextView)convertView.findViewById(R.id.title)).setText(i.getTitle());
        ((TextView)convertView.findViewById(R.id.value)).setText(i.getValue());

        return convertView;
      }

      /////////////////////////////////////////////////////////////////////////
      private LayoutInflater m_inflater;
    }

    private static class Item {
      public Item(String title, String value)
      {
        m_title = title;
        m_value = value;
      }

      public String getValue()
      {
        return m_value;
      }

      public void setValue(String value)
      {
        m_value = value;
      }

      public String getTitle()
      {
        return m_title;
      }

      public void setTitle(String title)
      {
        m_title = title;
      }

      /////////////////////////////////////////////////////////////////////////
      private String m_title;
      private String m_value;
    }
    private List<Item> m_faceStatus = new LinkedList<Item>();
  }

  /////////////////////////////////////////////////////////////////////////////

  private static String
  getScope(FaceScope scope)
  {
    assert scope.getNumericValue() < s_scopes.length;
    return s_scopes[scope.getNumericValue()];
  }

  private static String
  getPersistency(FacePersistency persistency)
  {
    assert persistency.getNumericValue() < s_persistencies.length;
    return s_persistencies[persistency.getNumericValue()];
  }

  private static String
  getLinkType(LinkType linkType)
  {
    assert linkType.getNumericValue() < s_linkTypes.length;
    return s_linkTypes[linkType.getNumericValue()];
  }

  private static final String[] s_scopes = {"Local", "Non-local"};
  private static final String[] s_persistencies = {"Persistent", "On-demand", "Permanent"};
  private static final String[] s_linkTypes = {"Point-to-point", "Multi-access"};

  private FaceListFragment m_faceListFragment = new FaceListFragment();
}
