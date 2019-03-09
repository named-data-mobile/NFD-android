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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import net.named_data.nfd.wifidirect.model.Peer;
import net.named_data.nfd.wifidirect.utils.NDNController;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;

import static android.app.ProgressDialog.show;
import static net.named_data.nfd.wifidirect.utils.NDNController.myAddress;
import static net.named_data.nfd.wifidirect.utils.NDNController.myDeviceName;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link WiFiDirectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WiFiDirectFragment extends Fragment {

  private static final String TAG = "WiFiDirectFragment";

  public WiFiDirectFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @return A new instance of fragment WiFiDirectFragment.
   */
  public static WiFiDirectFragment newInstance() {
    return new WiFiDirectFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    m_sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    m_handler = new Handler();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_wifidirect, container, false);

    // init UI elements
    m_wdGroupConnStatus = (TextView) view.findViewById(R.id.wd_group_conn_status_textview);
    m_wdIpAddress = (TextView) view.findViewById(R.id.wd_ip_address_textview);
    m_wdDeviceName = (TextView) view.findViewById(R.id.wd_this_device_name_textview);
    m_wdIsGroupOwner = (TextView) view.findViewById(R.id.wd_group_owner_textview);
    m_wdSwitch = (Switch) view.findViewById(R.id.wd_switch);

    if (m_sharedPreferences.getBoolean(PREF_WIFIDIRECT_STATUS, false)) {
      m_wdSwitch.setChecked(true);
      startNDNOverWifiDirect();
      startUiUpdateLoop();
    } else {
      // the button was off, make any desired UI changes
      m_wdGroupConnStatus.setText("");
    }

    m_wdSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // store state of switch
        m_sharedPreferences.edit().putBoolean(PREF_WIFIDIRECT_STATUS, isChecked).apply();

        if (isChecked) {
          startNDNOverWifiDirect();
          startUiUpdateLoop();
        } else {
          stopNDNOverWifiDirect();
          stopUiUpdateLoop();
          resetUi();
        }
      }
    });

    // list view for displaying peers
    m_wdConnectedPeerListview = (ListView) view.findViewById(R.id.wd_connected_peers_listview);
    m_ConnectedPeers = new ArrayList<>(NDNController.getInstance().getConnectedPeers());
    m_DicoveredPeers = new ArrayList<>(NDNController.getInstance().getDiscoveredPeers());

    m_ConnectedPeersAdapter = new ConnectPeerListAdapter(getActivity(), R.layout.row_devices, m_ConnectedPeers);
    m_wdConnectedPeerListview.setAdapter(m_ConnectedPeersAdapter);
    m_wdConnectedPeerListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Peer selectedPeer = (Peer) parent.getItemAtPosition(position);

        // toast a quick message!
        if (selectedPeer == null) {
          Toast.makeText(getActivity(),
            getResources().getString(R.string.fragment_wifidirect_toast_peer_no_longer_available),
            Toast.LENGTH_LONG).show();
        } else {
          String peerInfo = selectedPeer.getNumProbeTimeouts() == 0 ?
            getResources().getString(R.string.fragment_wifidirect_toast_connection_works_well) :
            getResources().getString(R.string.fragment_wifidirect_toast_didnt_get_response) +
            (selectedPeer.getNumProbeTimeouts() * NDNController.PROBE_DELAY / 1000) +
              getResources().getString(R.string.fragment_wifidirect_toast_seconds);
          Toast.makeText(getActivity(), peerInfo, Toast.LENGTH_LONG).show();
        }
      }
    });

    m_wdDiscoveredPeerListview = (ListView) view.findViewById(R.id.wd_discovered_peers_listview);
    m_DiscoveredPeersAdapter = new DiscoveredPeerListAdapter(getActivity(), R.layout.row_devices, m_DicoveredPeers);
    m_wdDiscoveredPeerListview.setAdapter(m_DiscoveredPeersAdapter);

    m_wdDiscoveredPeerListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final WifiP2pDevice device = (WifiP2pDevice) parent.getItemAtPosition(position);

        if(device.status == WifiP2pDevice.INVITED) {
          AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
          builder.setMessage(getResources().getString(R.string.fragment_wifidirect_dialog_cancel_invitation) + device.deviceName + getResources().getString(R.string.question_mark))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                m_progressDialog = show(getActivity(), getResources().getString(R.string.fragment_wifidirect_dialog_cancelling),
                  getResources().getString(R.string.fragment_wifidirect_dialog_cancelling_invitation) + device.deviceName, true, true
                );
                m_progressDialog.setCancelable(false);

                NDNController.getInstance().cancelConnect();
              }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                return;
              }
            });
          AlertDialog dialog = builder.create();
          dialog.show();
          return;
        }

        if(device.status == WifiP2pDevice.CONNECTED) {
          AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
          String alertMessage = getResources().getString(R.string.fragment_wifidirect_dialog_disconnect_from) + device.deviceName + getResources().getString(R.string.question_mark);
          if(NDNController.getInstance().getIsGroupOwner()) {
            alertMessage = alertMessage + getResources().getString(R.string.fragment_wifidirect_dialog_destroy_group_alter);
          }
          builder.setMessage(alertMessage)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                m_progressDialog = show(getActivity(),  getResources().getString(R.string.fragment_wifidirect_dialog_disconnecting),
                  getResources().getString(R.string.fragment_wifidirect_dialog_disconnecting_from) + device.deviceName, true, true
                );
                m_progressDialog.setCancelable(false);

                NDNController.getInstance().disconnect();
              }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                return;
              }
            });
          AlertDialog dialog = builder.create();
          dialog.show();
          return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getResources().getString(R.string.fragment_wifidirect_dialog_invite) +
          device.deviceName + getResources().getString(R.string.fragment_wifidirect_dialog_join_group)
          + getResources().getString(R.string.question_mark))
          .setCancelable(false)
          .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              m_progressDialog = ProgressDialog.show(getActivity(), getResources().getString(R.string.fragment_wifidirect_dialog_inviting),
                getResources().getString(R.string.fragment_wifidirect_dialog_inviting) + device.deviceName + getResources().getString(R.string.fragment_wifidirect_dialog_join_group), true, true
              );
              m_progressDialog.setCancelable(false);

              NDNController.getInstance().connect(device);
            }
          })
          .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              return;
            }
          });
        AlertDialog dialog = builder.create();
        dialog.show();
      }
    });
    return view;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
  }

  @Override
  public void onDetach() {
    super.onDetach();
  }

  @Override
  public void onPause() {
    super.onPause();
    stopUiUpdateLoop();
  }

  @Override
  public void onResume() {
    super.onResume();
    startUiUpdateLoop();
  }

  private void startNDNOverWifiDirect() {
    // this is set so that NDNController has context to start appropriate services
    NDNController.getInstance().setWifiDirectContext(getActivity());

    // only start if the device is not currently connected to a group
    // otherwise, the protocol must have been running to begin with!
    if (myAddress == null) {
      // main wrapper function that begins all elements of the protocol
      NDNController.getInstance().start();
    }
  }

  private void stopNDNOverWifiDirect() {
    // main wrapper function that stops all elements of the protocol
    NDNController.getInstance().stop();
  }

  private void startUiUpdateLoop() {
    // periodically check for changed state
    // to display to user
    m_handler.post(m_UiUpdateRunnable);
  }

  private void stopUiUpdateLoop() {
    m_handler.removeCallbacks(m_UiUpdateRunnable);
  }

  private void resetUi() {
    // simply resets what is displayed to user
    m_wdIpAddress.setText(getResources().getString(R.string.empty_string));
    m_wdGroupConnStatus.setText(getResources().getString(R.string.empty_string));
    m_wdIsGroupOwner.setText(getResources().getString(R.string.empty_string));
    m_ConnectedPeers.clear();
    m_DicoveredPeers.clear();
    m_ConnectedPeersAdapter.notifyDataSetChanged();
    m_DiscoveredPeersAdapter.notifyDataSetChanged();
  }

  /**
   * Array adapter for ListFragment that maintains WifiP2pDevice list.
   */
  private class ConnectPeerListAdapter extends ArrayAdapter<Peer> {

    private List<Peer> items;

    /**
     * @param context
     * @param textViewResourceId
     * @param objects
     */
    public ConnectPeerListAdapter(Context context, int textViewResourceId,
                                     List<Peer> objects) {
      super(context, textViewResourceId, objects);
      items = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;
      if (v == null) {
        LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
          Context.LAYOUT_INFLATER_SERVICE);
        v = vi.inflate(R.layout.row_devices, null);
      }
      Peer peer = items.get(position);
      if (peer != null) {
        TextView top = (TextView) v.findViewById(R.id.device_name);
        TextView bottom = (TextView) v.findViewById(R.id.device_details);
        if (top != null) {
          top.setText(peer.getDevice() == null ? getResources().getString(R.string.empty_string) : peer.getDevice().deviceName);
        }
        if (bottom != null) {
          bottom.setText(peer.getIpAddress());
        }
      }

      return v;
    }
  }

  /**
   * Array adapter for ListFragment that maintains WifiP2pDevice list.
   */
  private class DiscoveredPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

    private List<WifiP2pDevice> items;

    /**
     * @param context
     * @param textViewResourceId
     * @param objects
     */
    public DiscoveredPeerListAdapter(Context context, int textViewResourceId,
                               List<WifiP2pDevice> objects) {
      super(context, textViewResourceId, objects);
      items = objects;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;
      if (v == null) {
        LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
          Context.LAYOUT_INFLATER_SERVICE);
        v = vi.inflate(R.layout.row_devices, null);
      }
      WifiP2pDevice device = items.get(position);
      if (device != null) {
        TextView top = (TextView) v.findViewById(R.id.device_name);
        TextView bottom = (TextView) v.findViewById(R.id.device_details);
        if (top != null) {
          top.setText(device.deviceName);
        }
        if (bottom != null) {
          bottom.setText(getDeviceStatus(device.status));
        }
      }

      return v;
    }
  }
  ////////////////////////////////////////////////////////////////////////////////////
  private ListView m_wdConnectedPeerListview;
  private ListView m_wdDiscoveredPeerListview;
  private Switch m_wdSwitch;
  private TextView m_wdGroupConnStatus;
  private TextView m_wdIpAddress;
  private TextView m_wdDeviceName;
  private TextView m_wdIsGroupOwner;

  private Handler m_handler;
  private Runnable m_UiUpdateRunnable = new Runnable() {
    @Override
    public void run() {
      if (m_progressDialog != null && m_progressDialog.isShowing()) {
        m_progressDialog.dismiss();
      }
      if (myAddress != null) {
        m_wdGroupConnStatus.setText(getResources().getString(R.string.fragment_wifidirect_text_group_connected));
        m_wdIpAddress.setText(myAddress);
        if(NDNController.getInstance().getIsGroupOwner()) {
          m_wdIsGroupOwner.setText(getResources().getString(R.string.yes));
        } else {
          m_wdIsGroupOwner.setText(getResources().getString(R.string.no));
        }
      } else {
        if (!m_sharedPreferences.getBoolean(PREF_WIFIDIRECT_STATUS, false)) {
          m_wdGroupConnStatus.setText("");
        } else {
          if(NDNController.getInstance().WIFI_STATE == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
            m_wdGroupConnStatus.setText(getResources().getString(R.string.fragment_wifidirect_text_group_scanning));
          else {
            m_wdGroupConnStatus.setText(getResources().getString(R.string.fragment_wifidirect_text_group_wifi_p2p_disabled));
          }
        }

        m_wdIpAddress.setText(getResources().getString(R.string.empty_string));
        m_wdIsGroupOwner.setText(getResources().getString(R.string.empty_string));
      }
      if (myDeviceName != null) {
        m_wdDeviceName.setText(myDeviceName);
      } else {
        m_wdDeviceName.setText(getResources().getString(R.string.empty_string));
      }

      // refresh the list of peers
      m_ConnectedPeers.clear();
      m_DicoveredPeers.clear();
      m_ConnectedPeers.addAll(NDNController.getInstance().getConnectedPeers());
      m_DicoveredPeers.addAll(NDNController.getInstance().getDiscoveredPeers());
      m_ConnectedPeersAdapter.notifyDataSetChanged();
      m_DiscoveredPeersAdapter.notifyDataSetChanged();

      // call again in X seconds
      m_handler.postDelayed(m_UiUpdateRunnable, UI_UPDATE_DELAY_MS);
    }
  };

  private static String getDeviceStatus(int deviceStatus) {
    Log.d(TAG, "Peer status :" + deviceStatus);
    switch (deviceStatus) {
      case WifiP2pDevice.AVAILABLE:
        return "Available";
      case WifiP2pDevice.INVITED:
        return "Invited";
      case WifiP2pDevice.CONNECTED:
        return "Connected";
      case WifiP2pDevice.FAILED:
        return "Failed";
      case WifiP2pDevice.UNAVAILABLE:
        return "Unavailable";
      default:
        return "Unknown";
    }
  }

  private SharedPreferences m_sharedPreferences;
  private List<Peer> m_ConnectedPeers;
  private List<WifiP2pDevice> m_DicoveredPeers;
  private ConnectPeerListAdapter m_ConnectedPeersAdapter;
  private DiscoveredPeerListAdapter m_DiscoveredPeersAdapter;
  private ProgressDialog m_progressDialog;

  private final int UI_UPDATE_DELAY_MS = 5000;

  private static final String PREF_WIFIDIRECT_STATUS = "WIFIDIRECT_STATUS";
}
