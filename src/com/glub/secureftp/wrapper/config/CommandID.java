
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: CommandID.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

public interface CommandID {
  public final static short NOOP_COMMAND_ID           = 0;
  public final static short AMBIGUOUS_COMMAND_ID      = 1;
  public final static short DISPLAY_SERVER_COMMAND_ID = 2;
  public final static short NEW_SERVER_COMMAND_ID     = 3;
  public final static short EDIT_SERVER_COMMAND_ID    = 4;
  public final static short DELETE_SERVER_COMMAND_ID  = 5;
  public final static short CERT_MGR_COMMAND_ID       = 6;
  public final static short QUIT_COMMAND_ID           = 7;
}
