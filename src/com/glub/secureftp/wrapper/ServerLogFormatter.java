
//*****************************************************************************
//*
//* (c) Copyright 2007. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: ServerLogFormatter.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper;

import com.glub.util.*;

import java.io.*;
import java.util.logging.*;

public class ServerLogFormatter extends Formatter {
  public String format( LogRecord record ) {
    StringBuffer buf = new StringBuffer( 1000 );
    buf.append( new java.util.Date() );
    buf.append( " : " );

    if ( Level.INFO != record.getLevel() ) {
      buf.append( record.getLevel() );
      buf.append( ": " );
    }

    buf.append( formatMessage(record) );

    if ( Util.isWinOS() ) {
      buf.append( "\r\n" );
    }
    else {
      buf.append( "\n" );
    }

    return buf.toString();
  }
}
