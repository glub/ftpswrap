
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: AmbiguousCommand.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

public class AmbiguousCommand extends Command {
  public AmbiguousCommand() {
    super("", CommandID.AMBIGUOUS_COMMAND_ID, "");
  }

  public SecureFTPError doIt() throws CommandException { 
    return new SecureFTPError(); 
  }
}

