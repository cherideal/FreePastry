/*************************************************************************

"Free Pastry" Peer-to-Peer Application Development Substrate 

Copyright 2002, Rice University. All rights reserved. 

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither  the name  of Rice  University (RICE) nor  the names  of its
contributors may be  used to endorse or promote  products derived from
this software without specific prior written permission.

This software is provided by RICE and the contributors on an "as is"
basis, without any representations or warranties of any kind, express
or implied including, but not limited to, representations or
warranties of non-infringement, merchantability or fitness for a
particular purpose. In no event shall RICE or contributors be liable
for any direct, indirect, incidental, special, exemplary, or
consequential damages (including, but not limited to, procurement of
substitute goods or services; loss of use, data, or profits; or
business interruption) however caused and on any theory of liability,
whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even
if advised of the possibility of such damage.

********************************************************************************/
package rice.p2p.past.gc;

import java.math.*;
import java.security.*;
import java.util.*;

import rice.p2p.commonapi.*;

/**
 * @(#) GCIdSet.java
 *
 * Internal representation of a set of GCIds
 * 
 * @version $Id$
 *
 * @author Alan Mislove
 * @author Peter Druschel
 */
public class GCIdSet implements IdSet {
  
  // internal representation of the ids
  protected TreeSet ids;
  
  /**
   * Constructor
   */
  protected GCIdSet() {
    this.ids = new TreeSet();
  }
  
  /**
   * Constructor
   */
  protected GCIdSet(TreeSet set) {
    this.ids = new TreeSet(set);
  }
  
  /**
   * return the number of elements
   */
  public int numElements() {
    return ids.size();
  }
  
  /**
   * add a member
   * @param id the id to add
   */
  public void addId(Id id) {
    removeId(new GCMatchingId(((GCId) id).getId())); 
    doAddId(id);
  }
  
  /**
   * add a member
   * @param id the id to add
   */
  protected void doAddId(Id id) {
    ids.add(id);
  }
  
  /**
    * remove a member
   * @param id the id to remove
   */
  public void removeId(Id id) {
    ids.remove(id);
  }
  
  /**
    * test membership
   * @param id the id to test
   * @return true of id is a member, false otherwise
   */
  public boolean isMemberId(Id id) {
    return ids.contains(id);
  }
  
  /**
   * return a subset of this set, consisting of the member ids in a given range
   * @param from the lower end of the range (inclusive)
   * @param to the upper end of the range (exclusive)
   * @return the subset
   */
  public IdSet subSet(IdRange range) {
    if (range == null)
      return new GCIdSet(ids);
    
    GCIdSet res;
    
    if (range.getCCWId().compareTo(range.getCWId()) <= 0) {
      res = new GCIdSet((TreeSet) ids.subSet(range.getCCWId(), range.getCWId()));
    } else {
      res = new GCIdSet((TreeSet) ids.tailSet(range.getCCWId()));
      res.ids.addAll(ids.headSet(range.getCWId()));
    }
    
    return res;
  }
  
  /**
   * return an iterator over the elements of this set
   * @return the interator
   */
  public Iterator getIterator() {
    return ids.iterator();
  }
  
  /**
   * return this set as an array
   * @return the array
   */
  public Id[] asArray() {
    return (Id[]) ids.toArray(new Id[0]);
  }
  
  /**
   * return a hash of this set
   *
   * @return the hash of this set
   */
  public byte[] hash() {
    MessageDigest md = null;
    try {
      md = MessageDigest.getInstance("SHA");
    } catch (NoSuchAlgorithmException e) {
      System.err.println("No SHA support!");
      return null;
    }
    
    Id[] array = asArray();
    Arrays.sort(array);
         
    for (int i=0; i<array.length; i++) {
      GCId id = (GCId) array[i];
      md.update(id.getId().toByteArray());
      md.update(new BigInteger("" + id.getExpiration()).toByteArray());
    }
    
    return md.digest();
  }
  
  /**
   * Determines equality
   *
   * @param other To compare to
   * @return Equals
   */
  public boolean equals(Object o) {
    GCIdSet other = (GCIdSet) o;
    
    if (numElements() != other.numElements())
      return false;
    
    Iterator i = ids.iterator();
    while (i.hasNext())
      if (! other.isMemberId((Id) i.next()))
        return false;
    
    return true;
  }
  
  /**
   * Returns the hashCode
   *
   * @return hashCode
   */
  public int hashCode() {
    return ids.hashCode();
  }
  
  /**
   * Prints out the string
   *
   * @return A string
   */
  public String toString() {
    return "{GCIdSet of size " + numElements() + "}";
  }
  
  /**
   * Clones this object
   *
   * @return a clone
   */
  public Object clone() {
    return new GCIdSet(ids);
  }
  
  protected static class GCMatchingId extends GCId {
    
    public GCMatchingId(Id id) {
      super(id, 0L);
    }
    
    public boolean equals(Object o) {
      return ((GCId) o).id.equals(id);
    }
    
    public long getExpiration() {
      throw new IllegalArgumentException("getExpriation called on GCMathcing ID!");
    }
    
    public byte[] toByteArray() {
      throw new IllegalArgumentException("toByte[] called on GCMathcing ID!");
    }
  }
}
