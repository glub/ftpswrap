
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: ServerException.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper;

public class ServerException extends Exception {
  public ServerException() {
    super();
  }

  public ServerException( String message ) {
    super( message );
  }
}

