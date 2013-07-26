
//*****************************************************************************
//*
//* (c) Copyright 2005. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: Setup.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

import com.glub.secureftp.wrapper.*;

import java.io.*;
import java.util.*;

public class Setup {
  private BufferedReader stdin = null;
  private static PrintStream out = null;

  private static CommandDispatcher commandDispatcher = new CommandDispatcher();

  public static void main( String[] args ) {
    new Setup( args ); 
  }

  public Setup( String[] args ) {
    out = System.out;
    out.println(SecureFTPWrapper.PROGRAM_NAME + " v" + SecureFTPWrapper.VERSION);
    out.println(SecureFTPWrapper.COPYRIGHT);
    out.println("");

    stdin = new BufferedReader(new InputStreamReader(System.in));

    CommandParser cp = new CommandParser(stdin, out);

    do {
      printOptions();
      out.print("Please enter a choice (0 - " + 
                (CommandParser.getCommands().length - 1) + "): ");
    } while ( cp.parse() );
  }

  public static void printOptions() {
    Command c[] = CommandParser.getCommands();
    for ( int i = 0; i < c.length; i++ ) {
      out.println(c[i].getCommandName() + ". " + c[i].getMessage());
    }
    out.println("");
  }

  public static CommandDispatcher getCommandDispatcher() {
    return commandDispatcher;
  }

  public static List getServerList() { 
    return ConfigurationManager.getInstance().getConfiguration(); 
  }

  public static void addConfiguration( ServerInfo info ) {
    ConfigurationManager.getInstance().addConfiguration( info );
    updateConfiguration();
  }

  public static void updateConfiguration() {
    try {
      ConfigurationManager.getInstance().writeConfiguration();
    }
    catch ( IOException ioe ) {}
  }
}

