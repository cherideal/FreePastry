/*
 * Created on Aug 2, 2004
 */
package rice.visualization.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author Jeff Hoye
 */
public class FileCommandHandler implements DebugCommandHandler {

  public static final String GET_PROPS_COMMAND = "pastry.getproperties";
  public static final String GET_MANIFEST_COMMAND = "pastry.getmanifest";

  public static final String DEFAULT_PARAMS_FILE = "proxy.params";
  public static final String DEFAULT_JAR_FILE = "pastry.jar";
  

  public String handleDebugCommand(String command) {
    
    if (command.startsWith(GET_PROPS_COMMAND)) {
      return handlePropsCmd(command);
    }
    if (command.startsWith(GET_MANIFEST_COMMAND)) {
      return handleManifestCmd(command);
    }
    
    return null;
  }

  protected String handleManifestCmd(String command) {
    String filename = DEFAULT_JAR_FILE;
    String arg = getArg(command, GET_MANIFEST_COMMAND.length());
    if (arg != null) {
      filename = arg;
    }    
    try {  
      File f = new File(filename);
      if (!f.exists()) {
        return "Could not find file of name \""+filename+"\"";
      }
      JarFile jf = new JarFile(f);
      Manifest m = jf.getManifest();
      Map map = m.getEntries();
      Iterator i = map.keySet().iterator();
      String ret = "Jar "+filename+":\n";
      while (i.hasNext()) {
        Object key = i.next();
        Object val = map.get(key);
        ret+=key+" = " +val+"\n";
      }
      ret+="FileSize:"+f.length()+" Last Modified:"+new Date(f.lastModified());
      return ret;
    } catch (Throwable t) {
      t.printStackTrace();
      return "ERROR: opening \""+filename+"\":"+t.getMessage();
    }      
  }
  
  protected String handlePropsCmd(String command) {
    String filename = DEFAULT_PARAMS_FILE;
    String arg = getArg(command, GET_PROPS_COMMAND.length());
    if (arg != null) {
      filename = arg;
    }
    try {  
      File f = new File(filename);
      if (!f.exists()) {
        return "Could not find file of name \""+filename+"\"";
      }
      FileInputStream fis = new FileInputStream(f);
      InputStreamReader isr = new InputStreamReader(fis);
      BufferedReader br = new BufferedReader(isr);
      String ret = "";
      String s = br.readLine();
      while (s != null) {
        ret+=s+"\n";
        s = br.readLine();
      } 
      return ret;
    } catch (Throwable t) {
      t.printStackTrace();
      return "ERROR: opening \""+filename+"\":"+t.getMessage();
    }
  }

  private String getArg(String command, int l) {
    int i = command.indexOf(" ");
    if (i >= l) {
      String sub = command.substring(i,command.length());
      // strip off leading space
      while (sub.startsWith(" ")) {
        sub = sub.substring(1);
      }
      
      if (sub.length() >= 1)
        return sub;
    }    
    return null;
  }
}