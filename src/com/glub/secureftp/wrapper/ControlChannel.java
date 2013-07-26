
//*****************************************************************************
//*
//* (c) Copyright 2007. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: ControlChannel.java 121 2009-12-04 08:35:41Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper;

import com.glub.secureftp.common.*;
import com.glub.util.*;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.util.regex.*;

import javax.net.*;
import javax.net.ssl.*;

public class ControlChannel extends Thread {
  private static final int CLIENT_FAILURE = 0;
  private static final int CLIENT_SUCCESS = 1;
  private static final int SKIP_SERVER_RESPONSE = 2;

  private int controlSocketTimeout = -1;
  private static final int DEF_CONTROL_SOCKET_TIMEOUT = 5;
  private static final int DATA_SOCKET_TIMEOUT    = 60;

  private static final int FTP_DATA_PORT     = 20;
  private static final int FTP_SSL_DATA_PORT = 989;

  private static final String USER_PATTERN = "^user\\s+([\\\\/\\w.:_-]+)";
  private static final String PASS_PATTERN = "^pass\\s+(.+)";
  private static final String ADDR_PATTERN =
    "(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)";
  private static final String APPE_PATTERN = "^appe\\s+([\\\\/\\w\\s.:_-]+)";
  private static final String STOR_PATTERN = "^stor\\s+([\\\\/\\w\\s.:_-]+)";
  private static final String STOU_PATTERN = "^stou\\s+([\\\\/\\w\\s.:_-]+)";
  private static final String REST_PATTERN = "^rest\\s+(\\d+)";
  private static final String PBSZ_PATTERN = "^pbsz\\s+(\\d+)";
  private static final String PROT_PATTERN = "^prot\\s+(.)";

  private TimeoutThread timeoutThread = null;

  protected ServerLogger logger = null;

  private ServerInfo serverInfo = null;

  private PortRange portRange = null;

  protected DataChannel dataChannel = null;
  private boolean doingDataXfer   = false;

  private boolean timedOut = false;

  protected boolean controlRunning = true;

  private SSLSocketFactory sslSocketFactory = null;
  private SSLServerSocketFactory sslServerSocketFactory = null;

  private Socket wrapperSock = null;
  private Socket serverSock = null;

  private Socket wrapperSockInsecure = null;
  private Socket wrapperSockSecure = null;

  private boolean encryptData = false;
  private boolean explicitConnection = false;

  private boolean dataViaPort = false;
  private boolean dataViaPasv = false;

  private boolean authCmdCalled = false;

  private Pattern cmdUserPattern = null;
  private Pattern cmdPassPattern = null;
  private Pattern cmdPasvPattern = null;
  private Pattern cmdEPsvPattern = null;
  private Pattern cmdSSLPattern = null;
  private Pattern cmdTLSPattern = null;
  private Pattern cmdTLSTypePattern = null;
  private Pattern cmdCCCPattern = null;
  private Pattern cmdModeZPattern = null;
  private Pattern cmdFeatPattern = null;
  private Pattern cmdQuitPattern = null;
  private Pattern cmdPBSZPattern = null;
  private Pattern cmdProtPattern = null;
  private Pattern cmdAborPattern = null;
  private Pattern cmdTypePattern = null;

  private Pattern dataAppePattern = null;
  private Pattern dataRestPattern = null;
  private Pattern dataStorPattern = null;
  private Pattern dataStouPattern = null;

  private Pattern respPasvPattern = null;
  private Pattern respEPsvPattern = null;
  private Pattern respPasvDataPattern = null;
  private Pattern respPortPattern = null;

  private BufferedReader clientInputReader = null;
  private BufferedReader serverInputReader = null;

  private PrintWriter clientOutputWriter = null;
  private PrintWriter serverOutputWriter = null;

  private Socket dataSocket = null;
  private ServerSocket dataServerSocket = null;

  private String userName = null;

  private String dataAddr = null;
  private int    dataPort = -1; 

  private boolean passSent = false;
  private boolean loggedIn = false;
  private boolean quitCalled = false;

  private boolean requestFeat = false;
  private boolean requestPasv = false;
  private boolean requestEPsv = false;
  private boolean useModeZ = false;

  private boolean canAbortXfer = false;

  public ControlChannel( ThreadGroup tg, ServerInfo serverInfo, 
                         SSLSocketFactory sslSockFact, 
                         SSLServerSocketFactory sslServerSockFact,
                         Socket wrapperSock, Socket serverSock, 
                         boolean encryptData, boolean explicitConnection,
                         PortRange portRange ) {
    super( tg, serverInfo.getServerID() );
    this.serverInfo = serverInfo;
    this.sslSocketFactory = sslSockFact;
    this.sslServerSocketFactory = sslServerSockFact;
    this.wrapperSock = wrapperSock;
    this.serverSock = serverSock;
    this.encryptData = encryptData;
    this.explicitConnection = explicitConnection;
    this.portRange = portRange;

    this.logger = ServerLogger.getLogger( serverInfo );

    initRegExp();
  }

