
//*****************************************************************************
//*
//* (c) Copyright 2005. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: ServerInfo.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper;

import com.glub.net.*;

import java.io.*;
import java.util.*;

public class ServerInfo {
  private String serverID = null;
  private boolean wrapperEnabled = true;
  private String license = null;
  private String serverIP = null;
  private String wrapperIP = null;
  private int serverPort = 21;
  private boolean implicitEnabled = true;
  private int implicitPort = 990;
  private boolean explicitEnabled = true;
  private int explicitPort = 21;
  private boolean firewallEnabled = false;
  private String firewallIP = null;
  private int passivePortStart = 3000;
  private int passivePortEnd = 4000;
  private boolean preventPortTheft = true;
  private boolean encryptControlChannel = true;
  private String encryptDataChannel = null;
  private String email = null;
  private String banner = SecureFTPWrapper.PROGRAM_NAME;
  private File publicCert = null;
  private File privateKey = null;
  private ArrayList caCerts = null;
  private int logLevel = 0;
  private String logLocation = null;
  private String logType = "file";
  private String logHost = "localhost";
  private int backlog = 64;
  private File allowFile = null;
  private File denyFile = null;

  public ServerInfo() {
    setServerID( NetUtil.getHostNameOrAddress(NetUtil.getLocalAddress()) );
    setWrapperEnabled( true );
    setServerIP( NetUtil.getLocalAddress() );
    setServerPort( 21 );
    setWrapperIP( NetUtil.getLocalAddress() );
    setImplicitEnabled( true );
    setImplicitPort( 990 );
    setExplicitEnabled( false );
    setExplicitPort( 21 );
    setFirewallEnabled( false );
    setFirewallIP( null );
    setPassivePortStart( 0 );
    setPassivePortEnd( 0 );
    setPreventPortTheft( true );
    setEncryptControlChannel( true );
    setEncryptDataChannel( "true" );
    setBanner( SecureFTPWrapper.PROGRAM_NAME );
    setPublicCert( null );
    setPrivateKey( null );
    setCACerts( new ArrayList(1) ); 
    setEmail( null );
    setLogLevel( 0 );
    setBacklog( 64 );
  }

  public String getServerID() { return serverID; }
  public void setServerID( String serverID ) {
    this.serverID = serverID;
  }

  public boolean getWrapperEnabled() { return wrapperEnabled; }
  public void setWrapperEnabled( boolean wrapperEnabled ) {
    this.wrapperEnabled = wrapperEnabled;
  }

  public String getServerIP() { return serverIP; }
  public void setServerIP( String serverIP ) {
    this.serverIP = serverIP;
  }

  public int getServerPort() { return serverPort; }
  public void setServerPort( int serverPort ) {
    this.serverPort = serverPort;
  }

  public String getWrapperIP() { return wrapperIP; }
  public void setWrapperIP( String wrapperIP ) {
    this.wrapperIP = wrapperIP;
  }

  public boolean getImplicitEnabled() { return implicitEnabled; }
  public void setImplicitEnabled( boolean implicitEnabled ) {
    this.implicitEnabled = implicitEnabled;
  }

  public int getImplicitPort() { return implicitPort; }
  public void setImplicitPort( int implicitPort ) {
    this.implicitPort = implicitPort;
  }

  public boolean getExplicitEnabled() { return explicitEnabled; }
  public void setExplicitEnabled( boolean explicitEnabled ) {
    this.explicitEnabled = explicitEnabled;
  }

  public int getExplicitPort() { return explicitPort; }
  public void setExplicitPort( int explicitPort ) {
    this.explicitPort = explicitPort;
  }

  public boolean getFirewallEnabled() { return firewallEnabled; }
  public void setFirewallEnabled( boolean firewallEnabled ) {
    this.firewallEnabled = firewallEnabled;
  }

  public String getFirewallIP() { return firewallIP; }
  public void setFirewallIP( String firewallIP ) {
    this.firewallIP = firewallIP;
  }

  public int getPassivePortStart() { return passivePortStart; }
  public void setPassivePortStart( int start ) { passivePortStart = start; }

  public int getPassivePortEnd() { return passivePortEnd; }
  public void setPassivePortEnd( int end ) { passivePortEnd = end; }

  public String getEmail() { return email; }
  public void setEmail( String email ) {
    this.email = email;
  }

  public boolean getPreventPortTheft() { return preventPortTheft; }
  public void setPreventPortTheft( boolean prevent ) {
    this.preventPortTheft = prevent;
  }

  public boolean getEncryptControlChannel() { return encryptControlChannel; }
  public void setEncryptControlChannel( boolean encryptControlChannel ) {
    this.encryptControlChannel = encryptControlChannel;
  }

  public String getEncryptDataChannel() { return encryptDataChannel; }
  public void setEncryptDataChannel( String encryptDataChannel ) {
    this.encryptDataChannel = encryptDataChannel;
  }

