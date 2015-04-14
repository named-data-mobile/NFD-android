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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.EditText;

import net.named_data.jndn.Name;

public class RouteCreateDialogFragment extends DialogFragment
{
  public static interface OnRouteCreateRequested {
    public void
    createRoute(Name prefix, String faceUri);
  }

  public static RouteCreateDialogFragment
  newInstance() {
    return new RouteCreateDialogFragment();
  }

  @NonNull @Override
  public Dialog
  onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    LayoutInflater inflater = getActivity().getLayoutInflater();
    builder
      .setView(inflater.inflate(R.layout.dialog_create_route, null))
      .setPositiveButton(R.string.route_add_dialog_create_route, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id)
        {
          EditText prefixBox = (EditText) getDialog().findViewById(R.id.prefix);
          EditText uriBox = (EditText) getDialog().findViewById(R.id.faceUri);
          final String prefix = prefixBox.getText().toString();
          final String uri = uriBox.getText().toString();

          ((OnRouteCreateRequested)getTargetFragment()).createRoute(new Name(prefix), uri);
        }
      })
      .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id)
        {
          RouteCreateDialogFragment.this.getDialog().cancel();
        }
      })
    ;

    Dialog faceCreateDialog = builder.create();
    faceCreateDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    return faceCreateDialog;
  }
}
