
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: CommandDispatcher.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

import java.awt.event.ActionListener;
import javax.swing.event.EventListenerList;

public class CommandDispatcher extends EventListenerList {

  public void addListener( Class c, ActionListener l ) {
    //System.out.println(c);
    add( c, l );
  }

  public void removeListener( Class c, ActionListener l ) {
    remove( c, l );
  }

  public SecureFTPError fireCommand( Object source, Command command ) { 
    return fireCommand( source, command, false );
  }

  public SecureFTPError fireCommand( Object source, Command command, 
                                     boolean recordIt ) { 
    SecureFTPError result = new SecureFTPError();
    try {
      // Guaranteed to return a non-null array
      Object[] listeners = getListenerList();
    
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
        Class[] interfaces = ((Class)listeners[i]).getInterfaces();
        boolean foundInterface = false;
        for ( int j = 0; j < interfaces.length; j++ ) {
          foundInterface = (CommandHandler.class == interfaces[j]);
          if ( foundInterface )
            break;
        }

        boolean dispatchIt = true;

        if ( foundInterface && dispatchIt ) {
          SecureFTPError tRes = 
            ((CommandHandler)listeners[i+1]).handleCommand( command );

	  if ( listeners[i+1] instanceof CommandPlayer ) {
            result = tRes;
          }
        }
      }
    }
    catch ( CommandException ce ) {
    }

    return result;
  }

  public void fireMTCommand( final Object source, final Command command ) {
    fireMTCommand( source, command, new SecureFTPError() );
  }

  public void fireMTCommand( final Object source, final Command command, 
                             final SecureFTPError result ) {
    fireMTCommand( source, command, Thread.MIN_PRIORITY, result, false );
  }

  public void fireMTCommand( final Object source, final Command command, 
                             final int priority,
                             final SecureFTPError result, 
                             final boolean recordIt ) { 

    Thread t = new Thread() {
      public void run() {
        SecureFTPError tempResult = fireCommand( source, command, recordIt );
        result.setCode( tempResult.getCode() );
        result.setMessage( tempResult.getMessage() );
      }
    };
    t.setPriority( priority );
    t.start();
  }

}
