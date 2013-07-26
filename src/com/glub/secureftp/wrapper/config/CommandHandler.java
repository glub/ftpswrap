
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: CommandHandler.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

import java.awt.event.ActionListener;

public interface CommandHandler extends ActionListener {

  public SecureFTPError handleCommand( Command command ) 
                                                        throws CommandException;

}
