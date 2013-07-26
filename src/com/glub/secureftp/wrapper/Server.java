
//*****************************************************************************
//*
//* (c) Copyright 2007. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: Server.java 149 2009-12-30 00:04:42Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper;

import com.glub.secureftp.wrapper.config.*;

import com.glub.net.*;
import com.glub.util.*;
import com.glub.secureftp.common.*;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.text.*;
import java.util.*;

import javax.net.*;
import javax.net.ssl.*;

import java.security.*;
import java.security.cert.*;
import java.security.spec.*;

public class Server extends Thread {
  private ServerInfo info;

  private ServerLogger logger = null;

  private PrivateKey privateKey = null;
  private X509Certificate[] certList = null;

  private SSLSocketFactory sslSocketFactory = null;
  private SSLServerSocketFactory sslServerSocketFactory = null;

  private SSLServerSocket sslServerSocket = null;
  private ServerSocket plainServerSocket = null;

  private ImplicitServer implicitServer = null;
  private ExplicitServer explicitServer = null;

  private int licensedConnections = 0;

  public Server( ServerInfo info ) throws ServerException {
    if ( info == null ) {
      throw new ServerException( "Server information missing." );
    }

    this.info = info;

    logger = ServerLogger.getLogger( info );
  }

  public void run() {
    boolean failed = false;

    logger.info( "Starting " + SecureFTPWrapper.PROGRAM_NAME + 
                 " v" + SecureFTPWrapper.VERSION );

    logger.debug( "Java Version: " + System.getProperty("java.version"),
                  ServerLogger.DEBUG1_LOG_LEVEL );

    logger.debug( "OS: " + System.getProperty("os.name"),
                  ServerLogger.DEBUG1_LOG_LEVEL );

    int licenseType = -1;

    try {
      initSecurity();
      initSockets();

      if ( null != sslServerSocket ) {
        implicitServer = new ImplicitServer( info, sslSocketFactory,
                                             sslServerSocketFactory,
                                             sslServerSocket,
                                             licensedConnections );
        implicitServer.start();
        logger.info( "Accepting implicit connections (" + info.getWrapperIP() + 
                     ":" + info.getImplicitPort() + ")" );
      }

      if ( null != plainServerSocket ) {
        explicitServer = new ExplicitServer( info, sslSocketFactory,
                                             sslServerSocketFactory,
                                             plainServerSocket,
                                             licensedConnections );
        explicitServer.start();
        logger.info( "Accepting explicit connections (" + info.getWrapperIP() + 
                     ":" + info.getExplicitPort() + ")" );
      }

      Runtime.getRuntime().addShutdownHook( new ServerInterruptThread(this) );
    }
    catch ( KeyStoreException kse ) { failed = true; }
    catch ( CertificateException ce ) { failed = true; }
    catch ( UnrecoverableKeyException uke ) { failed = true; }
    catch ( KeyManagementException kme ) { failed = true; }
    catch ( BindException be ) {
      failed = true;
      logger.severe( "Error binding to " + info.getWrapperIP() + ":" + 
                     info.getImplicitPort() + ": " + be.getMessage() );
    }
    catch ( IOException ioe ) {
      failed = true;
      logger.severe("IOE (Server): " + ioe.getMessage());
    }

    if ( failed ) {
      logger.info( "Shutdown" );
      return;
    }
  } 


