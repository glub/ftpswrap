
//*****************************************************************************
//*
//* (c) Copyright 2007. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: ServerLogger.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper;

import com.glub.util.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.ice.syslog.*;

public class ServerLogger {
  public static final int INFO_LOG_LEVEL = 0;
  public static final int DEBUG1_LOG_LEVEL = 1;
  public static final int DEBUG2_LOG_LEVEL = 2;
  public static final int DEBUG3_LOG_LEVEL = 3;
  public static final int DEBUG4_LOG_LEVEL = 4;
  public static final int DEBUG5_LOG_LEVEL = 5;
  public static final int DEBUG6_LOG_LEVEL = 6;

  private static final int SYSLOG_FILE = 0;
  private static final int LOCAL_FILE = 1;

  private int logType = LOCAL_FILE;

  private ServerInfo info = null;

  static HashMap logMap = new HashMap();
  static HashMap fhMap = new HashMap();

  private Syslog syslog = null;
  private int syslogLoc = SyslogDefs.LOG_LOCAL0;

  public ServerLogger( ServerInfo info ) {
    this.info = info;
    if ( info.getLogType().toLowerCase().equals("syslog") ) {
      logType = SYSLOG_FILE;
    }
    else if ( info.getLogType().toLowerCase().equals("file") ) {
      logType = LOCAL_FILE;
    }
  }

  public static ServerLogger getLogger( ServerInfo info ) {
    if ( null == logMap.get(info) ) {
      ServerLogger logger = new ServerLogger( info );
      logMap.put( info, logger );
    }

    return (ServerLogger)logMap.get( info );
  }

  public void debug( String msg, int debugLevel ) {
    if ( info.getLogLevel() < debugLevel ) {
      return;
    }

    if ( LOCAL_FILE == logType ) {
      getLocalFileLogger( info ).info( msg );
    }
    else if ( SYSLOG_FILE == logType ) {
      Syslog s = getSyslogLogger( info );
      if ( s != null ) {
        try {
          s.syslog( info.getLogHost(), syslogLoc, SyslogDefs.LOG_DEBUG, msg ); 
        } catch ( SyslogException se ) {}
      }
    }
  }

  public void info( String msg ) {
    if ( LOCAL_FILE == logType ) {
      getLocalFileLogger( info ).info( msg );
    }
    else if ( SYSLOG_FILE == logType ) {
      Syslog s = getSyslogLogger( info );
      if ( s != null ) {
        try {
          s.syslog( info.getLogHost(), syslogLoc, SyslogDefs.LOG_INFO, msg ); 
        } catch ( SyslogException se ) { se.printStackTrace(); }
      }
    }
  }

  public void warning( String msg ) {
    if ( LOCAL_FILE == logType ) {
      getLocalFileLogger( info ).warning( msg );
    }
    else if ( SYSLOG_FILE == logType ) {
      Syslog s = getSyslogLogger( info );
      if ( s != null ) {
        try {
          s.syslog( info.getLogHost(), syslogLoc, SyslogDefs.LOG_WARNING, msg ); 
        } catch ( SyslogException se ) {}
      }
    }
  }

  public void severe( String msg ) {
    if ( LOCAL_FILE == logType ) {
      getLocalFileLogger( info ).warning( msg );
    }
    else if ( SYSLOG_FILE == logType ) {
      Syslog s = getSyslogLogger( info );
      if ( s != null ) {
        try {
          s.syslog( info.getLogHost(), syslogLoc, SyslogDefs.LOG_CRIT, msg ); 
        } catch ( SyslogException se ) {}
      }
    }
  }

  private Syslog getSyslogLogger( ServerInfo info ) {
    if ( syslog == null ) {
      try {
        syslog = new Syslog( info.getServerID(), SyslogDefs.LOG_ODELAY );

        String loc = info.getLogLocation();

        if ( loc.toUpperCase().equals("KERN") ) syslogLoc = SyslogDefs.LOG_KERN;
        else if ( loc.toUpperCase().equals("USER") ) syslogLoc = SyslogDefs.LOG_USER;
        else if ( loc.toUpperCase().equals("MAIL") ) syslogLoc = SyslogDefs.LOG_MAIL;
        else if ( loc.toUpperCase().equals("DAEMON") ) syslogLoc = SyslogDefs.LOG_DAEMON;
        else if ( loc.toUpperCase().equals("AUTH") ) syslogLoc = SyslogDefs.LOG_AUTH;
        else if ( loc.toUpperCase().equals("SYSLOG") ) syslogLoc = SyslogDefs.LOG_SYSLOG;
        else if ( loc.toUpperCase().equals("LPR") ) syslogLoc = SyslogDefs.LOG_LPR;
        else if ( loc.toUpperCase().equals("NEWS") ) syslogLoc = SyslogDefs.LOG_NEWS;
        else if ( loc.toUpperCase().equals("UUCP") ) syslogLoc = SyslogDefs.LOG_UUCP;
        else if ( loc.toUpperCase().equals("CRON") ) syslogLoc = SyslogDefs.LOG_CRON;
        else if ( loc.toUpperCase().equals("LOCAL0") ) syslogLoc = SyslogDefs.LOG_LOCAL0;
        else if ( loc.toUpperCase().equals("LOCAL1") ) syslogLoc = SyslogDefs.LOG_LOCAL1;
        else if ( loc.toUpperCase().equals("LOCAL2") ) syslogLoc = SyslogDefs.LOG_LOCAL2;
        else if ( loc.toUpperCase().equals("LOCAL3") ) syslogLoc = SyslogDefs.LOG_LOCAL3;
        else if ( loc.toUpperCase().equals("LOCAL4") ) syslogLoc = SyslogDefs.LOG_LOCAL4;
        else if ( loc.toUpperCase().equals("LOCAL5") ) syslogLoc = SyslogDefs.LOG_LOCAL5;
        else if ( loc.toUpperCase().equals("LOCAL6") ) syslogLoc = SyslogDefs.LOG_LOCAL6;
        else if ( loc.toUpperCase().equals("LOCAL7") ) syslogLoc = SyslogDefs.LOG_LOCAL7;
      }
      catch ( SyslogException se ) {
        System.err.println( "Cannot write to syslog: " + se.getMessage() );
      }
    }

    return syslog;
  }

  private Logger getLocalFileLogger( ServerInfo info ) {
    Logger logger = Logger.getLogger( info.getServerID() );

    try {
      if ( null == fhMap.get(info) ) {
        int fileLimit = GTOverride.getInt( "glub.log.limit", 0 );
        int fileCount = GTOverride.getInt( "glub.log.count", 1 );
        FileHandler fileHandler = 
          new FileHandler( info.getLogLocation(), fileLimit, fileCount, true );
        fileHandler.setFormatter( new ServerLogFormatter() );
        logger.addHandler( fileHandler );
        fhMap.put( info, fileHandler );
        Runtime.getRuntime().addShutdownHook( new LoggerInterruptThread(logger, fileHandler) );
      }
    }
    catch ( IOException ioe ) {
      System.err.println( "Cannot write to file: " + ioe.getMessage() );
    }

    return logger;
  }
}

class LoggerInterruptThread extends Thread {
  Logger logger = null;
  FileHandler fh = null;

  public LoggerInterruptThread( Logger logger, FileHandler fh ) {
    this.logger = logger;
    this.fh = fh;
  }

  public void run() {
    if ( null != fh ) {
      fh.close();
      logger.removeHandler( fh );
    }
  }
}
