
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: ServerInfoDataEntry.java 149 2009-12-30 00:04:42Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

import com.glub.util.*;
import com.glub.secureftp.wrapper.*;

import java.io.*;
import java.text.*;
import java.util.*;

public class ServerInfoDataEntry {
  private PrintStream out = System.out;
  private BufferedReader stdin = 
                           new BufferedReader(new InputStreamReader(System.in));
  private ServerInfo info = null;

  public ServerInfoDataEntry( ServerInfo info ) {
    this.info = info;
  }

  public void getData() throws CommandException { 
    try {
      getServerID();
      getWrapperIP();
      getServerIP();
      getServerPort();
      getImplicitInfo();
      getExplicitInfo();
      getFirewallInfo();
      getEncryptionInfo();
      getAdvancedInfo();
    }
    catch ( IOException ioe ) {
    }
  }

  protected void getServerID() throws IOException {
    ArrayList response = new ArrayList(1);

    if ( getResponse("Enter the server's name", 
                     info.getServerID(), response) ) {
      info.setServerID( ((String)response.get(0)).toString() );
    }
  }

  protected void getWrapperIP() throws IOException {
    ArrayList response = new ArrayList(1);
    if ( getResponse("Enter the IP address of this wrapper",
                     info.getWrapperIP(), response) ) {
      info.setWrapperIP( ((String)response.get(0)).toString() );
    }
  }

  protected void getServerIP() throws IOException {
    ArrayList response = new ArrayList(1);
    if ( getResponse("Enter the IP address of the FTP server",
                     info.getServerIP(), response) ) {
      info.setServerIP( ((String)response.get(0)).toString() );
    }
  }

  protected void getServerPort() throws IOException {
    ArrayList response = new ArrayList(1);
    if ( getInteger(" - Enter the FTP server's port", 
                                  info.getServerPort(), response) ) {
      Integer intResult = ((Integer)response.get(0));
      info.setServerPort( intResult.intValue() );
    }
  }

  protected void getImplicitInfo() throws IOException {
    ArrayList response = new ArrayList(1);
    if ( getBoolean("Do you want to enable implicit SSL support?", 
                    getYN(info.getImplicitEnabled()),
                    response) ) {
      boolean boolResult = ((Boolean)response.get(0)).booleanValue();
      info.setImplicitEnabled( boolResult );
      if ( boolResult && getInteger(" - Enter the port to run on", 
                                    info.getImplicitPort(), response) ) {
        Integer intResult = ((Integer)response.get(0));
        info.setImplicitPort( intResult.intValue() );
      }
    }
  }

  protected void getExplicitInfo() throws IOException {
    ArrayList response = new ArrayList(1);
    if ( getBoolean("Do you want to enable explicit SSL support?", 
                   getYN(info.getExplicitEnabled()),
                    response) ) {
      boolean boolResult = ((Boolean)response.get(0)).booleanValue();
      info.setExplicitEnabled( boolResult );
      int defExpPort = info.getExplicitPort();
      if ( boolResult && getInteger(" - Enter the port to run on", 
                                    defExpPort, response) ) {
        Integer intResult = ((Integer)response.get(0));
        info.setExplicitPort( intResult.intValue() );
      }
    }
  }

  protected void getFirewallInfo() throws IOException {
    ArrayList response = new ArrayList(1);
    if ( getBoolean("Are you behind a firewall?", 
                    getYN(info.getFirewallEnabled()),
                    response) ) {
      boolean boolResult = ((Boolean)response.get(0)).booleanValue();
      info.setFirewallEnabled( boolResult );
      if ( boolResult && getResponse(" - Enter the public facing IP address of the FTP server", 
                                    info.getFirewallIP(), response) ) {
        info.setFirewallIP(((String)response.get(0)).toString());
      }

      if ( boolResult &&  
           getBoolean("Do you want to set a range of ports for " +
                      "data connections?", getYN(true), response) ) {
         boolResult = ((Boolean)response.get(0)).booleanValue();
         if ( boolResult ) {
           if ( getInteger(" - Enter the starting port",
                           info.getPassivePortStart(), response) ) {
              Integer intResult = ((Integer)response.get(0));
              info.setPassivePortStart( intResult.intValue() );
           }

           if ( getInteger(" - Enter the ending port",
                           info.getPassivePortEnd(), response) ) {
              Integer intResult = ((Integer)response.get(0));
              info.setPassivePortEnd( intResult.intValue() );
           }
         }

         if ( info.getPassivePortEnd() - info.getPassivePortStart() < 50 ) {
            System.out.println("WARNING: We recommend a port range of at least 50 ports for a low volume server.");
         }
      }
    }
  }

  protected void getEncryptionInfo() throws IOException {
    ArrayList response = new ArrayList(1);
    if ( info.getExplicitEnabled() &&
         getBoolean("Always encrypt control/command channel during login?", 
                    getYN(info.getEncryptControlChannel()),
                    response) ) {
      boolean boolResult = ((Boolean)response.get(0)).booleanValue();
      info.setEncryptControlChannel( boolResult ); 
    }

    String edc = info.getEncryptDataChannel();
    if ( edc.equals("always") )
      edc = "a";
    else if ( edc.equals("false") )
      edc = "n";
    else
      edc = "y";

    if ( getResponse("Encrypt data channel? [y|n|a]", edc, response) ) {
      String r = ((String)response.get(0)).toString().toLowerCase();
      if (r.startsWith("a"))
        r = "always";
      else if (r.startsWith("n"))
        r = "false";
      else
        r = "true";
        
      info.setEncryptDataChannel( r );
    }
  }

