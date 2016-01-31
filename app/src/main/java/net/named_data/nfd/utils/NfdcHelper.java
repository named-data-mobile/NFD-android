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

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.ForwarderStatus;
import com.intel.jndn.management.types.RibEntry;

import net.named_data.jndn.ControlParameters;
import net.named_data.jndn.Face;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.*;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;
import net.named_data.jndn_xx.util.FaceUri;

import java.util.List;

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
  ribRegisterPrefix(Name prefix, int faceId, int cost, boolean isChildInherit, boolean isCapture) throws Exception
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
  ribUnregisterPrefix(Name prefix, int faceId) throws Exception
  {
    Nfdc.unregister(m_face,
                    new ControlParameters()
                      .setName(prefix)
                      .setFaceId(faceId));
  }

  /**
   * List all of routes (RIB entries)
   */
  public List<RibEntry>
  ribList() throws Exception
  {
    return Nfdc.getRouteList(m_face);
  }

  /**
   * Creates new face
   * <p>
   * This command allows creation of UDP unicast and TCP faces only
   */
  public int
  faceCreate(String faceUri) throws ManagementException, FaceUri.Error, FaceUri.CanonizeError
  {
    return Nfdc.createFace(m_face, new FaceUri(faceUri).canonize().toString());
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
  faceList() throws Exception
  {
    return Nfdc.getFaceList(m_face);
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