  private void initSecurity() throws KeyStoreException, CertificateException,
                                     UnrecoverableKeyException, 
                                     KeyManagementException {
    File publicCertFile = info.getPublicCert();
    File privateKeyFile = info.getPrivateKey();

    String certPrefix = info.getServerID();

    if ( info.getExplicitEnabled() ) {
      certPrefix += "_" + info.getExplicitPort(); 
    }

    if ( info.getImplicitEnabled() ) {
      certPrefix += "_" + info.getImplicitPort(); 
    }

    boolean regenerate = false;

    if ( publicCertFile == null || !publicCertFile.exists() ) {
      regenerate = true;
    }
    else if ( privateKeyFile == null || !privateKeyFile.exists() ) {
      regenerate = true;
    }

    if ( regenerate ) {
      logger.info( "Certificates not found." );
      logger.info( "Now being regenerated... " );

      String ipAddr = info.getWrapperIP();
      if ( info.getFirewallEnabled() ) {
        ipAddr = info.getFirewallIP();
      } 

      if ( ipAddr == null ) {
        ipAddr = NetUtil.getLocalAddress();
      }

      String hostname = NetUtil.getHostNameOrAddress( ipAddr );
      String o = SecureFTPWrapper.PROGRAM_NAME;
      String ou = "Automatically Generated";
      int numOfDays = 1025;

      CertInfo certInfo = new CertInfo( hostname, o, ou, "?", "?", "?" );

      File configDir = new File( System.getProperty("user.dir") );

      publicCertFile = new File( configDir, certPrefix + "_certificate.der" ); 
      privateKeyFile = new File( configDir, certPrefix + "_private.pk8" );

      boolean certGenerated = 
        KeyUtil.writeCertAndKey( certInfo, numOfDays, publicCertFile, 
                                 privateKeyFile );

      if ( certGenerated ) {
        info.setPublicCert( publicCertFile );
        info.setPrivateKey( privateKeyFile );

        try {
          ConfigurationManager.getInstance().writeConfiguration();
        } 
        catch ( IOException ioe ) { 
          logger.warning("Problem writing configuration: " + 
                             ioe.getMessage());
        }
      }
    }

    try {
      logger.debug( "Loading private key...", ServerLogger.DEBUG2_LOG_LEVEL );
      privateKey = KeyUtil.getPrivateKey( KeyUtil.getKeyFactory(), 
                                          privateKeyFile.getAbsolutePath() );
    }
    catch ( FileNotFoundException fnf ) {
      logger.severe( "Private key not found: " + fnf.getMessage() );
      throw new KeyManagementException( fnf.getMessage() );
    }
    catch ( InvalidKeySpecException ikse ) {
      logger.severe( "Error loading private key: " + ikse.getMessage() );
      throw new KeyManagementException( ikse.getMessage() );
    }
    catch ( IOException ioe ) {
      logger.severe( "Error loading private key: " + ioe.getMessage() );
      throw new KeyManagementException( ioe.getMessage() );
    }

    String[] certArr = null;
    if ( null != info.getCACerts() ) {
      logger.debug( "Found " + info.getCACerts().size() + " CA Certs...", ServerLogger.DEBUG2_LOG_LEVEL );
      ArrayList certArrList = new ArrayList( 2 );
      certArrList.add( publicCertFile.getAbsolutePath() );

      for ( int i = 0; i < info.getCACerts().size(); i++ ) {
        File caCertFile = (File)info.getCACerts().get(i);
        certArrList.add( caCertFile.getAbsolutePath() );
      }

      certArr = (String[])certArrList.toArray( new String[1] );
    }
    else {
      logger.debug( "No CA Certs...", ServerLogger.DEBUG2_LOG_LEVEL );
      certArr = new String[] { publicCertFile.getAbsolutePath() };
    }

   try {
      logger.debug( "Loading public certificates...", ServerLogger.DEBUG2_LOG_LEVEL );
      certList = KeyUtil.getCertificateList( KeyUtil.getCertificateFactory(),
                                             certArr );
    }
    catch ( FileNotFoundException fnf ) {
      logger.severe( "Public certificate not found: " + fnf.getMessage() );
      throw new CertificateException( fnf.getMessage() );
    }
    catch ( CertificateException ce ) {
      logger.severe( "Error loading public certificate: " + ce.getMessage() );
      throw ce;
    }

    if ( null == certList ) {
      logger.severe( "Error loading public certificates." );
      return;
    }

    for ( int i = 0; null != certList && i < certList.length; i++ ) {
      logger.debug("cert " + i + ": " + certList[i].getSubjectDN(), ServerLogger.DEBUG1_LOG_LEVEL );
    }

    byte[] passBytes = { -56, -56, -114, -21, -3, -57, -52, 113, -8, -6, 37, 125, -70, -92, 61, -117, 58 };

    char[] pass = new String(passBytes).toCharArray();

    KeyStore store = KeyUtil.getKeyStore( pass,
                                          "GT_" + certPrefix,
                                          privateKey, certList ); 

    KeyManagerFactory kmFactory = KeyUtil.getKeyManagerFactory( store, pass );

    TM[] trustManager = new TM[] {
      new TM( info )
    };
  
    SSLContext context = SSLUtil.getContext( kmFactory.getKeyManagers(),
                                             trustManager,
                                             null );

    sslSocketFactory = SSLUtil.getSocketFactory( context );
    sslServerSocketFactory = SSLUtil.getServerSocketFactory( context );
  }

