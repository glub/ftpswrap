
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: CertificateManagerCommand.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

import com.glub.secureftp.wrapper.*;

import java.io.*;
import java.util.*;

public class CertificateManagerCommand extends Command {
  private PrintStream out = System.out;
  private BufferedReader stdin = 
                           new BufferedReader(new InputStreamReader(System.in));
  private ServerInfo info = null;
  private final int numToList = 10;

  public CertificateManagerCommand() {
    super("4", CommandID.CERT_MGR_COMMAND_ID, 
          "Manage SSL certificates.");
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
        manageCert( 0, serverList );
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

          getResponse("Choose a configuration's certificates to manage (" + 
                      choiceRange + ")", defMsg, response);

          try {
            int value = Integer.parseInt((String)response.get(0)) - 1;

            if (value < 0 || value >= serverList.size()) {
              out.println("Invalid selection.");
              break;
            }
            manageCert( value, serverList );
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

  private void manageCert( int serverID, List serverList ) {
    info = ((ServerInfo)serverList.get(serverID));
    CertificateDataEntry cde = new CertificateDataEntry( info );
    try {
      cde.getData();
    }
    catch ( CommandException ce ) {
      out.println("> There was a problem retrieving the data.");
    }

    Setup.updateConfiguration();

    out.println("");
    out.println("> The certificate for \"" + info.getServerID() +
                "\" has been successfully updated.");
    out.println("");
  }
}
