
//*****************************************************************************
//*
//* (c) Copyright 2005. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: ConfigurationManager.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

import com.glub.secureftp.wrapper.*;

import com.glub.util.*;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;
import org.jaxen.*;
import org.jaxen.jdom.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class ConfigurationManager {
  private static ConfigurationManager instance = null;
  public static final String VERSION = "3.0";
  private static String pathToConfiguration = null;
  private static final String configurationFile = "configuration.xml";
  private Document doc = null;
  private ArrayList serverList = new ArrayList();

  private ConfigurationManager() {
    this( null );    
  }

  private ConfigurationManager( String pathToConfiguration ) {
    this.pathToConfiguration = pathToConfiguration;

    if ( null != pathToConfiguration ) {
      File configurationFolder = new File( pathToConfiguration );

      // check to see if the config file exists, if not create it
      File configFile = new File( configurationFolder, configurationFile );
      if ( configFile.exists() ) {
	try {
          loadConfiguration( configFile );
	}
	catch ( Exception e ) { e.printStackTrace(); }
      }
    }
  }

  public static ConfigurationManager getInstance() {
    String fileSep = File.separator;
    String pathToConfiguration = System.getProperty( "user.dir" ) + fileSep;
    if ( null == instance ) {
      instance = new ConfigurationManager( pathToConfiguration );
    }

    return instance;
  }

  public void loadConfiguration( File configFile ) 
                                   throws ConfigurationException, IOException {
    try {
      //doc = new SAXBuilder().build( pathToConfiguration + configurationFile );
      doc = new SAXBuilder().build( configFile );
      XPath path = new JDOMXPath( "/configuration/server" );
      List results = path.selectNodes( doc );
      Iterator iter = results.iterator();
      serverList.clear();

      while( iter.hasNext() ) {
        Element element = (Element)iter.next();
	ServerInfo info = new ServerInfo();

        List logList = element.getChildren("log");
        Iterator logIter = logList.iterator();
        while( logIter.hasNext() ) {
          Element e1 = (Element)logIter.next();
	  info.setLogType(e1.getChildTextNormalize("type"));
          String location = e1.getChildTextNormalize("location");
          if ( location.indexOf(":") > 0 ) {
            StringTokenizer tok = new StringTokenizer( location, ":" );
            String host = tok.nextToken();
            info.setLogHost(host);
            location = tok.nextToken();
          }
	  info.setLogLocation(location);
	  info.setLogLevel( 
            (new Integer(e1.getChildTextNormalize("level"))).intValue() );
        }

	ServerLogger logger = ServerLogger.getLogger( info );

	logger.debug( "Loading configuration...", ServerLogger.DEBUG2_LOG_LEVEL );	

        List certList = element.getChildren("certs");
        info.setServerID( element.getAttributeValue("id") ); 

	logger.debug( "Server ID: " + info.getServerID(), ServerLogger.DEBUG2_LOG_LEVEL );	

        info.setWrapperIP( element.getAttributeValue("ip") );

	logger.debug( "Wrapper IP: " + info.getWrapperIP(), ServerLogger.DEBUG2_LOG_LEVEL );	

        info.setWrapperEnabled( 
            (new Boolean(element.getAttributeValue("enabled"))).booleanValue());

	logger.debug( "Wrapper Enabled: " + info.getWrapperEnabled(), ServerLogger.DEBUG2_LOG_LEVEL );	

        List ftpServerList = element.getChildren("server-ftp");
        Iterator ftpServerIter = ftpServerList.iterator();
        while( ftpServerIter.hasNext() ) {
          Element e0 = (Element)ftpServerIter.next();
	  info.setServerIP( e0.getChildTextNormalize("ip") );
	  info.setServerPort(
            (new Integer(e0.getChildTextNormalize("port"))).intValue() ); 

	  logger.debug( "FTP Server: " + info.getServerIP() + ":" + info.getServerPort(), ServerLogger.DEBUG2_LOG_LEVEL );	
        }

        List implicitList = element.getChildren("ssl-implicit");
        Iterator implicitIter = implicitList.iterator();
        while( implicitIter.hasNext() ) {
          Element e1 = (Element)implicitIter.next();
	  info.setImplicitEnabled( 
            (new Boolean(e1.getAttributeValue("enabled"))).booleanValue() );

	  logger.debug( "Implicit Enabled: " + info.getImplicitEnabled(), ServerLogger.DEBUG2_LOG_LEVEL );	

	  info.setImplicitPort( 
            (new Integer(e1.getChildTextNormalize("port"))).intValue() );

	  logger.debug( "Implicit Port: " + info.getImplicitPort(), ServerLogger.DEBUG2_LOG_LEVEL );	
        }

        List explicitList = element.getChildren("ssl-explicit");
        Iterator explicitIter = explicitList.iterator();
        while( explicitIter.hasNext() ) {
          Element e2 = (Element)explicitIter.next();

	  info.setExplicitEnabled( 
            (new Boolean(e2.getAttributeValue("enabled"))).booleanValue() );

	  logger.debug( "Explicit Enabled: " + info.getExplicitEnabled(), ServerLogger.DEBUG2_LOG_LEVEL );	

	  info.setExplicitPort( 
            (new Integer(e2.getChildTextNormalize("port"))).intValue() );

	  logger.debug( "Explicit Port: " + info.getExplicitPort(), ServerLogger.DEBUG2_LOG_LEVEL );	
        }

        List firewallList = element.getChildren("firewall-support");
        Iterator firewallIter = firewallList.iterator();
        while( firewallIter.hasNext() ) {
          Element e3 = (Element)firewallIter.next();
	  info.setFirewallEnabled( 
            (new Boolean(e3.getAttributeValue("enabled"))).booleanValue() );

	  logger.debug( "Firewall Enabled: " + info.getFirewallEnabled(), ServerLogger.DEBUG2_LOG_LEVEL );	

	  info.setFirewallIP(e3.getChildTextNormalize("ip"));

	  logger.debug( "Firewall IP: " + info.getFirewallIP(), ServerLogger.DEBUG2_LOG_LEVEL );	

          List passiveList = e3.getChildren("passive-port-range");
          Iterator passiveListIter = passiveList.iterator();
          while( passiveListIter.hasNext() ) {
            Element p0 = (Element)passiveListIter.next();
            info.setPassivePortStart( 
              (new Integer(p0.getChildTextNormalize("start"))).intValue() );
            info.setPassivePortEnd( 
              (new Integer(p0.getChildTextNormalize("end"))).intValue() );

  	    logger.debug( "Passive Port Range: " + info.getPassivePortStart() + "-" + info.getPassivePortEnd(), ServerLogger.DEBUG2_LOG_LEVEL );	
          }
        }

	info.setPreventPortTheft( 
          (new Boolean(element.getChildTextNormalize("prevent-port-theft"))).booleanValue() );

        logger.debug( "Prevent Port Theft: " + info.getPreventPortTheft(), ServerLogger.DEBUG2_LOG_LEVEL );	

	info.setEncryptControlChannel( 
          (new Boolean(element.getChildTextNormalize("encrypt-control"))).booleanValue() );

        logger.debug( "Encrypt Control: " + info.getEncryptControlChannel(), ServerLogger.DEBUG2_LOG_LEVEL );	

        info.setEncryptDataChannel( element.getChildTextNormalize("encrypt-data") ); 

        logger.debug( "Encrypt Data: " + info.getEncryptDataChannel(), ServerLogger.DEBUG2_LOG_LEVEL );	

        String allowFile = element.getChildTextNormalize("allow-file"); 
        if ( allowFile.trim().length() > 0 ) {
          info.setAllowFile( new File(allowFile) );
          logger.debug( "Allow File: " + info.getAllowFile().getAbsolutePath(), ServerLogger.DEBUG2_LOG_LEVEL );	
        }

        String denyFile = element.getChildTextNormalize("deny-file"); 
        if ( denyFile.trim().length() > 0 ) {
          info.setDenyFile( new File(denyFile) );
          logger.debug( "Deny File: " + info.getDenyFile().getAbsolutePath(), ServerLogger.DEBUG2_LOG_LEVEL );	
        }

        info.setEmail( element.getChildTextNormalize("email") ); 

        info.setBanner( element.getChildTextNormalize("banner") );
        logger.debug( "Welcome Banner: " + info.getBanner(), ServerLogger.DEBUG2_LOG_LEVEL );	

        Iterator certIter = certList.iterator();
        while( certIter.hasNext() ) {
          Element e1 = (Element)certIter.next();
          String file = e1.getChildTextNormalize("cert-public");
          if ( file.trim().length() > 0 ) {
            info.setPublicCert( new File(file) );
            logger.debug( "Public Cert: " + info.getPublicCert().getAbsolutePath(), ServerLogger.DEBUG2_LOG_LEVEL );	
          }

          file = e1.getChildTextNormalize("key-private");
          if ( file.trim().length() > 0 ) {
            info.setPrivateKey( new File(file) );
            logger.debug( "Private Key: " + info.getPrivateKey().getAbsolutePath(), ServerLogger.DEBUG2_LOG_LEVEL );	
          }

          file = e1.getChildTextNormalize("cert-ca");
          if ( file.trim().length() > 0 ) {
            info.setCACertsFromString( file.trim() );
          }
        }

	serverList.add( info );
      }
    }
    catch ( JaxenException je ) {
      throw new ConfigurationException( je.getMessage() );
    }
    catch ( JDOMException jdome ) {
      throw new ConfigurationException( jdome.getMessage() );
    }
  }

  public boolean hasConfiguraion() {
    return serverList.size() > 0;
  }

  public void addConfiguration( ServerInfo info ) {
    serverList.add( info ); 
  }

  public ServerInfo getConfiguration( int index ) {
    return (ServerInfo)serverList.get( index );
  }

  public ServerInfo getConfigurationCopy( int index ) {
    ServerInfo newInfo = new ServerInfo();
    ServerInfo oldInfo = getConfiguration( index );

    newInfo.setWrapperEnabled( oldInfo.getWrapperEnabled() );
    newInfo.setServerID( oldInfo.getServerID() );
    newInfo.setServerIP( oldInfo.getServerIP() );
    newInfo.setServerPort( oldInfo.getServerPort() );
    newInfo.setWrapperIP( oldInfo.getWrapperIP() );
    newInfo.setImplicitEnabled( oldInfo.getImplicitEnabled() );
    newInfo.setImplicitPort( oldInfo.getImplicitPort() );
    newInfo.setExplicitEnabled( oldInfo.getExplicitEnabled() );
    newInfo.setExplicitPort( oldInfo.getExplicitPort() );
    newInfo.setFirewallEnabled( oldInfo.getFirewallEnabled() );
    newInfo.setFirewallIP( oldInfo.getFirewallIP() );
    newInfo.setPassivePortStart( oldInfo.getPassivePortStart() );
    newInfo.setPassivePortEnd( oldInfo.getPassivePortEnd() );
    newInfo.setAllowFile( oldInfo.getAllowFile() );
    newInfo.setDenyFile( oldInfo.getDenyFile() );
    newInfo.setEmail( oldInfo.getEmail() );
    newInfo.setLogHost( oldInfo.getLogHost() );
    newInfo.setLogLevel( oldInfo.getLogLevel() );
    newInfo.setLogType( oldInfo.getLogType() );
    newInfo.setLogLocation( oldInfo.getLogLocation() );
    newInfo.setBanner( oldInfo.getBanner() );
    newInfo.setPublicCert( oldInfo.getPublicCert() );
    newInfo.setPrivateKey( oldInfo.getPrivateKey() );
    newInfo.setCACerts( oldInfo.getCACerts() );

    return newInfo;
  }

  public void deleteConfiguration( int index ) {
    serverList.remove( index );
  }

  protected void addConfigurationToDOM( ServerInfo info ) {
    Element root = doc.getRootElement();

    Element config = new Element( "server" );

    config.setAttribute( "id", info.getServerID() );
    config.setAttribute( "ip", info.getWrapperIP() );
    config.setAttribute( "enabled", new Boolean(info.getWrapperEnabled()).toString() );

    addElement( "banner", info.getBanner(), config );

    Element ftpServer = new Element( "server-ftp" );
    addElement( "ip", info.getServerIP(), ftpServer );
    addElement( "port", new Integer(info.getServerPort()).toString(), ftpServer );
    config.addContent( ftpServer );
    
    Element implicit = new Element( "ssl-implicit" );
    implicit.setAttribute( "enabled", new Boolean(info.getImplicitEnabled()).toString() );
    addElement( "port", new Integer(info.getImplicitPort()).toString(), implicit );
    config.addContent( implicit );

    Element explicit = new Element( "ssl-explicit" );
    explicit.setAttribute( "enabled", new Boolean(info.getExplicitEnabled()).toString() );
    addElement( "port", new Integer(info.getExplicitPort()).toString(), explicit );
    config.addContent( explicit );

    Element firewall = new Element( "firewall-support" );
    firewall.setAttribute( "enabled", new Boolean(info.getFirewallEnabled()).toString() );
    addElement( "ip", info.getFirewallIP(), firewall );

    Element passive = new Element( "passive-port-range" );
    addElement( "start", new Integer(info.getPassivePortStart()).toString(), passive );
    addElement( "end", new Integer(info.getPassivePortEnd()).toString(), passive );
    firewall.addContent( passive );

    config.addContent( firewall );

    addElement( "prevent-port-theft", new Boolean(info.getPreventPortTheft()).toString(), config );

    addElement( "encrypt-control", new Boolean(info.getEncryptControlChannel()).toString(), config );
    addElement( "encrypt-data", info.getEncryptDataChannel(), config );

    Element certs = new Element( "certs" );

    File file = info.getPublicCert();
    String fileStr = "";
    if ( file != null ) { fileStr = file.getAbsolutePath(); }
    addElement( "cert-public", fileStr, certs );

    file = info.getPrivateKey();
    fileStr = "";
    if ( file != null ) { fileStr = file.getAbsolutePath(); }
    addElement( "key-private", fileStr, certs );

    fileStr = info.getCACertsAsString();
    addElement( "cert-ca", fileStr, certs );

    config.addContent( certs );

    file = info.getAllowFile();
    fileStr = "";
    if ( file != null ) { fileStr = file.getAbsolutePath(); }
    addElement( "allow-file", fileStr, config );

    file = info.getDenyFile();
    fileStr = "";
    if ( file != null ) { fileStr = file.getAbsolutePath(); }
    addElement( "deny-file", fileStr, config );

    addElement( "email", info.getEmail(), config );

    Element log = new Element( "log" );
    addElement( "type", info.getLogType(), log );

    if ( info.getLogType().toLowerCase().equals("syslog") ) {
      addElement( "location", info.getLogHost() + ":" + info.getLogLocation(), log );
    }
    else {
      addElement( "location", info.getLogLocation(), log );
    }

    addElement( "level", new Integer(info.getLogLevel()).toString(), log );

    config.addContent( log );

    root.addContent( config );
  }

  private void addElement( String name, String content, Element info ) {
    Element e = new Element( name );
    e.addContent( content );
    info.addContent( e );
  }

  public List getConfiguration() {
    return serverList;
  }

  public synchronized void writeConfiguration() throws IOException {
    doc = new Document();
    Element root = new Element( "configuration" );
    doc.setRootElement( root );

    Element version = new Element( "version" );
    version.addContent( VERSION );
    root.addContent( version );

    Iterator iter = serverList.iterator();
    while( iter.hasNext() ) {
      ServerInfo info = (ServerInfo)iter.next();
      addConfigurationToDOM( info );
    }

    File configFile = new File( pathToConfiguration + configurationFile );
    FileOutputStream fos = new FileOutputStream( configFile );

    XMLOutputter output = new XMLOutputter( "  ", true );
    output.output( doc, fos );

    fos.close();
  }
}


