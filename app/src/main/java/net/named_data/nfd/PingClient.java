/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2015-2016 Regents of the University of California
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

import android.os.Handler;
import android.os.HandlerThread;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.NetworkNack;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnNetworkNack;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.nfd.utils.G;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

class PingClient {

  interface PingClientListener {
    void onPingResponse(final String prefix, final long seq, final double elapsedTime);

    void onPingTimeout(final String prefix, final long seq);

    void onPingNack(final String prefix, final long seq, final NetworkNack.Reason reason);

    void onCalcStatistics(final String msg);

    void onPingFinish();
  }

  private class PingStats implements Serializable {
    int dataCount = 0;
    int timeoutCount = 0;
    int nackCount = 0;

    double timeSum = 0;
    double timeMin = Double.MAX_VALUE;
    double timeMax = 0;
    double timeSquareSum = 0;
  }

  private PingStats m_pingStats;
  private PingClientListener m_listener;
  private String m_pingPrefix;
  private long m_pingSeq;
  private AtomicBoolean m_isRunning = new AtomicBoolean(false);
  private Handler m_handler;
  private Face m_face;
  private HandlerThread m_thread;

  /////////////////////////////////////////////////////////////////////////

  PingClient(String pingPrefix)
  {
    m_pingPrefix = pingPrefix;
    m_pingStats = new PingStats();
  }

  PingClient(String pingPrefix, Serializable savedStats)
  {
    m_pingPrefix = pingPrefix;
    m_pingStats = (PingStats)savedStats;
  }

  Serializable getState() {
    return m_pingStats;
  }

  void setListener(PingClientListener listener) {
    m_listener = listener;
  }

  public void start() {
    m_thread = new HandlerThread("PingClientThread");
    m_thread.start();
    m_handler = new Handler(m_thread.getLooper());
    m_handler.post(new Runnable() {
      @Override
      public void run()
      {
        initiate();
      }
    });
  }

  public void stop() {
    m_isRunning.set(false);
    // thread will be killed shorty, but don't wait to so we not going to block UI
  }

  /////////////////////////////////////////////////////////////////////////

  private void notifyPingResponse(String prefix, long seq, double elapsedTime) {
    if (m_listener == null) return;
    m_listener.onPingResponse(prefix, seq, elapsedTime);
  }

  private void notifyPingTimeout(String prefix, long seq) {
    if (m_listener == null) return;
    m_listener.onPingTimeout(prefix, seq);
  }

  private void notifyPingNack(String prefix, long seq, NetworkNack.Reason reason) {
    if (m_listener == null) return;
    m_listener.onPingNack(prefix, seq, reason);
  }

  private void onCalcStatistics(String msg) {
    if (m_listener == null) return;
    m_listener.onCalcStatistics(msg);
  }

  private void notifyPingFinish() {
    if (m_listener == null) return;
    m_listener.onPingFinish();
  }

  private void calculateStatistics() {
    int count = m_pingStats.dataCount + m_pingStats.timeoutCount + m_pingStats.nackCount;
    if (count == 0) {
      return;
    }
    StringBuilder stat = new StringBuilder();
    stat
      .append("--- ").append(m_pingPrefix).append(" ping statistics ---")
      .append("\n")
      .append(count).append(" packets transmitted, ")
      .append(m_pingStats.dataCount).append(" received, ")
      .append(m_pingStats.timeoutCount * 100 / count).append("% packet loss, ")
      .append("time ")
      .append(String.format(Locale.getDefault(), "%.3f", m_pingStats.timeSum))
      .append(" ms");

    if (m_pingStats.dataCount != 0 && m_pingStats.timeSum > 0) {
      double avg = m_pingStats.timeSum * 1.0 / m_pingStats.dataCount;
      double mdev = Math.sqrt(m_pingStats.timeSquareSum / m_pingStats.dataCount - avg * avg);
      stat
        .append("\n")
        .append("rtt min/avg/max/mdev = ")
        .append(String.format(Locale.getDefault(), "%.3f", m_pingStats.timeMin))
        .append("/")
        .append(String.format(Locale.getDefault(), "%.4f", avg))
        .append("/")
        .append(String.format(Locale.getDefault(), "%.3f", m_pingStats.timeMax))
        .append("/")
        .append(String.format(Locale.getDefault(), "%.4f", mdev)).append(" ms");
    }

    onCalcStatistics(stat.toString());
  }

  private void initiate() {
    G.Log("INITIATE ping");
    m_isRunning.set(true);
    m_pingSeq = Math.abs(new Random().nextLong());

    m_face = new Face();
    requestNextPing(0);
    processEvents();
  }

  private void processEvents() {
    try {
      if (m_isRunning.get()) {
        m_face.processEvents();
        m_handler.postDelayed(new Runnable() {
          @Override
          public void run()
          {
            processEvents();
          }
        }, 100);
      }
      else {
        terminate();
      }
    }
    catch (IOException e) {
      G.Log("Face error: " + e.getMessage());
    }
    catch (EncodingException e) {
      G.Log("Encoding error: " + e.getMessage());
    }
  }

  private void terminate() {
    G.Log("TERMINATE ping, " + m_face.hashCode());
    calculateStatistics();
    notifyPingFinish();

    m_face.shutdown();
    m_face = null;
    m_isRunning.set(false);
    m_thread.quit();
  }

  private void requestNextPing(long delay) {
    m_handler.postDelayed(new Runnable() {
      @Override
      public void run()
      {
        newPing();
      }
    }, delay);
  }

  private void newPing()
  {
    if (m_face == null) {
      G.Log("Requested new ping, but face is not available");
      return;
    }
    Name name = new Name(m_pingPrefix + "/ping/" + m_pingSeq++);
    Interest interest = new Interest(name, 1000);
    interest.setMustBeFresh(true);

    final long startTime = System.nanoTime();
    try {
      m_face.expressInterest(interest,
                             new OnData() {
                               @Override
                               public void onData(Interest interest, Data data)
                               {
                                 double elapsedTime = (System.nanoTime() - startTime) / 1000000.0;
                                 ++m_pingStats.dataCount;
                                 m_pingStats.timeSum += elapsedTime;
                                 m_pingStats.timeSquareSum += elapsedTime * elapsedTime;
                                 if (elapsedTime > m_pingStats.timeMax)
                                   m_pingStats.timeMax = elapsedTime;
                                 if (elapsedTime < m_pingStats.timeMin)
                                   m_pingStats.timeMin = elapsedTime;

                                 // Send a result to Screen
                                 notifyPingResponse(m_pingPrefix, m_pingSeq, elapsedTime);
                                 requestNextPing(1000);
                               }
                             },
                             new OnTimeout() {
                               @Override
                               public void onTimeout(Interest interest)
                               {
                                 ++m_pingStats.timeoutCount;

                                 notifyPingTimeout(m_pingPrefix, m_pingSeq);
                                 requestNextPing(0);
                               }
                             },
                             new OnNetworkNack() {
                               @Override
                               public void onNetworkNack(Interest interest, NetworkNack networkNack)
                               {
                                 ++m_pingStats.nackCount;

                                 notifyPingNack(m_pingPrefix, m_pingSeq, networkNack.getReason());
                                 requestNextPing(1000);
                               }
                             });
    }
    catch (IOException e) {
      G.Log("Error expressing the interest: " + e.getMessage());
    }
  }
}
