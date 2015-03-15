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
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.intel.jndn.management.types.RibEntry;
import com.intel.jndn.management.types.Route;

import net.named_data.nfd.utils.Nfdc;
import net.named_data.nfd.utils.NfdcAsyncTask;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class RouteListActivity extends Activity
{
  @Override
  public void
  onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    FragmentManager fm = getFragmentManager();
    FragmentTransaction ft = fm.beginTransaction();
    if (fm.findFragmentById(android.R.id.content) == null) {
      ft.add(android.R.id.content, m_routeListFragment);
    }
    else {
      ft.replace(android.R.id.content, m_routeListFragment);
    }
    ft.commit();
  }

  public static class RouteListFragment extends ListFragment {
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
      super.onActivityCreated(savedInstanceState);

      RouteListAdapter adapter = new RouteListAdapter(getActivity());
      setListAdapter(adapter);

      adapter.updateFaceList();
    }

    private class RouteListAdapter extends BaseAdapter
    {
      public RouteListAdapter(Context context)
      {
        this.m_inflater = LayoutInflater.from(context);
        this.m_context = context;
      }

      public void
      updateFaceList()
      {
        new NfdcAsyncTask(m_context,
                          new NfdcAsyncTask.Task() {
                            public String
                            runTask() throws Exception
                            {
                              synchronized (m_routesLock) {
                                Nfdc nfdc = new Nfdc();
                                m_routes = nfdc.ribList();
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
        synchronized (m_routesLock) {
          if (m_routes == null)
            return 0;
          else
            return m_routes.size();
        }
      }

      @Override
      public Object
      getItem(int position)
      {
        synchronized (m_routesLock) {
          assert m_routes != null && position < m_routes.size();
          return m_routes.get(position);
        }
      }

      @Override
      public long
      getItemId(int position)
      {
        return position;
      }

      @Override
      public View
      getView(int position, View convertView, ViewGroup parent)
      {
        RouteListItemViewHolder holder;
        if (convertView == null) {
          convertView = this.m_inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
          holder = new RouteListItemViewHolder(convertView);
          convertView.setTag(holder);
        } else {
          holder = (RouteListItemViewHolder)convertView.getTag();
        }

        RibEntry e;
        synchronized (m_routesLock) {
          e = m_routes.get(position);
        }
        holder.text1.setText(e.getName().toUri());

        List<String> faceList = new ArrayList<>();
        for (Route r : e.getRoutes()) {
          faceList.add(String.valueOf(r.getFaceId()));
        }
        holder.text2.setText(StringUtils.join(faceList, ", "));

        return convertView;
      }

      /////////////////////////////////////////////////////////////////////////
      private LayoutInflater m_inflater;
      private Context m_context;
      private List<RibEntry> m_routes;
      private final Object m_routesLock = new Object();

    }

    private static class RouteListItemViewHolder
    {
      RouteListItemViewHolder(View v)
      {
        text1 = (TextView)v.findViewById(android.R.id.text1);
        text2 = (TextView)v.findViewById(android.R.id.text2);
      }

      /////////////////////////////////////////////////////////////////////////
      public TextView text1;
      public TextView text2;
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  private RouteListFragment m_routeListFragment = new RouteListFragment();
}
