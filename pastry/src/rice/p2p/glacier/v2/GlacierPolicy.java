package rice.p2p.glacier.v2;

import rice.p2p.commonapi.Id;
import rice.p2p.glacier.v2.*;
import rice.p2p.glacier.*;
import rice.Continuation;
import java.io.Serializable;

public interface GlacierPolicy {

  public boolean checkSignature(Manifest manifest, VersionKey key);
  
  public Fragment[] encodeObject(Serializable obj, boolean[] generateFragment);
  
  public Manifest[] createManifests(VersionKey key, Serializable obj, Fragment[] fragments, long expiration);

  public Manifest updateManifest(VersionKey key, Manifest manifest, long newExpiration);

  public Serializable decodeObject(Fragment[] fragments);
 
  public void prefetchLocalObject(VersionKey key, Continuation command);
 
}
