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
import android.util.SparseArray;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.enums.FacePersistency;
import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.ForwarderStatus;
import com.intel.jndn.management.types.RibEntry;
import com.intel.jndn.management.types.Route;

import net.named_data.jndn.ControlParameters;
import net.named_data.jndn.Face;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.*;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;
import net.named_data.jndn_xx.util.FaceUri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NfdcHelper
{
  public NfdcHelper()
  {
    m_face = new Face("localhost");
    try {
      m_face.setCommandSigningInfo(s_keyChain, s_keyChain.getDefaultCertificateName());
    }
    catch (SecurityException e) {
      // shouldn't really happen
      /// @todo add logging
    }
  }

  public void
  shutdown()
  {
    m_face.shutdown();
  }

  /**
   * Get general NFD status
   */
  public ForwarderStatus
  generalStatus() throws Exception
  {
    return Nfdc.getForwarderStatus(m_face);
  }

  /**
   * Registers name to the given faceId or faceUri
   */
  public void
  ribRegisterPrefix(Name prefix,
                    int faceId,
                    int cost,
                    boolean isChildInherit,
                    boolean isCapture) throws Exception
  {
    ForwardingFlags flags = new ForwardingFlags();
    flags.setChildInherit(isChildInherit);
    flags.setCapture(isCapture);
    Nfdc.register(m_face,
                  new ControlParameters()
                    .setName(prefix)
                    .setFaceId(faceId)
                    .setCost(cost)
                    .setForwardingFlags(flags));
  }

  /**
   * Unregisters name from the given faceId/faceUri
   */
  public void
  ribUnregisterPrefix(Name prefix, int faceId) throws ManagementException {
    Nfdc.unregister(m_face,
                    new ControlParameters()
                      .setName(prefix)
                      .setFaceId(faceId));
  }

  /**
   * List all of routes (RIB entries)
   */
  public List<RibEntry>
  ribList() throws ManagementException {
    return Nfdc.getRouteList(m_face);
  }

  public SparseArray<Set<Name>>
  ribAsFaceIdPrefixNameArray() throws ManagementException {
    List<RibEntry> ribEntryList = ribList();
    SparseArray<Set<Name>> faceIdPrefixArray = new SparseArray<>();

    for (RibEntry rib: ribEntryList){
      for (Route route : rib.getRoutes()){
        Set<Name> prefixes = faceIdPrefixArray.get(route.getFaceId(), null);
        if (null == prefixes){
          prefixes = new HashSet<>();
          faceIdPrefixArray.append(route.getFaceId(), prefixes);
        }
        prefixes.add(rib.getName());
      }
    }
    return faceIdPrefixArray;
  }

  /**
   * Creates new face
   * <p>
   * This command allows creation of UDP unicast and TCP faces only
   */
  public int
  faceCreate(String faceUri) throws ManagementException, FaceUri.Error, FaceUri.CanonizeError
  {
    return Nfdc.createFace(m_face, formatFaceUri(faceUri));
  }

  /**
   * Destroys face
   */
  public void
  faceDestroy(int faceId) throws Exception
  {
    Nfdc.destroyFace(m_face, faceId);
  }

  /**
   * List all faces
   */
  public List<FaceStatus>
  faceList(Context context) throws ManagementException
  {
    List<FaceStatus> result = Nfdc.getFaceList(m_face);
    for(FaceStatus one : result) {
      if(PermanentFaceUriAndRouteManager.isPermanentFace(context, one.getFaceId())) {
        one.setFacePersistency(FacePersistency.PERMANENT);
      }
    }
    return result;
  }

  public SparseArray<FaceStatus>
  faceListAsSparseArray(Context context) throws ManagementException {
    List<FaceStatus> faceList = faceList(context);
    SparseArray<FaceStatus> array = new SparseArray<>();
    for (FaceStatus face : faceList){
      array.append(face.getFaceId(), face);
    }
    return array;
  }

  public Map<String, FaceStatus>
  faceListAsFaceUriMap(Context context) throws ManagementException{
    List<FaceStatus> faceList = faceList(context);
    Map<String, FaceStatus> map = new HashMap<>();
    for (FaceStatus face: faceList){
      map.put(face.getRemoteUri(), face);
    }
    return map;
  }

  /**
   * format a faceUri
   */
  public static String formatFaceUri(String faceUri) throws FaceUri.CanonizeError {
    return new FaceUri(faceUri).canonize().toString();
  }

//  /**
//   * Sets the strategy for a namespace
//   */
//  public void
//  strategyChoiceSet(Name namespace, Name strategy)
//  {
//  }
//
//  /**
//   * Unset the strategy for a namespace
//   */
//  public void
//  strategyChoiceUnset(Name namespace)
//  {
//  }

  /////////////////////////////////////////////////////////////////////////////

  private static KeyChain
  configureKeyChain() {
    final MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
    final MemoryPrivateKeyStorage privateKeyStorage = new MemoryPrivateKeyStorage();
    final KeyChain keyChain = new KeyChain(new IdentityManager(identityStorage, privateKeyStorage),
                                           new SelfVerifyPolicyManager(identityStorage));

    Name name = new Name("/tmp-identity");

    try {
      // create keys, certs if necessary
      if (!identityStorage.doesIdentityExist(name)) {
        keyChain.createIdentityAndCertificate(name);

        // set default identity
        keyChain.getIdentityManager().setDefaultIdentity(name);
      }
    }
    catch (SecurityException e){
      // shouldn't really happen
      /// @todo add logging
    }

    return keyChain;
  }

  /////////////////////////////////////////////////////////////////////////////

  final static KeyChain s_keyChain = configureKeyChain();
  private Face m_face;
}
