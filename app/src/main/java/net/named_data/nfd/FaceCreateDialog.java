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
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.EditText;

import net.named_data.jndn_xx.util.FaceUri;
import net.named_data.nfd.utils.Nfdc;
import net.named_data.nfd.utils.NfdcAsyncTask;

public class FaceCreateDialog extends DialogFragment
{
  @Override
  public Dialog
  onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    LayoutInflater inflater = getActivity().getLayoutInflater();
    builder
      .setView(inflater.inflate(R.layout.create_face, null))
      .setPositiveButton("Create face", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id)
          {
            EditText uriBox = (EditText) getDialog().findViewById(R.id.faceUri);
            final String uri = uriBox.getText().toString();
            new NfdcAsyncTask(getActivity(),
                              new NfdcAsyncTask.Task() {
                                public String
                                runTask() throws Exception
                                {
                                  try {
                                    Nfdc nfdc = new Nfdc();
                                    int faceId = nfdc.faceCreate(m_uri);
                                    nfdc.shutdown();
                                    return "OK. Face id: " + String.valueOf(faceId);
                                  } catch (FaceUri.CanonizeError e) {
                                    return "Error creating face (" + e.getMessage() + ")";
                                  } catch (FaceUri.Error e) {
                                    return "Error creating face (" + e.getMessage() + ")";
                                  }
                                }

                                ///////////////////////////
                                private String m_uri = uri;
                              }).execute();
          }
        })
      .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id)
          {
            FaceCreateDialog.this.getDialog().cancel();
          }
        })
    ;

    Dialog faceCreateDialog = builder.create();
    faceCreateDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    return faceCreateDialog;
  }
}
