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

import java.util.ArrayList;
import java.util.List;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

/**
 * Store {@link Data} packets in a linked list and iterate over the list to find
 * the best match; this is a subset of the functionality provided in
 * {@link net.named_data.jndn.util.MemoryContentCache} and borrows the matching
 * logic from there. Code for removing stale packets is not yet implemented.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ForLoopRepository implements Repository {

  private List<Data> storage = new ArrayList<>();

  /**
   * Helper data structure
   */
  private class Record {

    public Name name;
    public Data data;

    public Record(Name name, Data data) {
      this.name = name;
      this.data = data;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void put(Data data) {
    storage.add(data);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Data get(Interest interest) throws DataNotFoundException {
    Name.Component selectedComponent = null;
    Data selectedData = null;
    for (Data content : storage) {
      if (interest.matchesName(content.getName())) {
        if (interest.getChildSelector() < 0) {
          // No child selector, so send the first match that we have found.
          return content;
        } else {
          // Update selectedEncoding based on the child selector.
          Name.Component component;
          if (content.getName().size() > interest.getName().size()) {
            component = content.getName().get(interest.getName().size());
          } else {
            component = new Name.Component();
          }

          boolean gotBetterMatch = false;
          if (selectedData == null) {
            // Save the first match.
            gotBetterMatch = true;
          } else {
            if (interest.getChildSelector() == 0) {
              // Leftmost child.
              if (component.compare(selectedComponent) < 0) {
                gotBetterMatch = true;
              }
            } else {
              // Rightmost child.
              if (component.compare(selectedComponent) > 0) {
                gotBetterMatch = true;
              }
            }
          }

          if (gotBetterMatch) {
            selectedComponent = component;
            selectedData = content;
          }
        }
      }
    }

    if (selectedData != null) {
      // We found the leftmost or rightmost child.
      return selectedData;
    } else {
      throw new DataNotFoundException();
    }
  }
}