  protected void getEmail() throws IOException {
    ArrayList response = new ArrayList(1);

    if ( getResponse("Enter an email address", 
                     info.getEmail(), response) ) {
      info.setEmail( ((String)response.get(0)).toString() );
    }
  }

  protected void getAdvancedInfo() throws IOException {
    ArrayList response = new ArrayList(1);
    if ( getBoolean("Set advanced options?", 
                    getYN(false),
                    response) ) {
      boolean boolResult = ((Boolean)response.get(0)).booleanValue();
      if ( boolResult ) {
        getPortTheftInfo();
        getTCPWrapperInfo();
        getBannerInfo();
        getLogInfo();
      }
    }
  }

  protected void getBannerInfo() throws IOException {
    ArrayList response = new ArrayList(1);

    if ( getResponse("Enter a welcome banner", 
                     info.getBanner(), response) ) {
      info.setBanner( ((String)response.get(0)).toString() );
    }
  }

  protected void getPortTheftInfo() throws IOException {
    ArrayList response = new ArrayList(1);
    if ( getBoolean("Prevent passive port theft?", 
                    getYN(info.getPreventPortTheft()),
                    response) ) {
      boolean boolResult = ((Boolean)response.get(0)).booleanValue();
      info.setPreventPortTheft( boolResult ); 
    }
  }

  protected void getTCPWrapperInfo() throws IOException {
    ArrayList response = new ArrayList(1);
    if ( getBoolean("Do you want to filter connections?", 
                    getYN(false), response) ) {
      if ( ((Boolean)response.get(0)).booleanValue() ) {
        if ( getBoolean("Do you want to specify allowable IPs?",
                        getYN(false), response) ) {
          if ( ((Boolean)response.get(0)).booleanValue() ) {
             String def = "";
             if ( info.getAllowFile() != null ) {
               def = info.getAllowFile().getAbsolutePath();
             }
             if ( getResponse(" - Enter the 'allow' file location", def, 
                               response) ) {
               String fileLoc = ((String)response.get(0)).toString();
               info.setAllowFile( new File(fileLoc) );
             }
          }
        }
        else {
           String def = "";
           if ( info.getDenyFile() != null ) {
             def = info.getDenyFile().getAbsolutePath();
           }
           if ( getResponse(" - Enter the 'deny' file location", def, 
                             response) ) {
             String fileLoc = ((String)response.get(0)).toString();
             info.setDenyFile( new File(fileLoc) );
           }
        }
      }
    }
  }

  protected void getLogInfo() throws IOException {
    ArrayList response = new ArrayList(1);

    boolean defQ = info.getLogType().toLowerCase().equals("syslog");

    if ( getBoolean("Use syslog for logging?", 
                    getYN(defQ),
                    response) ) {
      boolean boolResult = ((Boolean)response.get(0)).booleanValue();
      if ( boolResult ) {
        info.setLogType( "syslog" );
        if ( getResponse("Enter the syslog host", info.getLogHost(),
                         response) ) {
          info.setLogHost( ((String)response.get(0)).toString() );
        }
        if ( getResponse("Enter the syslog facility", info.getLogLocation(),
                         response) ) {
          info.setLogLocation( ((String)response.get(0)).toString().toUpperCase() );
        }
      }
    }
    else {
      info.setLogType( "file" );
      if ( getResponse("Enter the log location", info.getLogLocation(),
                       response) ) {
        info.setLogLocation( ((String)response.get(0)).toString() );
      }
    }

    // get log level
  }

  protected boolean getResponse( String question, String def,
                                 ArrayList response ) throws IOException {
    boolean result = false;

    response.clear();

    if ( null != def )
      out.print(question + " [" + def + "]: ");
    else
      out.print(question + ": ");

    String in = stdin.readLine().trim();

    if ( in.length() > 0 )
      result = true;

    if ( in.length() == 0 ) {
      in = def;
    }

    response.add( in );

    return result;
  }

  protected boolean getInteger( String question, int def,
                                ArrayList response ) throws IOException {
    boolean result = getResponse( question, new String(def + ""), response );

    String responseStr = (String)response.get(0);

    Integer intResult = new Integer(def);
    try {
      intResult = new Integer(responseStr);
    }
    catch ( Exception e ) {}

    response.clear();
    response.add( intResult );

    return result;
  }

  protected boolean getBoolean( String question, String def,
                                ArrayList response ) throws IOException {
    boolean result = getResponse( question, def, response );

    String responseStr = (String)response.get(0);

    boolean boolResult = responseStr.equalsIgnoreCase(def);

    if ( responseStr.equalsIgnoreCase("y") ) {
      boolResult = true;
    }
    else {
      boolResult = false;
    }

    response.clear();
    response.add( new Boolean(boolResult) );

    if ( !result && boolResult ) {
      result = boolResult;
    }

    return result;
  }

  private String getYN( boolean doIt ) {
    if ( doIt )
      return "y";
    else
      return "n";
  }
}

