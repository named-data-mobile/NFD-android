/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2015-2016 Regents of the University of California
 * <p>
 * This file is part of NFD (Named Data Networking Forwarding Daemon) Android.
 * See AUTHORS.md for complete list of NFD Android authors and contributors.
 * <p>
 * NFD Android is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * <p>
 * NFD Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * NFD Android, e.g., in COPYING.md file.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.named_data.nfd.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * The manager to record and delete permanent faceUris and routes
 */
public class PermanentFaceUriAndRouteManager {
  private static final String TAG = "Permanent Manager";
  private static final String PREFS_NAME = "permanent";
  private static final String PERMANENT_FACEURI = "permanentFaceUri";
  private static final String PERMANENT_ROUTE = "permanentRoute";
  private static final String PERMANENT_FACEID = "permanentFaceId";
  private static final String PREFIX_FACEURI_DELIMITER = "\t";
  // We need to cache permanent face IDs in order to display whether a face is permanent face or not.

  @SuppressWarnings("deprecation")
  public static void addPermanentFaceId(Context context, int faceId) {
    SharedPreferences setting = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
    Set<String> permanentFace = setting.getStringSet(PERMANENT_FACEID, new HashSet<String>());
    Set<String> newPermanentFace = new HashSet<>(permanentFace);

    if (newPermanentFace.add(Integer.toString(faceId))) {
      SharedPreferences.Editor editor = setting.edit();
      editor.putStringSet(PERMANENT_FACEID, newPermanentFace);
      editor.commit();
    }
  }

  @SuppressWarnings("deprecation")
  public static void deletePermanentFaceId(Context context, int faceId) {
    SharedPreferences setting = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
    Set<String> permanentFace = setting.getStringSet(PERMANENT_FACEID, new HashSet<String>());
    Set<String> newPermanentFace = new HashSet<>(permanentFace);

    if (newPermanentFace.remove(Integer.toString(faceId))) {
      SharedPreferences.Editor editor = setting.edit();
      editor.putStringSet(PERMANENT_FACEID, newPermanentFace);
      editor.commit();
    }
  }

  @SuppressWarnings("deprecation")
  static boolean isPermanentFace(Context context, int faceId) {
    SharedPreferences setting = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
    Set<String> permanentFace = setting.getStringSet(PERMANENT_FACEID, new HashSet<String>());

    return permanentFace.contains(Integer.toString(faceId));
  }

  @SuppressWarnings("deprecation")
  public static void clearFaceIds(Context context) {
    SharedPreferences setting = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);

    SharedPreferences.Editor editor = setting.edit();
    editor.putStringSet(PERMANENT_FACEID, new HashSet<String>());
    editor.commit();
  }

  @SuppressWarnings("deprecation")
  public static Set<String> getPermanentFaceUris(Context context){
    SharedPreferences setting = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
    return setting.getStringSet(PERMANENT_FACEURI, new HashSet<String>());
  }

  @SuppressWarnings("deprecation")
  public static Set<String[]> getPermanentRoutes(Context context){
    SharedPreferences setting = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
    Set<String> permanentRoutes = setting.getStringSet(PERMANENT_ROUTE, new HashSet<String>());
    Set<String[]> prefixAndFacePairs = new HashSet<>();
    for (String oneRecord : permanentRoutes){
      String[] prefixAndFaceUri = oneRecord.split(PREFIX_FACEURI_DELIMITER);
      prefixAndFacePairs.add(prefixAndFaceUri);
    }
    return prefixAndFacePairs;
  }

  @SuppressWarnings("deprecation")
  public static void addPermanentFaceUri(Context context, String faceUri) {
    SharedPreferences setting = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
    Set<String> permanentFace = setting.getStringSet(PERMANENT_FACEURI, new HashSet<String>());
    Set<String> newPermanentFace = new HashSet<>(permanentFace);

    G.Log(TAG, "Try to record permanent face");
    G.Log(TAG, "Permanent face list has " + permanentFace.size() + " item(s)");
    if (newPermanentFace.add(faceUri)) {
      SharedPreferences.Editor editor = setting.edit();
      editor.putStringSet(PERMANENT_FACEURI, newPermanentFace);
      editor.commit();
    }
  }

  @SuppressWarnings("deprecation")
  public static void addPermanentRoute(Context context, String prefix, String faceUri) {
    SharedPreferences setting = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
    Set<String> permanentRoute = setting.getStringSet(PERMANENT_ROUTE, new HashSet<String>());
    Set<String> newPermanentRoute = new HashSet<>(permanentRoute);

    G.Log(TAG, "Try to record permanent route");
    G.Log(TAG, "Permanent route list has " + permanentRoute.size() + " item(s)");
    if (newPermanentRoute.add(prefix + PREFIX_FACEURI_DELIMITER + faceUri)) {
      SharedPreferences.Editor editor = setting.edit();
      editor.putStringSet(PERMANENT_ROUTE, newPermanentRoute);
      editor.commit();
      G.Log(TAG, "Record permanent route " + faceUri);
    }
  }

  @SuppressWarnings("deprecation")
  public static void deletePermanentFaceUri(Context context, String faceUri) {
    SharedPreferences setting = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
    Set<String> permanentFace = setting.getStringSet(PERMANENT_FACEURI, new HashSet<String>());
    Set<String> newPermanentFace = new HashSet<>(permanentFace);

    G.Log(TAG, "Try to delete permanent face");
    G.Log(TAG, "Permanent face list has " + permanentFace.size() + " item(s)");
    if (newPermanentFace.remove(faceUri)) {
      SharedPreferences.Editor editor = setting.edit();
      editor.putStringSet(PERMANENT_FACEURI, newPermanentFace);
      editor.commit();
      G.Log(TAG, "Delete permanent face " + faceUri);
    } else {
      G.Log(TAG, faceUri + " is not a permanent face");
    }
  }

  @SuppressWarnings("deprecation")
  public static void deletePermanentRoute(Context context, String prefix, String faceUri) {
    SharedPreferences setting = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
    Set<String> permanentRoute = setting.getStringSet(PERMANENT_ROUTE, new HashSet<String>());
    Set<String> newPermanentRoute = new HashSet<>(permanentRoute);

    G.Log(TAG, "Try to delete permanent route");
    G.Log(TAG, "Permanent route list has " + permanentRoute.size() + " item(s)");
    if (newPermanentRoute.remove(prefix + PREFIX_FACEURI_DELIMITER + faceUri)) {
      SharedPreferences.Editor editor = setting.edit();
      editor.putStringSet(PERMANENT_ROUTE, newPermanentRoute);
      editor.commit();
      G.Log(TAG, "Delete permanent route " + prefix + " " + faceUri);
    } else {
      G.Log(TAG, prefix + " " + faceUri + " is not a permanent route");
    }
  }
}
