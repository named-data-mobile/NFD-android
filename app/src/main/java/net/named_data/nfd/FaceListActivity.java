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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.intel.jndn.management.types.FacePersistency;
import com.intel.jndn.management.types.FaceScope;
import com.intel.jndn.management.types.FaceStatus;

import net.named_data.nfd.utils.NfdcAsyncTask;
import net.named_data.nfd.utils.Nfdc;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import java.util.List;

public class FaceListActivity extends Activity
{
  @Override
  public void
  onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    FragmentManager fm = getFragmentManager();
    FragmentTransaction ft = fm.beginTransaction();
    if (fm.findFragmentById(android.R.id.content) == null) {
      ft.add(android.R.id.content, m_faceListFragment);
    }
    else {
      ft.replace(android.R.id.content, m_faceListFragment);
    }
    ft.commit();
  }

  public static class FaceListFragment extends ListFragment {
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
      super.onActivityCreated(savedInstanceState);

      setListAdapter(new FaceListAdapter(getActivity()));
      getListView().setLongClickable(true);

      getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
      {
        @Override
        public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id)
        {
          final int faceId = (int)id;

          AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
          alertDialogBuilder
            .setMessage("Delete face " + String.valueOf(faceId) + "?")
            .setCancelable(false)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog,int id) {
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
              public void onClick(DialogInterface dialog,int id) {
                dialog.cancel();
              }
            });

          AlertDialog alertDialog = alertDialogBuilder.create();
          alertDialog.show();

          return true;
        }
      });
    }

    private class FaceListAdapter extends BaseAdapter
    {
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
        FaceListItemViewHolder holder;
        if (convertView == null) {
          convertView = this.m_inflater.inflate(R.layout.face_list_item, parent, false);
          holder = new FaceListItemViewHolder(convertView);
          convertView.setTag(holder);
        } else {
          holder = (FaceListItemViewHolder) convertView.getTag();
        }

        FaceStatus s;
        synchronized (m_facesLock) {
          s = m_faces.get(position);
        }
        holder.localUri.setText(s.getLocalUri());
        if (!s.getLocalUri().equals(s.getUri())) {
          holder.remoteUri.setVisibility(View.VISIBLE);
          holder.remoteUri.setText(s.getUri());
        }
        else {
          holder.remoteUri.setVisibility(View.GONE);
        }
        holder.faceId.setText(String.valueOf(s.getFaceId()));
        holder.scope.setText(getScope(s.getFaceScope()));
        holder.persistency.setText(getPersistency(s.getFacePersistency()));
        if (s.getExpirationPeriod() > 0) {
          holder.expires.setVisibility(View.VISIBLE);
          holder.expires.setText("expires in " + PeriodFormat.getDefault().print(new Period(s.getExpirationPeriod())));
        }
        else {
          holder.expires.setVisibility(View.GONE);
        }

        return convertView;
      }

      /////////////////////////////////////////////////////////////////////////
      private LayoutInflater m_inflater;
      private Context m_context;
      private List<FaceStatus> m_faces;
      private final Object m_facesLock = new Object();

    }

    private static class FaceListItemViewHolder {
      FaceListItemViewHolder(View v)
      {
        localUri = (TextView)v.findViewById(R.id.localUri);
        remoteUri = (TextView)v.findViewById(R.id.remoteUri);
        faceId = (TextView)v.findViewById(R.id.faceId);
        scope = (TextView)v.findViewById(R.id.scope);
        persistency = (TextView)v.findViewById(R.id.persistency);
        expires = (TextView)v.findViewById(R.id.expires);
      }

      /////////////////////////////////////////////////////////////////////////
      public TextView localUri;
      public TextView remoteUri;
      public TextView faceId;
      public TextView scope;
      public TextView persistency;
      public TextView expires;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

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


  private static final String[] s_scopes = {"Local", "Non-local"};
  private static final String[] s_persistencies = {"Persistent", "On-demand", "Permanent"};

  private FaceListFragment m_faceListFragment = new FaceListFragment();
}
