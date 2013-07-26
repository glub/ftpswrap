
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: CommandPlayer.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

public class CommandPlayer implements CommandHandler {
  public void actionPerformed(java.awt.event.ActionEvent e){}

  public SecureFTPError handleCommand( Command c ) throws CommandException {
    return c.doIt();
  }
}