  private void initRegExp() {
    cmdUserPattern = Pattern.compile( USER_PATTERN, Pattern.CASE_INSENSITIVE );
    cmdPassPattern = Pattern.compile( PASS_PATTERN, Pattern.CASE_INSENSITIVE );
    cmdPasvPattern = Pattern.compile( "^pasv$", Pattern.CASE_INSENSITIVE );
    cmdEPsvPattern = Pattern.compile( "^epsv$", Pattern.CASE_INSENSITIVE );
    cmdSSLPattern = Pattern.compile( "^auth ssl$", Pattern.CASE_INSENSITIVE );
    cmdTLSPattern = Pattern.compile( "^auth tls$", Pattern.CASE_INSENSITIVE );
    cmdTLSTypePattern = Pattern.compile( "^auth tls-(\\w)$", Pattern.CASE_INSENSITIVE );
    cmdCCCPattern = Pattern.compile( "^ccc$", Pattern.CASE_INSENSITIVE );
    cmdModeZPattern = Pattern.compile( "^mode z$", Pattern.CASE_INSENSITIVE );
    cmdFeatPattern = Pattern.compile( "^feat$", Pattern.CASE_INSENSITIVE );
    cmdQuitPattern = Pattern.compile( "^quit$", Pattern.CASE_INSENSITIVE );
    cmdPBSZPattern = Pattern.compile( PBSZ_PATTERN, Pattern.CASE_INSENSITIVE );
    cmdProtPattern = Pattern.compile( PROT_PATTERN, Pattern.CASE_INSENSITIVE );
    cmdAborPattern = Pattern.compile( "^abor$", Pattern.CASE_INSENSITIVE );
    cmdTypePattern = Pattern.compile( "^type\\s+\\w", Pattern.CASE_INSENSITIVE );

    dataAppePattern = Pattern.compile( APPE_PATTERN, Pattern.CASE_INSENSITIVE );
    dataStorPattern = Pattern.compile( STOR_PATTERN, Pattern.CASE_INSENSITIVE );
    dataStouPattern = Pattern.compile( STOU_PATTERN, Pattern.CASE_INSENSITIVE );
    dataRestPattern = Pattern.compile( REST_PATTERN, Pattern.CASE_INSENSITIVE );

    respPasvPattern = Pattern.compile( ".+?" + ADDR_PATTERN + ".*", Pattern.CASE_INSENSITIVE );
    respPasvDataPattern = Pattern.compile( ".+?" + serverSock.getInetAddress().getHostAddress() + "[,:]\\s*(\\d+).+?", Pattern.CASE_INSENSITIVE );
    respPortPattern = Pattern.compile( "^port\\s*" + ADDR_PATTERN, Pattern.CASE_INSENSITIVE );
  }

  public void run() {
    logger.info( "Connection from " + 
                 wrapperSock.getInetAddress().getHostName() + " (" + 
                 wrapperSock.getInetAddress() + ")." );

    try {
      clientInputReader =
        new BufferedReader(new InputStreamReader(wrapperSock.getInputStream()));
      clientOutputWriter = 
        new PrintWriter( wrapperSock.getOutputStream(), true );

      serverInputReader =
        new BufferedReader(new InputStreamReader(serverSock.getInputStream()));
      serverOutputWriter = 
        new PrintWriter( serverSock.getOutputStream(), true );

      String banner = serverInfo.getBanner();
      if ( SecureFTPWrapper.PROGRAM_NAME.equals(banner) ) {
        banner += " v" + SecureFTPWrapper.VERSION;
      }

      boolean status = writeToClient( "220-" + banner ) &&
                       connectToServer();

      timedOut = false;
      while ( status && !timedOut ) {
        status = speakTheProtocol();
      }
    }
    catch ( IOException ioe ) { 
      logger.warning("IOE (Control/Startup): " + ioe.getMessage() );
    }
    finally {
      logger.debug( "close connection", ServerLogger.DEBUG2_LOG_LEVEL );

      closeAll();
    }
  }

  private boolean connectToServer() {
    boolean status = true;

    String line = null;

    try {
        status = parseServerResponse();
    }
    catch ( IOException ioe ) {
      status = false;
    }

    return status;
  }

