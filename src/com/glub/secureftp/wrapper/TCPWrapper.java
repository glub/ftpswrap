
//*****************************************************************************
//*
//* (c) Copyright 2007. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: TCPWrapper.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper;

import java.io.*;
import java.net.*;
import java.util.*;

public class TCPWrapper extends HashMap {
  public static final int UNSET = -1;
  public static final int ALLOW = 0;
  public static final int DENY  = 1;

  private ServerLogger logger = null;
  private ServerInfo serverInfo = null;
  private int status = UNSET;
  private File inputFile = null;

  public TCPWrapper( ServerInfo serverInfo, ServerLogger logger ) {
    super();
    this.serverInfo = serverInfo;
    this.logger = logger;
    initWrapper();
  }

  private void initWrapper() {
    if ( serverInfo.getAllowFile() != null &&
         serverInfo.getAllowFile().exists() ) {
      status = ALLOW;
      inputFile = serverInfo.getAllowFile();
    }
    else if ( serverInfo.getDenyFile() != null &&
         serverInfo.getDenyFile().exists() ) {
      status = DENY;
      inputFile = serverInfo.getDenyFile();
    }

    if ( null != inputFile ) {
      loadFile( inputFile );
      (new TCPWrapperUpdate(this, inputFile)).start();
    }
  }

  public boolean allow( InetAddress ip ) {
    boolean result = false;

    Object val = get( ip.getHostAddress() );

    if ( status == UNSET ) {
      result = true;
    }
    else if ( status == ALLOW ) {
      result = val != null;
    }
    else if ( status == DENY ) {
      result = val == null;
    }

    return result;
  }

  protected void loadFile( File inputFile ) {
    clear();
    try {
      FileReader fileRead = new FileReader( inputFile );
      BufferedReader read = new BufferedReader( fileRead );
      String line = null;

      logger.debug( "Loading IP ACLs...", ServerLogger.DEBUG2_LOG_LEVEL );

      while( (line = read.readLine()) != null ) {
        if ( line.trim().length() > 0 ) {
          put( line, new Integer(1) );
          if ( serverInfo.getLogLevel() >= ServerLogger.DEBUG3_LOG_LEVEL ) {
            String statStr = "ALLOWING";
            if ( status == DENY ) {
              statStr = "DENYING";
            }
  
            logger.info( statStr + " IP: " + line);
          }
        }
      }
    }
    catch ( FileNotFoundException fnfe ) {
      logger.warning("IP control file not found: " + 
                     inputFile.getAbsolutePath());
    }
    catch ( IOException ioe ) {
      logger.warning( "Problem parsing IP control file: " + ioe.getMessage() );
    }
  }
}

class TCPWrapperUpdate extends Thread {
  private TCPWrapper tcpd = null;
  private File inputFile = null;
  private long modTime = 0L;
  private boolean running = true;

  public TCPWrapperUpdate( TCPWrapper tcpd, File inputFile ) {
    this.tcpd = tcpd;
    this.inputFile = inputFile;
    modTime = inputFile.lastModified();
  }

  public void run() {
    while( running ) {
      if ( null != inputFile && inputFile.lastModified() != modTime ) {
        modTime = inputFile.lastModified();
        tcpd.loadFile( inputFile );
      }

      try {
        sleep( 20000 );
      }
      catch( InterruptedException ie ) {}
    }
  }

  public void terminate() { running = false; }
}
