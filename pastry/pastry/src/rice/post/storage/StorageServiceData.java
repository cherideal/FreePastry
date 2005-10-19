package rice.post.storage;

import java.io.*;
import java.security.*;
import java.util.*;

import rice.p2p.commonapi.*;
import rice.p2p.past.*;

/**
 * This class is the abstraction of a class used by the storage package to
 * store data.
 *
 * @version $Id$
 */
abstract class StorageServiceData implements PastContent {

  // The data stored in this content hash object.
  protected byte[] data;

  // The location where the data is stored
  protected Id location;

  /**
   * Builds a StorageServiceData from a location and a byte array
   *
   * @param location The location
   * @param data The data to store
   */
  public StorageServiceData(Id location, byte[] data) {
    this.location = location;
    this.data = data;
  }

  /**
   * Returns the location of this data
   *
   * @return The location of this data.
   */
  public rice.p2p.commonapi.Id getId() {
    return location;
  }

  /**
   * Returns the internal array of data
   *
   * @return The byte array of actual data.
   */
  public byte[] getData() {
    return data;
  }

  /**
   * Force subclasses to override equals
   *
   * @return Whether this and o are equal
   */
  public abstract boolean equals(Object o);
  
  /**
   * Produces a handle for this content object. The handle is retrieved and returned to the
   * client as a result of the Past.lookupHandles() method.
   *
   * @param The local Past service which the content is on.
   * @return the handle
   */
  public PastContentHandle getHandle(Past local) {
    return new StorageServiceDataHandle(local.getLocalNodeHandle(), location);
  }
}