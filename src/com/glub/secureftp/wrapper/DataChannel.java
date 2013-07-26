
//*****************************************************************************
//*
//* (c) Copyright 2007. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: DataChannel.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper;

import com.glub.util.*;

import java.io.*;
import java.net.*;
import java.util.zip.*;

public class DataChannel extends Thread {
  public static final int DATA_UNSET       = -1;
  public static final int DATA_FROM_CLIENT = 0;
  public static final int DATA_FROM_SERVER = 1;

  private static final int CONTROL_SOCKET_TIMEOUT = 120;
  private static final int DATA_SOCKET_TIMEOUT    = 300;

  private ControlChannel controlChannel = null;

  private BufferedReader serverInputReader = null;
  private PrintWriter clientOutputWriter = null;
  private PrintWriter serverOutputWriter = null;
  private ServerSocket serverSocket = null;
  private Socket socket1 = null;
  private Socket socket2 = null;
  private boolean pasv = true;
  private PrintWriter clientOutput = null;
  private String clientAddr = null;
  private ServerInfo serverInfo = null;
  private PortRange portRange = null;
  private boolean modeZ = false;
  private ServerLogger logger = null;

  private int direction = DATA_UNSET;

  private boolean abort = false;

  private boolean isTransferring = false;

  public DataChannel( ControlChannel controlChannel,
                      BufferedReader serverInput, PrintWriter serverOutput,
                      PrintWriter clientOutput, 
                      ServerSocket dataServerSocket, Socket dataSocket,
                      boolean pasv, PrintWriter clientOutputWriter, 
                      String clientAddr, ServerInfo serverInfo, 
                      PortRange portRange, boolean modeZ ) {
    this.controlChannel = controlChannel;
    this.serverInputReader = serverInput;
    this.serverOutputWriter = serverOutput;
    this.clientOutputWriter = clientOutput;
    this.serverSocket = dataServerSocket;
    this.socket1 = dataSocket;
    this.pasv = pasv;
    this.clientOutput = clientOutputWriter;
    this.clientAddr = clientAddr;
    this.serverInfo = serverInfo;
    this.portRange = portRange;
    this.modeZ = modeZ;
    this.logger = ServerLogger.getLogger( serverInfo );
  }

  private int getDirection() {
    int result = direction;

     if ( !pasv && DATA_FROM_SERVER == direction ) {
       direction = DATA_FROM_CLIENT;
     }
     else if ( !pasv && DATA_FROM_CLIENT == direction ) {
       direction = DATA_FROM_SERVER;
     }

    return direction;
  }

  public void setDirection( int direction ) {
    isTransferring = true;
    this.direction = direction;
  }
                      
  public void run() {
    String line = null;

    boolean keepGoing = false;
    boolean status = true;

    try {
      line = serverInputReader.readLine();

      do {
        logger.debug( "Data (from server) 1: " + line, ServerLogger.DEBUG4_LOG_LEVEL );

        if ( null == line ) {
          keepGoing = false;
        }
        else {
          int len = line.length();
          String replyCodeStr = ( len >= 3 ) ? line.substring( 0, 3 ) : null;
          int    replyCode = Util.parseInt( replyCodeStr, -1 );

          char   separator = ( len >= 4 ) ? line.charAt( 3 ) : '?';
          String extraInfo = ( len >= 5 ) ? line.substring( 4 ) : "";

          status = writeToClient( line );

          keepGoing = separator == '-' && status;

          if ( keepGoing ) {
            logger.debug( "Data (more coming from server)", ServerLogger.DEBUG4_LOG_LEVEL );
            line = serverInputReader.readLine();
          }
        }
      } while ( keepGoing );

      socket1.setSoLinger( true, 1 );
      //socket1.setSoTimeout( DATA_SOCKET_TIMEOUT * 1000 );
      socket2 = serverSocket.accept(); 
      serverSocket.setSoTimeout( 0 );

      String clientDataAddr = socket2.getInetAddress().getHostAddress();

      boolean theftOccurred = pasv && !clientAddr.equals( clientDataAddr );

      if ( theftOccurred ) {
        logger.warning( "Possible PASV port theft occured: " +
                        clientAddr + " != " + clientDataAddr );
      }

      if ( serverInfo.getPreventPortTheft() && theftOccurred ) {
        String m = "425 Possible PASV port theft, cannot open data connection.";
        clientOutput.print( m + "\r\n" );
        clientOutput.flush();
        closeSocket( socket1 );
        closeSocket( socket2 );
        return;
      }

      switch( getDirection() ) {
        case DATA_FROM_CLIENT:
          getDataFromClient();
          break;
        case DATA_FROM_SERVER:
          getDataFromServer();
          break;
        case DATA_UNSET:
        default:
          break;
      }
    }
    catch ( SocketException se ) {
      logger.debug( "SE (Data): " + se.getMessage(), ServerLogger.DEBUG4_LOG_LEVEL );
    }
    catch ( IOException ioe ) {
      logger.debug( "IOE (Data): " + ioe.getMessage(), ServerLogger.DEBUG4_LOG_LEVEL );
    }
    finally {
      isTransferring = false;
      if ( controlChannel != null ) {
        controlChannel.setControlTimeout();
      }

      logger.debug( "transfer finished", ServerLogger.DEBUG3_LOG_LEVEL );

      try {
        line = serverInputReader.readLine();

        do {
          logger.debug( "Data (from server) 2: " + line, ServerLogger.DEBUG4_LOG_LEVEL );

          if ( null == line ) {
            keepGoing = false;
          }
          else {
            int len = line.length();
            String replyCodeStr = ( len >= 3 ) ? line.substring( 0, 3 ) : null;
            int    replyCode = Util.parseInt( replyCodeStr, -1 );

            char   separator = ( len >= 4 ) ? line.charAt( 3 ) : '?';
            String extraInfo = ( len >= 5 ) ? line.substring( 4 ) : "";

            status = writeToClient( line );

            keepGoing = separator == '-' && status;

            if ( keepGoing ) {
              logger.debug( "Data (more coming from server)", ServerLogger.DEBUG4_LOG_LEVEL );
              line = serverInputReader.readLine();
            }
          }
        }
        while ( keepGoing );

        if ( abort && null != line ) {
          line = serverInputReader.readLine();
          writeToClient( line );
        }
      }
      catch ( IOException ioe ) {
        // if it dies here we have bigger problems
      }

      if ( pasv ) {
        portRange.freePort( serverSocket.getLocalPort() );
      }

      closeSocket( socket1 );
      closeSocket( socket2 );
      close( serverSocket );
    }
  }

