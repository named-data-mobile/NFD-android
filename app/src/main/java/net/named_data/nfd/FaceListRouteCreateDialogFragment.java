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
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;

import net.named_data.jndn.Name;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class FaceListRouteCreateDialogFragment extends DialogFragment {

  public static interface OnFaceListRouteCreateRequestd {
    public void
    createRoute(Name prefix, int faceId, boolean isPermanent);
  }

  public static FaceListRouteCreateDialogFragment
  newInstance(int faceId) {
    FaceListRouteCreateDialogFragment faceListRouteCreateDialogFragment =
        new FaceListRouteCreateDialogFragment();
    Bundle args = new Bundle();
    args.putInt("faceId", faceId);
    faceListRouteCreateDialogFragment.setArguments(args);
    return faceListRouteCreateDialogFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);
    int faceId = getArguments().getInt("faceId");
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    LayoutInflater inflater = getActivity().getLayoutInflater();
    builder
        .setView(inflater.inflate(R.layout.dialog_create_face_list_add_route, null))
        .setPositiveButton(R.string.route_add_dialog_create_route, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            EditText prefixBox = getDialog().findViewById(R.id.face_list_add_route_prefix);
            CheckBox permanent = getDialog().findViewById(R.id.face_list_add_route_permanent);
            final String prefix = prefixBox.getText().toString();

            ((OnFaceListRouteCreateRequestd) getTargetFragment()).createRoute(new Name(prefix),
                                                                              faceId,
                                                                              permanent.isChecked());
          }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            FaceListRouteCreateDialogFragment.this.getDialog().cancel();
          }
        });

    Dialog faceListRouteCreateDialog = builder.create();
    faceListRouteCreateDialog
        .getWindow()
        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    return faceListRouteCreateDialog;
  }
}
