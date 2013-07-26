
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: DataChannelException.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper;

import java.io.*;

public class DataChannelException extends IOException {
  public DataChannelException() {
    super();
  }

  public DataChannelException( String message ) {
    super( message );
  }
}

