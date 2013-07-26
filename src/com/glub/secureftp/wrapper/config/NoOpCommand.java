
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: NoOpCommand.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

public class NoOpCommand extends Command {
  public NoOpCommand() {
    super("", CommandID.NOOP_COMMAND_ID, "");
  }

  public SecureFTPError doIt() throws CommandException { 
    return new SecureFTPError();
  }
}