  private int getControlSocketTimeout() {
    if ( controlSocketTimeout < 0 ) {
      int timeout = GTOverride.getInt( "control_socket.timeout", 5 );
      if ( timeout < 0 ) { timeout = 5; }
      logger.debug( "control timeout set to " + timeout + " minutes", ServerLogger.DEBUG3_LOG_LEVEL );
      timeout *= 60; // put into seconds
      controlSocketTimeout = timeout;
    }

    return controlSocketTimeout;
  }

  public void unsetControlTimeout() {
    if ( timeoutThread != null ) {
      timeoutThread.clear();
      //timeoutThread.stop();
      timeoutThread.interrupt();
    }
    timeoutThread = null;
  }

  public void setControlTimeout() {
    unsetControlTimeout();

    timeoutThread = new TimeoutThread( this, getControlSocketTimeout() );

    timeoutThread.start();
    logger.debug( "control timeout set", ServerLogger.DEBUG4_LOG_LEVEL );
  }

  private boolean speakTheProtocol() {
    boolean status = true;

    dataViaPasv = false;
    dataViaPort = false;

    String line = null;

    if ( !controlRunning ) {
      return false;
    }

    try {
      if ( null == dataChannel || 
           (dataChannel != null && !dataChannel.isTransferring()) ) {
        setControlTimeout();
      }

      int result = parseClientRequest();
      status = ( result == CLIENT_FAILURE ) ? false : true;
      boolean skipServer = (result == SKIP_SERVER_RESPONSE);

      unsetControlTimeout();

      logger.debug( "control timeout released", ServerLogger.DEBUG4_LOG_LEVEL );

      if ( status && !skipServer ) {
        status = parseServerResponse();
      }

      status &= !quitCalled;
    }
    catch ( DataChannelException dce ) {
        close( dataServerSocket );
        close( dataSocket );
        logger.warning( dce.getMessage() );
        writeToClient( "425 " + dce.getMessage() );
    }
    catch ( IOException ioe ) {
      logger.debug( "connection dropped by client", ServerLogger.DEBUG2_LOG_LEVEL );

      if ( dataServerSocket != null ) {
        close( dataServerSocket );
      }

      if ( dataSocket != null ) {
        close ( dataSocket );
      }

      status = false;
    }

    return status;
  }

