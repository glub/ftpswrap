
//*****************************************************************************
//*
//* (c) Copyright 2007. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: SecureFTPWrapper.java 152 2009-12-31 05:11:43Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper;

import com.glub.secureftp.wrapper.config.*;
import com.glub.jni.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;

public class SecureFTPWrapper {
  public static final String PROGRAM_NAME = "Glub Tech Secure FTP Wrapper";
  public static final String UPDATE_ID = "ftpswrap_3";
  public static final String VERSION = "3.0.9";
  public static final int MAJOR_VERSION = 3;
  public static final String COPYRIGHT = "Copyright (c) 1999-2013 " +
                                   "Glub Tech, Inc.";

  private static boolean running = true;

  public static synchronized boolean runServer() { return running; }

  private static HashMap licenseMap = new HashMap();

  private static ArrayList servers = null;

  public static void main( String[] args ) {
    new SecureFTPWrapper( args ); 
  }

  public SecureFTPWrapper( String[] args ) {
    List serverList = getServerList();
    Iterator iter = serverList.iterator();

    if ( 0 == serverList.size() ) {
      System.err.println( "Configuration has to be setup before wrapper can be run." );
      return;
    }

    servers = new ArrayList( serverList.size() );

    Logger rootLogger = Logger.getLogger("");
    Handler[] handlers = rootLogger.getHandlers();
    if (handlers[0] instanceof ConsoleHandler) {
        rootLogger.removeHandler(handlers[0]);
    }

    try {
      while ( iter.hasNext() ) {
        ServerInfo info = (ServerInfo)iter.next();
        Server server = new Server( info );
        servers.add( server );
        server.start();
      }
    }
    catch ( ServerException se ) {
      System.err.println( "Secure FTP Wrapper Error: " + se.getMessage() );
    }

    try {
      for( int i = 0; i < servers.size(); i++ ) {
        ((Server)servers.get(i)).join();
      }
    }
    catch ( InterruptedException ie ) { }

    changeRuntimePermissions();
  }

  public static void shutdown() {
    running = false;

    for( int i = 0; i < servers.size(); i++ ) {
      Server s = (Server)servers.get(i);
      if ( s.isAlive() ) {
        s.shutdown();
      }
    }

    System.exit( 0 );
  }

  public static List getServerList() { 
    return ConfigurationManager.getInstance().getConfiguration(); 
  }

  private void changeRuntimePermissions() {
    String group = System.getProperty("glub.group");
    if ( null != group ) {
      try {
        int gid = Integer.parseInt(group);
        if ( UID.FAILURE == UID.setgid(gid) ||
             UID.FAILURE == UID.setegid(gid) ) {
          System.err.println("Failed to change group: " + gid);
        }
      }
      catch ( NumberFormatException nfe ) {
        System.err.println("Failed to change group: enter valid group ID number.");
      }
    }

    String user = System.getProperty("glub.user");

    if ( null != user ) {
      try {
        int uid = Integer.parseInt(user);
        if ( UID.FAILURE == UID.setuid(uid) ||
             UID.FAILURE == UID.seteuid(uid) ) {
          System.err.println("Failed to change user: " + uid);
        }
      }
      catch ( NumberFormatException nfe ) {
        System.err.println("Failed to change user: enter valid user ID number.");
      }
    }
  }
}

