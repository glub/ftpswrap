
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: NewServerCommand.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

import com.glub.secureftp.wrapper.*;

import java.io.*;
import java.util.*;

public class NewServerCommand extends Command {
  private PrintStream out = System.out;
  private BufferedReader stdin = 
                           new BufferedReader(new InputStreamReader(System.in));
  private ServerInfo info = null;

  public NewServerCommand() {
    super("1", CommandID.NEW_SERVER_COMMAND_ID, "Create a new server");
  }

  public SecureFTPError doIt() throws CommandException { 
    info = new ServerInfo();

    ServerInfoDataEntry side = new ServerInfoDataEntry( info );
    side.getData();

    out.println("");
    out.println( info );
    out.println("");

    Setup.addConfiguration(info);

    return new SecureFTPError(); 
  }
}