  private int parseClientRequest() throws IOException {
    boolean status = true;
    boolean sendToServer = true;
    boolean dataXfer = false;

    logger.debug("waiting for client", ServerLogger.DEBUG4_LOG_LEVEL);

    String line = clientInputReader.readLine();

    if ( serverInfo.getLogLevel() >= ServerLogger.DEBUG4_LOG_LEVEL ) {
      if ( matches(line, cmdPassPattern) ) {
        logger.debug("client sent PASS ********", ServerLogger.DEBUG4_LOG_LEVEL );
      }
      else {
        logger.debug("client sent " + line, ServerLogger.DEBUG4_LOG_LEVEL );
      }
    }

    line = overrideClientInput( line );

    if ( null == line ) {
      status = false;
    } 

    /* REST */
    else if ( matches(line, dataRestPattern) ) {
    }

    /* PASV */
    else if ( matches(line, cmdPasvPattern) ) {
      requestPasv = true;
    }

    /* EPSV */
    else if ( matches(line, cmdEPsvPattern) ) {
      requestEPsv = true;
      line = "PASV";
    }

    /* PORT */
    else if ( matches(line, respPortPattern) ) {
      line = handlePortRequest( line );
    }

    /* TYPE */
    else if ( matches(line, cmdTypePattern) ) {
      // let it go through. fix for curl on upload. it sends it in between
      // pasv and data upload
      logger.debug( "type command received", ServerLogger.DEBUG4_LOG_LEVEL );
    }

    /* STOR/STOU/APPE */
    else if ( doingDataXfer ) {
      canAbortXfer = true;
      doingDataXfer = false;
      String xferLine = line.toLowerCase();

      if ( xferLine.startsWith("appe ") ||
           xferLine.startsWith("stor ") ||
           xferLine.startsWith("stou ") ) {
        logger.debug( "send data from client", ServerLogger.DEBUG4_LOG_LEVEL );

        dataChannel.setDirection( DataChannel.DATA_FROM_CLIENT );
      }
      else {
        logger.debug( "send data from server", ServerLogger.DEBUG4_LOG_LEVEL );

        dataChannel.setDirection( DataChannel.DATA_FROM_SERVER );
      }

      if ( dataServerSocket == null || dataSocket == null ) {
        String msg = "Could not open data channel";
        throw new DataChannelException( msg );
      }
      else {
        dataChannel.start();

        sendToServer = false;
        dataXfer = true;
      }
    }

    /* ABOR */
    else if ( matches(line, cmdAborPattern) ) {
      if ( canAbortXfer ) {
        canAbortXfer = false;
        sendToServer = false;
        dataXfer = true;
        dataChannel.abort();
        abort();
      }
    }

    /* QUIT */
    else if ( matches(line, cmdQuitPattern) ) {
      quitCalled = true;
    }

    /* USER */
    else if ( matches(line, cmdUserPattern) ) {
      userName = group( line, cmdUserPattern, 1 );

      if ( explicitConnection && !authCmdCalled && 
           serverInfo.getEncryptControlChannel() ) {
        logger.warning( userName + 
                        " tried to log in before calling auth command." );
        sendToServer = false;
        status = writeToClient( "503 Login with AUTH SSL/TLS first." );
      }
      else {
        logger.debug("USER = " + userName, ServerLogger.DEBUG4_LOG_LEVEL);
      }
    }

    /* PASS */
    else if ( matches(line, cmdPassPattern) ) {
      logger.debug("PASS = ********", ServerLogger.DEBUG4_LOG_LEVEL);

      passSent = true;
    }

    /* FEAT */
    else if ( matches(line, cmdFeatPattern) ) {
      requestFeat = true;
    }

    /* MODE Z */
    else if ( matches(line, cmdModeZPattern) ) {
      useModeZ = true;
      sendToServer = false;
      status = writeToClient( "200 Mode set to Z." );
    }

    /* PBSZ */
    else if ( matches(line, cmdPBSZPattern) ) {
      sendToServer = false;
      status = writeToClient( "200 PBSZ Command OK. " + 
                              "Protection buffer size set to " + 
                              group(line, cmdPBSZPattern, 1) + "." );
    }

    /* PROT */
    else if ( matches(line, cmdProtPattern) ) {
      sendToServer = false;
      status = handleProtRequest( line );
    }

    /* CCC */
    else if ( matches(line, cmdCCCPattern) ) {
      sendToServer = false;
      status = handleCCCRequest( line );
    }

    /* AUTH SSL/TLS */
    else if ( explicitConnection &&
              (matches(line, cmdSSLPattern) ||
               matches(line, cmdTLSPattern) ||
               matches(line, cmdTLSTypePattern)) ) {
      sendToServer = false;
      status = handleAuthTLS( line );
    }

    if ( status && (sendToServer || dataXfer)  ) {
      serverOutputWriter.print( line + "\r\n" );
      if ( serverOutputWriter.checkError() ) {
        if ( serverInfo.getLogLevel() >= ServerLogger.DEBUG4_LOG_LEVEL ) {
          logger.warning( "Error writing to server." );
        }

        status = false;
      }
    }

    int result = ( status ) ? CLIENT_SUCCESS : CLIENT_FAILURE;
    if ( !sendToServer ) {
      result = SKIP_SERVER_RESPONSE;
    }

    return result;
  }

  private String handlePortRequest( String line ) {
    Matcher portMatch = respPortPattern.matcher( line );
    portMatch.matches();
    dataAddr = portMatch.group( 1 ) + '.' +
               portMatch.group( 2 ) + '.' +
               portMatch.group( 3 ) + '.' +
               portMatch.group( 4 );
    String hiPort = portMatch.group( 5 );
    String loPort = portMatch.group( 6 );
    dataPort = (Util.parseInt(hiPort, 0) << 8) |
               (Util.parseInt(loPort, 0));
    logger.debug( "received PORT response from client: port " + dataPort, ServerLogger.DEBUG4_LOG_LEVEL );

    try {
      if ( encryptData ) {
        // try to set the localPort to 20 if were make an explicit
        // connection, 989 for implicit
        // this will (hopefully) work for clients using NAT although there
        // seems to be problems if we have a secure command channel
        // does NAT actually look at the commands going through it?
        // this change is in the hope that i'm wrong or NAT will fix it

        Socket plainSock = null;

        int originatingPort = ( explicitConnection ) ? FTP_DATA_PORT :
                                                       FTP_SSL_DATA_PORT;

        try {
          plainSock = 
            new Socket( dataAddr, dataPort,
                        InetAddress.getByName(serverInfo.getWrapperIP()),
                        originatingPort );
        }
        catch ( BindException be ) {
          plainSock = new Socket( dataAddr, dataPort );
        }

        SSLSocket sslSock = SSLUtil.createSocket( plainSock, dataAddr, dataPort,
                                                  sslSocketFactory );
        sslSock.setUseClientMode( false );

        dataSocket = sslSock;
      }
      else {
        // try to set the localPort to 20
        // this will work for clients using NAT although there
        // seems to be problems if we have a secure command channel
        // does NAT actually look at the commands going through it?

        try {
          dataSocket = 
            new Socket( dataAddr, dataPort,
                        InetAddress.getByName(serverInfo.getWrapperIP()),
                        FTP_DATA_PORT );
        }
        catch ( BindException be ) {
          dataSocket = new Socket( dataAddr, dataPort );
        }
      }

      if ( dataServerSocket != null ) {
        logger.debug( "data server socket already open. closing old connection.", ServerLogger.DEBUG4_LOG_LEVEL );
        unsetControlTimeout();
        close( dataServerSocket );
      }

      dataServerSocket = 
        new ServerSocket( 0, 4,
                          InetAddress.getByName(serverInfo.getWrapperIP()) );

      String newAddress = dataServerSocket.getInetAddress().getHostAddress();
      int newPort = dataServerSocket.getLocalPort();

      String portInfo = newAddress.replace( '.', ',' ) + ',' + 
                        (newPort >> 8) + ',' + (newPort & 0xff);

      line = "PORT " + portMatch.replaceAll( portInfo );

      dataViaPort = true;
    }
    catch ( IOException ioe ) {
      if ( serverInfo.getLogLevel() >= ServerLogger.DEBUG4_LOG_LEVEL ) {
        logger.warning( "IOE (Control/PORT): " + ioe.getMessage() );
      }
    }

    return line;
  }

