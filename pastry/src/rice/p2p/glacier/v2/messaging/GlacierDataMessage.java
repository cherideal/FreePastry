package rice.p2p.glacier.v2.messaging;

import rice.*;
import rice.p2p.commonapi.*;
import rice.p2p.glacier.v2.Manifest;
import rice.p2p.glacier.Fragment;
import rice.p2p.glacier.FragmentKey;

public class GlacierDataMessage extends GlacierMessage {

  protected FragmentKey[] keys;
  protected Fragment[] fragments;
  protected Manifest[] manifests;

  public GlacierDataMessage(int uid, FragmentKey key, Fragment fragment, Manifest manifest, NodeHandle source, Id dest, boolean isResponse, char tag) {
    this(uid, new FragmentKey[] { key }, new Fragment[] { fragment }, new Manifest[] { manifest }, source, dest, isResponse, tag);
  }

  public GlacierDataMessage(int uid, FragmentKey[] keys, Fragment[] fragments, Manifest[] manifests, NodeHandle source, Id dest, boolean isResponse, char tag) {
    super(uid, source, dest, isResponse, tag);

    this.keys = keys;
    this.fragments = fragments;
    this.manifests = manifests;
  }

  public int numKeys() {
    return keys.length;
  }

  public FragmentKey getKey(int index) {
    return keys[index];
  }

  public Fragment getFragment(int index) {
    return fragments[index];
  }

  public Manifest getManifest(int index) {
    return manifests[index];
  }

  public String toString() {
    return "[GlacierData for " + keys[0] + " ("+(numKeys()-1)+" more keys)]";
  }
}

