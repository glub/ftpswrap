
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: QuitCommand.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

public class QuitCommand extends Command {
  public QuitCommand() {
    super("5", CommandID.QUIT_COMMAND_ID, "Exit");
  }

  public SecureFTPError doIt() throws CommandException { 
    return new SecureFTPError(); 
  }
}