  private boolean handleProtRequest( String line ) {
    boolean status = true;

    String secLevel = group( line, cmdProtPattern, 1 ).toLowerCase();
    String secMessage = "";

    encryptData = ! "c".equals( secLevel );

    logger.debug( "encrypting data ?= " + encryptData, ServerLogger.DEBUG4_LOG_LEVEL );

    if ( encryptData && "false".equals(serverInfo.getEncryptDataChannel()) ) {
      status = 
        writeToClient( "504 PROT Command Failed. Data encryption disabled." );
      encryptData = false;
    }
    else if ( !encryptData && 
              "always".equals(serverInfo.getEncryptDataChannel()) ) {
      status = 
        writeToClient( "504 PROT Command Failed. Data encryption required." );
      encryptData = true;
    }
    else {
      if ( "p".equals(secLevel) ) {
        secMessage = "Using Private data connection.";
      }
      else if ( "c".equals(secLevel) ) {
        secMessage = "Using Clear data connection.";
      }

      status = writeToClient( "200 PROT Command OK. " + secMessage );
    }

    return status;
  }

  private boolean handleCCCRequest( String line ) {
    boolean status = true;

    wrapperSockSecure = wrapperSock;

    String err = 
      "533 CCC Command Failed. Control connection not protected by TLS.";

    if ( null != wrapperSockInsecure ) {
      try {
        status = 
          writeToClient("200 CCC Command OK. Control connection sending in clear.");
        wrapperSock = wrapperSockInsecure;
        clientOutputWriter = new PrintWriter(wrapperSock.getOutputStream(), true);
        clientInputReader = 
          new BufferedReader(new InputStreamReader(wrapperSock.getInputStream()));
        wrapperSockInsecure = null;
      }
      catch ( IOException ioe ) {
        logger.debug( "IOE (Control/CCC): " + ioe.getMessage(), ServerLogger.DEBUG4_LOG_LEVEL );

        status = writeToClient( err );
      }
    }
    else {
      status = writeToClient( err );
    }

    return status;
  }

  private String overrideClientInput( String line ) {
    String result = line;

    if ( null != line ) {
      // handle SMC router idiocy
      if ( "p@sw".equalsIgnoreCase(line) ) {
        result = "pasv";
      }
    }

    return result;
  }

