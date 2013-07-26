
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: DeleteServerCommand.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

import com.glub.secureftp.wrapper.*;

import java.io.*;
import java.util.*;

public class DeleteServerCommand extends Command {
  private PrintStream out = System.out;
  private BufferedReader stdin = 
                           new BufferedReader(new InputStreamReader(System.in));
  private ServerInfo info = null;
  private final int numToList = 10;

  public DeleteServerCommand() {
    super("3", CommandID.DELETE_SERVER_COMMAND_ID, 
          "Delete an existing server");
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
        deleteServer( 0, serverList );
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

          getResponse("Choose a configuration to delete (" + 
                      choiceRange + ")", defMsg, response);

          out.println("");
          try {
            int valueToDelete = Integer.parseInt((String)response.get(0)) - 1;

            if (valueToDelete < 0 || valueToDelete >= serverList.size()) {
              out.println("Invalid selection.");
              break;
            }

            deleteServer( valueToDelete, serverList );
            break;
          }
          catch ( NumberFormatException nfe ) {
          }
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

  private boolean deleteServer( int serverID, List serverList ) {
    ArrayList response = new ArrayList(1);
    info = ((ServerInfo)serverList.get(serverID));
    boolean deleted = false;
    try {
      if (getResponse("Are you sure you want to delete \"" + 
        info.getServerID() + "\"?", "n", response)) {
        if ( ((String)response.get(0)).equalsIgnoreCase("y") ) {
          SecureFTPWrapper.getServerList().remove(serverID);
          deleted = true;
        }
      }
    }
    catch ( IOException ioe ) {
      deleted = false;
    }

    out.println("");

    if ( deleted ) {
      out.println("> \"" + info.getServerID() + "\" deleted.");
    }
    else {
      out.println("> Deletion aborted.");
    }

    out.println("");

    Setup.updateConfiguration();
  
    return deleted;
  }
}



