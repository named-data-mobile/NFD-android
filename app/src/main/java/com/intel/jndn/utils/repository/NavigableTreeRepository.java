/*
 * jndn-utils
 * Copyright (c) 2015, Intel Corporation.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the GNU Lesser General Public License,
 * version 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 */
package com.intel.jndn.utils.repository;

import java.util.NavigableMap;
import java.util.TreeMap;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

/**
 * Store {@link Data} packets in a {@link TreeMap}; this is an initial concept
 * class and should not be used in production. In tests, see
 * RepositoryTest.java, this class is faster on retrieval but not enough to make
 * up for its slow put().
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class NavigableTreeRepository implements Repository {

  private NavigableMap<Name, PossibleData> storage = new TreeMap<>();

  /**
   * Helper data structure
   */
  private class PossibleData {

    public PossibleData() {
      // no data provided
    }

    public PossibleData(Data data) {
      this.data = data;
    }
    public Data data;

    public boolean hasData() {
      return data != null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void put(Data data) {
    storage.put(data.getName(), new PossibleData(data));

    Name name = data.getName();
    do {
      name = name.getPrefix(-1);
      if (storage.get(name) == null) {
        storage.put(name, new PossibleData());
      }
    } while (name.size() > 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Data get(Interest interest) throws DataNotFoundException {
    Data data;
    if (interest.getChildSelector() == Interest.CHILD_SELECTOR_LEFT) {
      data = getLowest(interest);
    } else if (interest.getChildSelector() == Interest.CHILD_SELECTOR_RIGHT) {
      data = getHighest(interest);
    } else {
      data = getFirstMatching(interest);
    }
    checkForNull(data);
    return data;
  }

  /**
   * @param interest the {@link Interest} to search with
   * @return the lowest matching {@link Data} packet
   */
  private Data getLowest(Interest interest) {
    PossibleData found = storage.get(interest.getName());

    Name name = interest.getName();
    while (found != null && interest.matchesName(name)) {
      name = storage.lowerKey(name);
      found = (name != null) ? storage.get(name) : null;
    }

    return found == null ? null : found.data;
  }

  /**
   * @param interest the {@link Interest} to search with
   * @return the highest matching {@link Data} packet
   */
  private Data getHighest(Interest interest) {
    PossibleData found = storage.get(interest.getName());

    if (found != null) {
      Name name = interest.getName();
      while (name != null && interest.matchesName(name)) {
        found = storage.get(name);
        name = storage.higherKey(name);
      }
    }

    return found == null ? null : found.data;
  }

  /**
   * @param interest the {@link Interest} to search with
   * @return the first matching {@link Data} packet
   */
  private Data getFirstMatching(Interest interest) {
    PossibleData found = storage.get(interest.getName());

    Name name = interest.getName();
    while (found != null && !found.hasData() && interest.matchesName(name)) {
      name = storage.higherKey(name);
      found = (name != null) ? storage.get(name) : null;
    }

    return found == null ? null : found.data;
  }

  /**
   * @param data the {@link Data} packet to check
   * @throws DataNotFoundException if data is null
   */
  private void checkForNull(Data data) throws DataNotFoundException {
    if (data == null) {
      throw new DataNotFoundException();
    }
  }

}
