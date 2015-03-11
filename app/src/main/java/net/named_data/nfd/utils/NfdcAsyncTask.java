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

package net.named_data.nfd.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;


public class NfdcAsyncTask extends AsyncTask<Void, Void, String> {

  public NfdcAsyncTask(Context context, Task task)
  {
    m_context = context;
    m_progressBar = new ProgressDialog(m_context);
    m_task = task;
  }

  public interface Task {
    public String
    runTask() throws Exception;
  }

  @Override
  protected String
  doInBackground(Void... params)
  {
    try {
      return m_task.runTask();
    }
    catch (Exception e) {
      return "Error communicating with NFD (" + e.getMessage() + ")";
    }
  }

  @Override
  protected void
  onPostExecute(String result)
  {
    m_progressBar.dismiss();
    if (result != null)
      Toast.makeText(m_context.getApplicationContext(), result, Toast.LENGTH_LONG).show();
  }

  @Override
  protected void
  onPreExecute()
  {
    m_progressBar.setMessage("Communicating with NFD...");
    m_progressBar.show();
  }

  @Override
  protected void
  onProgressUpdate(Void... values)
  {
  }

  /////////////////////////////////////////////////////////////////////////////

  private Context m_context = null;
  private ProgressDialog m_progressBar = null;
  private Task m_task = null;
}