  public synchronized boolean isTransferring() { return isTransferring; }

  public void abort() {
    abort = true;
  }

  private boolean writeToServer( String msg ) {
    serverOutputWriter.print( msg + "\r\n" );
    serverOutputWriter.flush();
    return !serverOutputWriter.checkError();
  }

  private boolean writeToClient( String msg ) {
    clientOutputWriter.print( msg + "\r\n" );
    clientOutputWriter.flush();
    return !clientOutputWriter.checkError();
  }

  public void getDataFromClient() throws IOException {
    InputStream is = null;
    OutputStream os = null;

    try {
      is = socket2.getInputStream();
      os = socket1.getOutputStream();

      if ( modeZ ) {
        if ( pasv ) {
          is = new InflaterInputStream( is, new Inflater(), 1024 );
        }
        else {
          os = new DeflaterOutputStream( os, new Deflater(), 1024 );
        }
      }

      transferData( is, os );
    }
    finally {
      closeInputStream( is );
      closeOutputStream( os );
    }
  }

  public void getDataFromServer() throws IOException {
    InputStream is = null;
    OutputStream os = null;

    try {
      is = socket1.getInputStream();
      os = socket2.getOutputStream();

      if ( modeZ ) {
        if ( pasv ) {
          os = new DeflaterOutputStream( os, new Deflater(), 1024 );
        }
        else {
          is = new InflaterInputStream( is, new Inflater(), 1024 );
        }
      }

      transferData( is, os );
    }
    finally {
      closeInputStream( is );
      closeOutputStream( os );
    }
  }

  private void transferData( InputStream is, OutputStream os ) 
                                                           throws IOException {
    byte[] b = new byte[1024];
    int size = 0;

    while( size >= 0 ) {
      socket2.setSoTimeout( DATA_SOCKET_TIMEOUT * 1000 );
      Util.clearByteArray( b );
      size = is.read( b );
      if ( !socket2.isClosed() && null != os ) {
        int writeSize = size >= 0 ? size : 0;
        os.write( b, 0, writeSize );
      }
    }

    os.flush();

    if ( os instanceof DeflaterOutputStream ) {
      ((DeflaterOutputStream)os).finish();
    }

    socket2.setSoTimeout( 0 );
  }

  private void close( ServerSocket ss ) {
    try {
      if (ss != null)
        ss.close();
      ss = null;
    }
    catch (Exception e) { }
  }

  private void closeInputStream( InputStream is ) {
    try {
      if (is != null)
        is.close();
      is = null;
    }
    catch (IOException ioe) { }
  }

  private void closeOutputStream( OutputStream os ) {
    try {
      if (os != null)
        os.close();
      os = null;
    }
    catch (IOException ioe) { 
      logger.debug( "IOE (Data/Close): " + ioe.getMessage(), ServerLogger.DEBUG4_LOG_LEVEL );
    }
  }

  private void closeSocket( Socket s ) {
    try {
      if (s != null && !s.isClosed() ) {
        try {
          s.shutdownInput();
        }
        catch ( Exception e ) { }
        try {
          s.shutdownOutput();
        }
        catch ( Exception e ) { }
        s.close();
      }
      s = null;
    }
    catch (IOException ioe) { }
  }
}
