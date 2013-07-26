
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: EditServerCommand.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

import com.glub.secureftp.wrapper.*;

import java.io.*;
import java.util.*;

public class EditServerCommand extends Command {
  private PrintStream out = System.out;
  private BufferedReader stdin = 
                           new BufferedReader(new InputStreamReader(System.in));
  private ServerInfo info = null;
  private final int numToList = 10;

  public EditServerCommand() {
    super("2", CommandID.EDIT_SERVER_COMMAND_ID, "Edit an existing server");
  }

  public SecureFTPError doIt() throws CommandException { 
    info = new ServerInfo();

    try {
      List serverList = SecureFTPWrapper.getServerList();

      out.println("");

      ArrayList response = new ArrayList(1);

      if ( serverList.size() == 0 ) {
        out.println("> No servers exist.");
        out.println("");
      }
      else if ( serverList.size() == 1 ) {
        updateServer( 0, serverList );
        return new SecureFTPError(); 
      }

      int count = 0;

      for( int i = 0; i < serverList.size(); i++, count++ ) {
        ServerInfo tempInfo = (ServerInfo)serverList.get(i);
        out.println( i + 1 + ". " + tempInfo.getServerID() );

        if ( i % numToList == numToList - 1 ||
             serverList.size() == i + 1 ) {
          int startNum = i + 1 - count;
          count = -1;
          int endNum = i + 1;
          out.println("");

          String choiceRange = startNum + "-" + endNum;

          if ( startNum == endNum ) {
            choiceRange = startNum + "";
          }

          String defMsg = "next";
          if ( serverList.size() <= numToList ) {
            defMsg = "main menu";
          }

          getResponse("Choose a configuration to edit (" + 
                      choiceRange + ")", defMsg, response);
          out.println("");
          try {
            int valueToEdit = Integer.parseInt((String)response.get(0)) - 1;
            updateServer( valueToEdit, serverList );
            break;
          }
          catch ( NumberFormatException nfe ) {}
        }
      }
    }
    catch ( IOException ioe ) {
    }

    return new SecureFTPError(); 
  }

  protected boolean getResponse( String question, String def,
                                 ArrayList response ) throws IOException {
    boolean result = false;

    response.clear();

    if ( null != def )
      out.print(question + " [" + def + "]: ");
    else
      out.print(question + ": ");

    String in = stdin.readLine().trim();

    if ( in.length() > 0 )
      result = true;

    if ( in.length() == 0 ) {
      in = def;
    }

    response.add( in );

    return result;
  }

  private void updateServer( int serverID, List serverList ) {
    info = ((ServerInfo)serverList.get(serverID));
    ServerInfoDataEntry side = new ServerInfoDataEntry( info );
    try {
      side.getData();
    }
    catch ( CommandException ce ) {
      out.println("> There was a problem retrieving the data.");
      return;
    }

    Setup.updateConfiguration();

    out.println("");
    out.println("> \"" + info.getServerID() + 
                "\" has been successfully updated.");
    out.println("");
  }
}