  private boolean parseServerResponse() throws IOException {
    boolean status = true;

    boolean keepGoing = false;
    boolean firstLine = true;
    int     origReply  = -1;
    int     finalReply = -1;

    String line = serverInputReader.readLine();

    do {
      logger.debug("server sent " + line, ServerLogger.DEBUG4_LOG_LEVEL );

      if ( null == line ) {
        if ( serverInfo.getLogLevel() >= ServerLogger.DEBUG4_LOG_LEVEL ) {
          logger.warning( "server sent is null" );
        }

        keepGoing = false;
        status = false;
      }
      else {
        int len = line.length();
        String replyCodeStr = ( len >= 3 ) ? line.substring( 0, 3 ) : null;
        int    replyCode = Util.parseInt( replyCodeStr, -1 );

        char   separator = ( len >= 4 ) ? line.charAt( 3 ) : '?';
        String extraInfo = ( len >= 5 ) ? line.substring( 4 ) : "";

        if ( firstLine ) {
          origReply = replyCode;
        }
        
        firstLine = false;

        finalReply = replyCode;

        /* PASV RESPONSE */
        if ( (requestPasv || requestEPsv)  && matches(line, respPasvPattern) ) {
          requestPasv = false;
          line = handlePasvResponse( line, requestEPsv );
        }

        /* PASV RESPONSE FROM SERVER (rewrite for firewall) */
        else if ( matches(line, respPasvDataPattern) ) {
          Matcher pasvDataMatch = respPasvDataPattern.matcher( line );
          pasvDataMatch.matches();
          line = "227 Entering Passive Mode (" +
            pasvDataMatch.replaceAll(wrapperSock.getInetAddress().getHostAddress()) + ")";
        }

        /* FEAT RESPONSE */
        else if ( requestFeat ) {
          requestFeat = false;
          line = handleFeatResponse( line, replyCode );
        }

        /* LOG LOGIN */
        else if ( passSent ) {
          passSent = false;

          if ( 230 == replyCode ) {
            String encType = "Secure Login ";
            if ( explicitConnection ) {
              encType += "[explicit SSL] ";
            }
            else if ( wrapperSock.getLocalPort() == serverInfo.getImplicitPort() ) {
              encType += "[implicit SSL] ";
            }
            else {
              encType = "Insecure Login ";
            }

            loggedIn = true;

            logger.info( encType + "by " + userName + " from " + 
                         wrapperSock.getInetAddress().getHostName() +
                         " (" + 
                         wrapperSock.getInetAddress().getHostAddress() +
                         ")." );
          }
          else {
            logger.warning( "FAILED login for " + userName + " from " +
                            wrapperSock.getInetAddress().getHostName() +
                            " (" + 
                            wrapperSock.getInetAddress().getHostAddress() +
                            ")." );
          }
        }

        if ( (dataViaPort || dataViaPasv) && null != dataServerSocket &&
             2 == origReply/100 ) {
          String clientAddr = wrapperSock.getInetAddress().getHostAddress();

          dataServerSocket.setSoTimeout( DATA_SOCKET_TIMEOUT * 1000 );

          dataChannel = 
            new DataChannel( this, serverInputReader, serverOutputWriter,
                             clientOutputWriter, 
                             dataServerSocket, dataSocket, dataViaPasv,
                             clientOutputWriter, clientAddr, serverInfo,
                             portRange, useModeZ ); 
          doingDataXfer = true;
        }

        status = writeToClient( line );

        keepGoing = ( separator == '-' || origReply != finalReply ||
                      separator != ' ' ) && status;

        // 1xy codes require another server response
        // TODO: deal with app waiting for server response
        //       yet i need to listen to abor signal during xfer
        if ( !keepGoing && finalReply / 100 == 1 ) {
          firstLine = true;
          keepGoing = true;
          origReply  = -1;
          finalReply = -1;
        }

        if ( keepGoing ) {
          logger.debug( "waiting for line from server", ServerLogger.DEBUG4_LOG_LEVEL );

          line = serverInputReader.readLine();

          logger.debug( "line from server: " + line, ServerLogger.DEBUG4_LOG_LEVEL );
        }
      }
    } while ( keepGoing );

    return status;
  }

