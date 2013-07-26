
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: CommandException.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

public class CommandException extends Exception {
  public CommandException() {
    super();
  }

  public CommandException( String message ) {
    super( message );
  }
}

