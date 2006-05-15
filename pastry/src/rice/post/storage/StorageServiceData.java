package rice.post.storage;

import java.io.*;
import java.security.*;
import java.util.*;

import rice.p2p.commonapi.*;
import rice.p2p.commonapi.rawserialization.*;
import rice.p2p.past.*;
import rice.p2p.past.gc.*;
import rice.p2p.past.gc.rawserialization.RawGCPastContent;

/**
 * This class is the abstraction of a class used by the storage package to
 * store data.
 *
 * @version $Id$
 */
abstract class StorageServiceData implements RawGCPastContent {
  
  // serialver for backwards compatibility
  private static final long serialVersionUID = 2882784831315993461L;
  
  // default version number for objects which don't have different versions
  public static final long NO_VERSION = 0L;

  // The data stored in this content hash object.
  protected transient byte[] data;

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
   * Returns the metadata which should be stored with this object.  Allows applications
   * to add arbitrary items into the object's metadata.
   *
   * @param The local GCPast service which the content is on.
   * @return the handle
   */
  public GCPastMetadata getMetadata(long expiration) {
    return new GCPastMetadata(expiration);
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
   * Returns the version number of this object
   *
   * @return The version number
   */
  public long getVersion() {
    return NO_VERSION;
  }
  
  /**
    * Produces a handle for this content object. The handle is retrieved and returned to the
   * client as a result of the Past.lookupHandles() method.
   *
   * @param The local Past service which the content is on.
   * @return the handle
   */
  public PastContentHandle getHandle(Past local) {
    return new StorageServiceDataHandle(local.getLocalNodeHandle(), location, getVersion(), GCPastImpl.DEFAULT_EXPIRATION, local.getEnvironment());
  }
  
  /**
   * Produces a handle for this content object. The handle is retrieved and returned to the
   * client as a result of the Past.lookupHandles() method.
   *
   * @param The local Past service which the content is on.
   * @return the handle
   */
  public GCPastContentHandle getHandle(GCPast local, long expiration) {
    return new StorageServiceDataHandle(local.getLocalNodeHandle(), location, getVersion(), expiration, local.getEnvironment());
  }

  /**
   * Force subclasses to override equals
   *
   * @return Whether this and o are equal
   */
  public abstract boolean equals(Object o);
  
  /**
   * Internal method for writing out this data object
   *
   * @param oos The current output stream
   */
  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.defaultWriteObject();
    
    oos.writeInt(data.length);
    oos.write(data);
  }
  
  /**
   * Internal method for reading in this data object
   *
   * @param ois The current input stream
   */
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    
    data = new byte[ois.readInt()];
    ois.readFully(data, 0, data.length);
  }
  
  
  public StorageServiceData(InputBuffer buf, Endpoint endpoint) throws IOException {
    location = endpoint.readId(buf, buf.readShort()); 
    
    data = new byte[buf.readInt()];
    buf.read(data);
  }
  
  public void serialize(OutputBuffer buf) throws IOException {
    buf.writeShort(location.getType());
    location.serialize(buf);
    
    buf.writeInt(data.length);
    buf.write(data,0,data.length);
  }
}
