/*
 * Created on Nov 22, 2006
 */
package rice.pastry.socket.appsocket;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import rice.p2p.commonapi.exception.*;
import rice.p2p.util.MathUtils;
import rice.pastry.socket.*;

public class BlockingAppSocketFactory {

  /**
   * 12 bytes, contains the magic number, the version number, and the HEADER_DIRECT
   */
  public static final byte[] magic_version_direct;
  
  static {
    int mvdLength = 
      SocketCollectionManager.PASTRY_MAGIC_NUMBER.length+
      4+ // version
      SocketCollectionManager.HEADER_DIRECT.length;

    // allocate it
    magic_version_direct = new byte[mvdLength];

    // copy in magic number
    System.arraycopy(SocketCollectionManager.PASTRY_MAGIC_NUMBER,0,magic_version_direct,0,SocketCollectionManager.PASTRY_MAGIC_NUMBER.length);
    
    // handle the version number
    // TODO: do this if you use a different version number
    
    // copy in HEADER_DIRECT
    System.arraycopy(SocketCollectionManager.HEADER_DIRECT,0,magic_version_direct,8,SocketCollectionManager.HEADER_DIRECT.length);    
  }
  
  // the size of the buffers for the socket
  private int SOCKET_BUFFER_SIZE = 32768;

  public BlockingAppSocketFactory() {
    
  }
  
  public SocketChannel connect(InetSocketAddress addr, int appId) throws IOException, AppSocketException {
    SocketChannel channel;
    channel = SocketChannel.open();
    channel.socket().setSendBufferSize(SOCKET_BUFFER_SIZE);
    channel.socket().setReceiveBufferSize(SOCKET_BUFFER_SIZE);
    
    channel.connect(addr);
/*
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    +                   PASTRY_MAGIC_NUMBER                         + 
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    +                   version number 0                            + 
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ =}
    +                   HEADER_SOURCE_ROUTE                         +   }
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+   }
    +            Next Hop (EpochInetSocketAddress)                  +    > // zero or more 
    +                                                               +    >           
    +                                                               +   }           
    +                                                               +   }           
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ =}
    +                      HEADER_DIRECT                            +   
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+   
    +                          AppId                                +   
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+   
*/
    ByteBuffer bb[] = new ByteBuffer[2];
    bb[0] = ByteBuffer.wrap(magic_version_direct);
    bb[1] = ByteBuffer.wrap(MathUtils.intToByteArray(appId));
    channel.write(bb);

    // read result
    ByteBuffer answer = ByteBuffer.allocate(1);
    channel.read(answer);
    answer.clear();
    byte connectResult = answer.get();
    
    //System.out.println(this+"Read "+connectResult);
    switch(connectResult) {
      case SocketAppSocket.CONNECTION_OK:
        break;
      case SocketAppSocket.CONNECTION_NO_APP:
        throw new AppNotRegisteredException();
      case SocketAppSocket.CONNECTION_NO_ACCEPTOR:
        throw new NoReceiverAvailableException();            
      default:
        throw new AppSocketException("Unknown error "+connectResult);
    }
    
    
    return channel;
  }  
}
