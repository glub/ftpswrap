
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: PortRange.java 108 2009-11-19 22:21:35Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper;

import com.glub.secureftp.common.*;

import java.io.*;
import java.net.*;

import javax.net.*;

public class PortRange {
  public static final byte UNUSED_PORT = 0;
  public static final byte USED_PORT   = 1;

  private int lastServerPort = 0;
  private int minPassivePort = 0;
  private int maxPassivePort = 0;
  private byte[] serverPorts = null;
  private boolean usePortRange = false;

  private ServerInfo serverInfo = null;
  private ServerLogger logger = null;

  public PortRange( ServerInfo serverInfo ) {
    this.serverInfo = serverInfo;

    this.minPassivePort = serverInfo.getPassivePortStart();
    this.maxPassivePort = serverInfo.getPassivePortEnd();

    this.logger = ServerLogger.getLogger( serverInfo );

    if ( !serverInfo.getFirewallEnabled() ) {
      return;
    }

    if ( minPassivePort == 0 && maxPassivePort == 0 ) {
      return;
    }

    if ( minPassivePort < maxPassivePort &&
         minPassivePort > 0 && minPassivePort < 65536 && 
         maxPassivePort > 0 && maxPassivePort < 65536 ) {
      usePortRange = true;
    }

    if ( !usePortRange ) {
      String err = "Port range is not formatted correctly.";
      if ( serverInfo.getLogLevel() >= ServerLogger.DEBUG3_LOG_LEVEL ) {
        logger.warning( err );
      }
    }
    else {
      serverPorts = new byte[(maxPassivePort - minPassivePort) + 1];
    }
  }

  public synchronized ServerSocket getServerSocket( String host, int backlog,
                                                    ServerSocketFactory ssf ) 
                                                         throws IOException {
    ServerSocket ss = null;
    int port = 0;

    if ( usePortRange ) {
      boolean portNotFound = true;
      while ( portNotFound ) {
        if ( lastServerPort < minPassivePort ) {
          lastServerPort = 
          (int)(Math.random()*(maxPassivePort-minPassivePort)+minPassivePort);
        }
        else if ( lastServerPort > maxPassivePort ) {
          lastServerPort = minPassivePort;
        }

        port = lastServerPort;

        logger.debug( "next avail port: " + port, ServerLogger.DEBUG4_LOG_LEVEL );

        boolean noPortsAvailable = true;

        for ( int i = 0; i < serverPorts.length; i++ ) {
          int pos = (port - minPassivePort) + i;
          if (pos >= serverPorts.length) {
            pos = 0;
          }

          port = minPassivePort + pos;

          if (port > maxPassivePort) {
            pos = 0;
            port = minPassivePort;
          }

          if ( serverPorts[pos] == USED_PORT ) {
            logger.debug( "port used: " + (minPassivePort + pos), ServerLogger.DEBUG4_LOG_LEVEL );
            continue;
          }
          else {
            logger.debug( "port now used: " + port, ServerLogger.DEBUG4_LOG_LEVEL );
            noPortsAvailable = false;
            break;
          }
        }

        if ( noPortsAvailable ) {
          throw new IOException("No free ports available.");
        }

        // we might have a free port, test it and see
        try {
          if ( ssf instanceof javax.net.ssl.SSLServerSocketFactory ) {
            ss = 
              SSLUtil.createServerSocket( port,
                                          backlog,
                                          host,
                                          (javax.net.ssl.SSLServerSocketFactory)ssf ); 
          }
          else {
            ss = 
              ssf.createServerSocket( port,
                                      backlog,
                                      InetAddress.getByName( host ) ); 
          }

          serverPorts[port - minPassivePort] = USED_PORT;

          portNotFound = false;
        }
        catch ( Exception e ) {
          // if we can't create the socket, it's used.
          if ( serverInfo.getLogLevel() >= ServerLogger.DEBUG3_LOG_LEVEL ) {
            logger.warning( "can't accept on port " + port );
          }

          serverPorts[port - minPassivePort] = USED_PORT;
        }

        lastServerPort = port + 1;
      }
    }
    else {
      if ( ssf instanceof javax.net.ssl.SSLServerSocketFactory ) {
        ss = 
          SSLUtil.createServerSocket( port,
                                      backlog,
                                      host,
                                      (javax.net.ssl.SSLServerSocketFactory)ssf ); 
      }
      else {
        ss =  
          ssf.createServerSocket( port,
                                  backlog,
                                  InetAddress.getByName( host ) );  
      }
    }

    return ss;
  }

  public void freePort( int port ) {
    if ( usePortRange && (port - minPassivePort) < serverPorts.length ) {
      serverPorts[port - minPassivePort] = UNUSED_PORT;
      logger.debug( "port now free: " + port, ServerLogger.DEBUG4_LOG_LEVEL );
    }
  }
}
