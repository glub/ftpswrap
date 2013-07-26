
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: SecureFTPError.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

public class SecureFTPError {
  private int errorCode = OK;
  private String msg = null;

  public final static int OK = 0;
  public final static int BAD_ARGUMENTS = 1;
  public final static int UNKNOWN = 99;

  public SecureFTPError() {
    this( OK, null );
  }

  public SecureFTPError( int errorCode ) {
    this( errorCode, null );
  }

  public SecureFTPError( int errorCode, String msg ) {
    setCode( errorCode );
    setMessage( msg );
  }

  public int getCode() { return errorCode; }
  public void setCode( int errorCode ) { this.errorCode = errorCode; }

  public String getMessage() { return msg; }
  public void setMessage( String msg ) {
    this.msg = ( null == msg ) ? "" : msg;
  }

  public String toString() {
    return getCode() + ": " + getMessage();
  }
}