  private String handlePasvResponse( String line, boolean extendedPasv ) {
    Matcher pasvMatch = respPasvPattern.matcher( line );
    pasvMatch.matches();
    dataAddr = pasvMatch.group( 1 ) + '.' +
               pasvMatch.group( 2 ) + '.' +
               pasvMatch.group( 3 ) + '.' +
               pasvMatch.group( 4 );
    String hiPort = pasvMatch.group( 5 );
    String loPort = pasvMatch.group( 6 );
    dataPort = (Util.parseInt(hiPort, 0) << 8) |
               (Util.parseInt(loPort, 0));
    logger.debug( "received PASV response from server: port " + dataPort, ServerLogger.DEBUG4_LOG_LEVEL );

    boolean createdServerSocket = false;

    if ( dataServerSocket != null ) {
      logger.debug( "data server socket already open. closing old connection.", ServerLogger.DEBUG4_LOG_LEVEL );
      unsetControlTimeout();
      close( dataServerSocket );
    }

    if ( encryptData ) {
      try {
        dataServerSocket = 
          portRange.getServerSocket( serverInfo.getWrapperIP(), 4, 
                                     sslServerSocketFactory );
        createdServerSocket = true;
        ((SSLServerSocket)dataServerSocket).setUseClientMode( false );

        if ( serverInfo.getLogLevel() >= ServerLogger.DEBUG5_LOG_LEVEL ) {
          String[] supportedCiphers = ((SSLServerSocket)dataServerSocket).getEnabledCipherSuites();
          logger.debug( "(Data) Enabled ciphers:", ServerLogger.DEBUG5_LOG_LEVEL );
          for( int i = 0; i < supportedCiphers.length; i++ ) {
            logger.debug( "  " + supportedCiphers[i], ServerLogger.DEBUG5_LOG_LEVEL );
          }
        }
      }
      catch ( IOException ioe ) {
        logger.debug( "IOE (Control/PASV): " + ioe.getMessage(), ServerLogger.DEBUG4_LOG_LEVEL );
      }
    }
    else {
      try {
        dataServerSocket = 
          portRange.getServerSocket( serverInfo.getWrapperIP(), 4, 
                                     ServerSocketFactory.getDefault() );
        createdServerSocket = true;
      }
      catch ( IOException ioe ) {
        logger.debug( "IOE (Control/PASV 2): " + ioe.getMessage(), ServerLogger.DEBUG4_LOG_LEVEL );
      }
    }

    if ( createdServerSocket ) {
      try {
        logger.debug("data socket " + dataAddr + ":" + dataPort, ServerLogger.DEBUG4_LOG_LEVEL);

        dataSocket = new Socket( dataAddr, dataPort );

        String newIP = serverInfo.getWrapperIP();
        if ( serverInfo.getFirewallEnabled() ) {
          newIP = serverInfo.getFirewallIP();
        }

        InetAddress newInetAddress = InetAddress.getByName( newIP );

        if ( null == newInetAddress ) {
          newInetAddress = dataServerSocket.getInetAddress();
        }

        String newAddress = newInetAddress.getHostAddress();

        if ( null == newAddress ) {
          newAddress = dataServerSocket.getInetAddress().getHostAddress();
        }

        int newPort = dataServerSocket.getLocalPort();

        logger.debug("new addr " + newAddress + ":" + newPort, ServerLogger.DEBUG4_LOG_LEVEL);

        if ( extendedPasv ) { 
          line = "229 Entering Extended Passive Mode (|||" + newPort + "|)";

          logger.debug( "send EPSV response to client: port " + newPort, ServerLogger.DEBUG4_LOG_LEVEL );
        }
        else {
          String pasvInfo = newAddress.replace( '.', ',' ) + ',' + 
                            (newPort >> 8) + ',' + (newPort & 0xff);
  
          line = "227 Entering Passive Mode (" + 
                 pasvMatch.replaceAll(pasvInfo) + ")";

          logger.debug( "send PASV response to client: port " + newPort, ServerLogger.DEBUG4_LOG_LEVEL );
        }
      }
      catch ( IOException ioe ) {
        logger.debug( "IOE (Control/PASV 3): " + ioe.getMessage(), ServerLogger.DEBUG4_LOG_LEVEL );
      }
    }
    else {
      line = "425 The server is too busy. No free ports available.";
    }

    dataViaPasv = true;

    return line;
  }

  private boolean handleAuthTLS( String line ) {
    boolean status = true;

    Matcher tlsMatch = cmdTLSTypePattern.matcher( line );
    String tlsType = null;
    if ( tlsMatch.matches() ) {
      tlsType = tlsMatch.group( 1 );
    }

    try {
      wrapperSockInsecure = wrapperSock;
      SSLSocket sslSock =
        SSLUtil.createSocket( wrapperSock,
                              wrapperSock.getInetAddress().getHostAddress(),
                              wrapperSock.getPort(), sslSocketFactory,
                              true );
      sslSock.setUseClientMode( false );
      wrapperSock = (Socket)sslSock;

     // this is not exactly correct... but i'm doing this for ws_ftp
     // this flag should be set using the PROT command
     if ( null != tlsType && "c".equalsIgnoreCase(tlsType) ) {
       encryptData = false;
     } 
     else {
       encryptData = true;
     }

     status = 
       writeToClient( "234 AUTH Command OK. Initializing SSL connection." );

     authCmdCalled = true;

     clientInputReader = 
       new BufferedReader( new InputStreamReader(sslSock.getInputStream()) );

     clientOutputWriter = new PrintWriter( sslSock.getOutputStream(), true );
    }
    catch ( IOException ioe ) {
      status = false;
      logger.debug( "IOE: (Control/AUTH) " + ioe.getMessage(), ServerLogger.DEBUG4_LOG_LEVEL );
    }

   return status;
  }

  private String handleFeatResponse( String line, int replyCode ) {
    StringBuffer features = new StringBuffer();

    if ( explicitConnection ) {
      features.append(" AUTH SSL;TLS;\r\n");
      features.append(" CCC\r\n");
    }

    features.append( " PBSZ\r\n" );
    features.append( " PROT\r\n" );
    features.append( " MODE Z" );

    // we must be logged in for this info
/*
    if ( !loggedIn ) {
      line = "530 Please login with USER and PASS";
    }
*/

    // if the server doesn't support FEAT, it does now!
    if ( 500 == replyCode ) {
      StringBuffer result = new StringBuffer();
      result.append("211-Extensions supported\r\n");
      result.append(features.toString());
      result.append("\r\n");
      result.append("211 END");
      line = result.toString();
    }

    // if the server supports FEAT, prepend our extensions
    else if ( 211 == replyCode ) {
      line += "\r\n" + features.toString();
    }

    return line;
  }