  private void initSockets() throws BindException, IOException {
    if ( info.getImplicitEnabled() ) {
      sslServerSocket = 
        SSLUtil.createServerSocket( info.getImplicitPort(),
                                    info.getBacklog(),
                                    info.getWrapperIP(),
                                    sslServerSocketFactory );

      if ( null == sslServerSocket ) {
        throw new IOException( "There was a problem creating a secure server socket." );
      }

      //sslServerSocket.setWantClientAuth( true );
      sslServerSocket.setWantClientAuth( false );
    }

    if ( info.getExplicitEnabled() ) {
      plainServerSocket = 
        new ServerSocket( info.getExplicitPort(),
                          info.getBacklog(),
                          InetAddress.getByName(info.getWrapperIP()) );
    }
  }

  public void shutdown() {
    // we might have a socket blocking for accept
    // this should answer that request.
    Socket s = null; 
    
    try {
      if ( null != explicitServer ) {
        s = new Socket(info.getWrapperIP(), info.getExplicitPort());
        s.close(); 
      }
    } 
    catch ( Exception e ) { }
        
    try {    
      if ( null != implicitServer ) { 
        s = new Socket(info.getWrapperIP(), info.getImplicitPort());
        s.close();
      }
    }       catch ( Exception e ) { }

    logger.info( "Shutdown" );
  }
}

class ServerThread extends Thread {
  private ServerInfo serverInfo = null;
  private SSLSocketFactory sslSocketFactory = null;
  private SSLServerSocketFactory sslServerSocketFactory = null;
  private ServerSocket serverSocket = null;
  private boolean explicitConnection = false;
  private int licensedConnections = 0;
  private PortRange portRange = null;
  private ServerLogger logger = null;

  private TCPWrapper connectControl = null;

  private boolean running = true;
  private String termMsg  = "";

  public ServerThread( ServerInfo serverInfo, 
                       SSLSocketFactory sslSocketFactory,
                       SSLServerSocketFactory sslServerSocketFactory,
                       ServerSocket serverSocket, 
                       boolean explicit, int licensedConnections ) {
    this.serverInfo = serverInfo;
    this.serverSocket = serverSocket;
    this.explicitConnection = explicit;
    this.sslSocketFactory = sslSocketFactory;
    this.sslServerSocketFactory = sslServerSocketFactory;
    this.logger = ServerLogger.getLogger( serverInfo );
    this.licensedConnections = licensedConnections;
    this.connectControl = new TCPWrapper( serverInfo, logger );
  }

  public void run() {
    try {
      portRange = new PortRange( serverInfo );
      ThreadGroup tg = new ThreadGroup( serverInfo.getServerID() );

      while( SecureFTPWrapper.runServer() && running ) { 
        // the ControlChannel has two threads (one to act as a socket timeout)
        int connectionCount = (tg.activeCount() / 2) + 1;

        Socket sock = serverSocket.accept();

        boolean allowConnection = connectControl.allow(sock.getInetAddress());
       
        if ( !allowConnection &&
             serverInfo.getLogLevel() >= ServerLogger.DEBUG1_LOG_LEVEL ) {
          logger.warning( "Attempted connection rejected: " + 
                          sock.getInetAddress() );
        }

        if ( serverInfo.getLogLevel() >= ServerLogger.DEBUG2_LOG_LEVEL ) {
          logger.debug( "Current Connections: " + connectionCount, ServerLogger.DEBUG2_LOG_LEVEL );

          Runtime rt = Runtime.getRuntime();
          long usage = (rt.totalMemory() - rt.freeMemory());
          logger.debug( "Memory Usage: " + usage + " bytes", ServerLogger.DEBUG2_LOG_LEVEL );
        }

        if ( !running ) {
          logger.severe( termMsg );
        }
        else if ( !allowConnection ) {
          close( sock );
        }
        else if ( licensedConnections == 0 || 
                  connectionCount <= licensedConnections ) {
          try {
            ControlChannel control = 
              new ControlChannel( tg, serverInfo, sslSocketFactory, 
                                  sslServerSocketFactory, sock,
                                  new Socket( serverInfo.getServerIP(),
                                              serverInfo.getServerPort() ),
                                  false, explicitConnection, portRange );
            control.start();
          }
          catch ( ConnectException ce ) {
            String msg = "Service not available.";
            writeToClient( sock, 421, msg );

            String logMsg = "Destination interface (" + 
                            serverInfo.getServerIP() +
                            ":" + serverInfo.getServerPort() + 
                            ") is not accepting connections.";
            logger.warning( logMsg );
            close( sock );
          }
        }
        else {
          String msg = 
            "The FTP server has reached the maximum number of connections.";
          writeToClient( sock, 530, msg );
          logger.warning( msg );
          close( sock );
        }
      } 
    }
    catch ( IOException ioe ) {
      logger.warning( "IOE (Control): " + ioe.getMessage() );
    }
  }