  public String getBanner() { return banner; }
  public void setBanner( String banner ) { this.banner = banner; }

  public File getPublicCert() { return publicCert; }
  public void setPublicCert( File cert ) { publicCert = cert; }

  public File getPrivateKey() { return privateKey; }
  public void setPrivateKey( File key ) { privateKey = key; }

  public ArrayList getCACerts() { return caCerts; }
  public void setCACerts( ArrayList certs ) { caCerts = certs; }

  public String getCACertsAsString() {
    String result = "";

    for( int i = 0; i < caCerts.size(); i++ ) {
      File file = (File)caCerts.get( i );
      if ( file != null ) {
        result += (file.getAbsolutePath() + ";");
      }
    }

    return result;
  }
  public void setCACertsFromString( String caCertsStr ) {
    StringTokenizer tok = new StringTokenizer( caCertsStr, ";" );
    caCerts.clear();

    ServerLogger logger = ServerLogger.getLogger( this );

    while( tok.hasMoreTokens() ) {
      File certFile = new File( tok.nextToken() );
      if ( certFile.exists() ) {
        caCerts.add( certFile );
        logger.debug( "CA Cert: " + certFile.getAbsolutePath(), ServerLogger.DEBUG2_LOG_LEVEL );
      }
      else {
        logger.warning( "Cannot find CA Cert: " + certFile.getAbsolutePath() );
      }
    }
  }

  public int getBacklog() { return backlog; }
  public void setBacklog( int bl ) { backlog = bl; }

  public int getLogLevel() { return logLevel; }
  public void setLogLevel( int level ) { logLevel = level; }

  public String getLogType() { return logType; }
  public void setLogType( String type ) { logType = type; }

  public String getLogHost() { return logHost; }
  public void setLogHost( String host ) { logHost = host; }

  public String getLogLocation() { 
    if ( null == logLocation ) {
      if ( getLogType().toLowerCase().equals("syslog") ) {
        logLocation = "LOCAL0";
      }
      else {
        logLocation = System.getProperty( "user.dir" ) + File.separator + 
                      getServerID() + ".%u.%g.log";
      }
    }

    return logLocation;
  }
  public void setLogLocation( String loc ) { logLocation = loc; }

  public File getAllowFile() {
    return allowFile;
  }
  public void setAllowFile( File allowFile ) {
    this.allowFile = allowFile;
  }

  public File getDenyFile() {
    return denyFile;
  }
  public void setDenyFile( File denyFile ) {
    this.denyFile = denyFile;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("*********************************************************\n");
    buf.append("Server ID: " + getServerID());
    buf.append("FTP Server: " + getServerIP() + "\n");
    buf.append(" - Port: " + getServerPort() + "\n");
    buf.append("Wrapper: " + getWrapperIP() + "\n");

    if ( getImplicitEnabled() )
      buf.append(" - Implicit Port: " + getImplicitPort() + "\n");

    if ( getExplicitEnabled() )
      buf.append(" - Explicit Port: " + getExplicitPort() + "\n");

    if ( getFirewallEnabled() ) {
      buf.append(" - Firewall: " + getFirewallIP() );
      if ( getPassivePortStart() != getPassivePortEnd() )
        buf.append(":" + getPassivePortStart() + "-" + getPassivePortEnd());
      buf.append("\n");
    }

    buf.append("Encrypt Control Channel: " + getEncryptControlChannel());
    buf.append(", Encrypt Data Channel: " + getEncryptDataChannel() + "\n");
    buf.append("Prevent Passive Port Theft: " + getPreventPortTheft() + "\n");

    if ( getAllowFile() != null ) {
      buf.append("Allow file: " + getAllowFile().getAbsolutePath() + "\n");
    }

    if ( getDenyFile() != null ) {
      buf.append("Deny file: " + getDenyFile().getAbsolutePath() + "\n");
    }

    if ( getEmail() != null && getEmail().length() > 0 )
      buf.append("Email: " + getEmail() + "\n");

    buf.append("Welcome Banner: " + getBanner() + "\n");

    if ( getPrivateKey() != null && getPublicCert() != null ) {
      buf.append("Certificates:\n");
      buf.append(" - Private Key: " + getPrivateKey() + "\n");
      buf.append(" - Public Cert: " + getPublicCert() + "\n");
      if ( getCACertsAsString() != null && getCACertsAsString().length() > 0 )
        buf.append(" - CA Cert: " + getCACertsAsString() + "\n");
    }

    buf.append("Logging:\n");
    buf.append(" - Type: " + getLogType() + "\n");
    buf.append(" - Location: " + getLogLocation() + "\n");
    buf.append(" - Level: " + getLogLevel() + "\n");
    buf.append("*********************************************************\n");

    return buf.toString();
  }
}