  private boolean matches( String line, Pattern pattern ) {
    if ( null == line ) {
      return false;
    }

    Matcher m = pattern.matcher( line );
    return m.matches();
  }

  private String group( String line, Pattern pattern, int group ) {
    if ( null == line ) {
      return "";
    }

    Matcher m = pattern.matcher( line );
    m.matches();
    return m.group( group ); 
  }

  private boolean writeToClient( String msg ) {
    clientOutputWriter.print( msg + "\r\n" );
    clientOutputWriter.flush();
    return !clientOutputWriter.checkError();
  }

  public void timeout() {
    timedOut = true;
    closeAll();
  }

  public void closeAll() {
    controlRunning = false;
    logger.debug( "close all sockets", ServerLogger.DEBUG4_LOG_LEVEL );
    close( dataServerSocket );
    close( dataSocket );
    close( serverSock );
    close( wrapperSock );
    close( serverInputReader );
    close( serverOutputWriter );
    close( clientInputReader );
    close( clientOutputWriter );
  }

  private void close( BufferedReader reader ) {
    try {
      if (reader != null) {
        reader.close();
      }
      reader = null;
    } catch ( IOException ioe ) {}
  }

  private void close( InputStream is ) {
    try {
      if (is != null)
        is.close();
      is = null;
    } catch ( IOException ioe ) {}
  }

  private void close( OutputStream os ) {
    try {
      if (os != null)
        os.close();
      os = null;
    } catch ( IOException ioe ) {}
  }

  private void close( PrintWriter writer ) {
    if (writer != null)
      writer.close();
    writer = null;
  }

  private void close( Socket s ) {
    try {
      if (s != null && !s.isClosed())
        s.close();
      s = null;
    } catch ( IOException ioe ) {}
  }

  private void close( ServerSocket ss ) {
    try {
      if (ss != null) {
        portRange.freePort( ss.getLocalPort() );  
        ss.close();
      }
      ss = null;
    }
    catch (Exception e) { }
  }

  private void abort() throws IOException {
    byte interpretAs = (byte)255;
    byte interruptedProcess = (byte)244;

    try {
      Method urgentDataMethod = null;
      Class[] args = { int.class };
      urgentDataMethod = serverSock.getClass().getMethod("sendUrgentData", args);
      if ( null != urgentDataMethod ) {
        serverSock.sendUrgentData( interpretAs );
        serverSock.sendUrgentData( interruptedProcess );
        logger.debug( "aborting...", ServerLogger.DEBUG4_LOG_LEVEL );
      }
    }
    catch ( SocketException se ) {
      // in case the sendUrgentData doesn't exist
    }
    catch ( NoSuchMethodException nsme ) {}
  }
}

class TimeoutThread extends Thread {
  private ControlChannel control = null;
  private long elapsed = 0L;
  private long timeout = 0L;
  private boolean running = true;

  private static final int ONE_SECOND = 1000;

  public TimeoutThread( ControlChannel control, long timeout ) {
    this.control = control;
    this.timeout = timeout;
  }

  public void clear() {
    running = false;
  }

  public void run() {
    while ( running ) {
      try {
        Thread.sleep( ONE_SECOND );
      }
      catch ( InterruptedException ioe ) {
        running = false;
        control.logger.debug( "control timeout removed", ServerLogger.DEBUG4_LOG_LEVEL );
        continue;
      }

      synchronized ( this ) {
        if ( control.dataChannel != null && control.dataChannel.isTransferring() ) {
          control.logger.debug( "control timeout set, but data is transferring", ServerLogger.DEBUG4_LOG_LEVEL );
          running = false;
          return;
        }
        else if ( !control.controlRunning ) {
          control.logger.debug( "control timeout set, but control is gone", ServerLogger.DEBUG4_LOG_LEVEL );
          running = false;
          return;
        }
        else {
          //control.logger.debug( "control timeout set, and data is not transferring", ServerLogger.DEBUG4_LOG_LEVEL );
        }

        elapsed += ONE_SECOND;

        if ( isInterrupted() ) {
          running = false;
          control.logger.debug( "control timeout removed", ServerLogger.DEBUG4_LOG_LEVEL );
        }
        else if ( elapsed > timeout * ONE_SECOND ) {
          control.logger.debug( "control timeout reached", ServerLogger.DEBUG3_LOG_LEVEL );
          control.timeout();
          running = false;
        }
      }
    }
  }

  public void reset() { elapsed = 0L; }
}