  private void close( Socket s ) {
    try {
      if (s != null && !s.isClosed())
        s.close();
    } catch ( IOException ioe ) {}
  }

  public void terminate( String msg ) {
    termMsg = msg;
    running = false;

    String logMsg = "The license has expired.";
    logger.severe( logMsg );
  }

  private void writeToClient( Socket sock, int errCode, String msg ) 
                                                         throws IOException {
    PrintWriter outClient = new PrintWriter(sock.getOutputStream(), true);
    outClient.print( errCode + msg + "\r\n" );
    outClient.flush();
  }
}

class ImplicitServer extends ServerThread {
  public ImplicitServer( ServerInfo serverInfo, SSLSocketFactory sslSockFact,
                         SSLServerSocketFactory sslServerSockFact,
                         ServerSocket serverSocket, int licensedConn ) {
    super( serverInfo, sslSockFact, sslServerSockFact,
           serverSocket, false, licensedConn );
  }
}

class ExplicitServer extends ServerThread {
  public ExplicitServer( ServerInfo serverInfo, SSLSocketFactory sslSockFact,
                         SSLServerSocketFactory sslServerSockFact,
                         ServerSocket serverSocket, int licensedConn ) {
    super( serverInfo, sslSockFact, sslServerSockFact,
           serverSocket, true, licensedConn );
  }
}

class TM implements X509TrustManager {
  private ServerInfo info = null;
  private ServerLogger logger = null;

  public TM( ServerInfo info ) {
    this.info = info;
    this.logger = ServerLogger.getLogger( info );
  }
 
  public X509Certificate[] getAcceptedIssuers() {
    // not yet implemented
    return new X509Certificate[0];
  }

  public void checkServerTrusted(X509Certificate[] chain, String authType)
                                 throws CertificateException {
    // not an issue for the wrapper
  }

  public void checkClientTrusted(X509Certificate[] chain, String authType)
                                 throws CertificateException {
    X509Certificate cert = chain[0];
    logger.debug("client cert: " + cert.getSubjectDN(), ServerLogger.DEBUG1_LOG_LEVEL );
  }
}

class ExpirationThread extends Thread {
  private ServerThread server = null;
  private Calendar expiration = null;
  private boolean runThread = true;

  public ExpirationThread( ServerThread st, Calendar expiration ) {
    this.server = st;
    this.expiration = expiration;
  }

  public void run() {
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

    while ( runThread ) {
      if ( expiration.getTimeInMillis() < new Date().getTime() ) {
        String msg = "This license expired on " +
                     dateFormat.format(expiration.getTime()) + "."; 
        server.terminate( msg );
        runThread = false;
      }
      else {
        // sleep 1 hr (in ms)
        try {
          sleep( 3600000 );
        }
        catch ( InterruptedException ie ) {}
      }
    }
  }
}

class ServerInterruptThread extends Thread {
  Server server = null;

  public ServerInterruptThread( Server server ) {
    this.server = server;
  }

  public void run() {
    if ( null != server && server.isAlive() ) {
      server.shutdown();
    }
  }
}


