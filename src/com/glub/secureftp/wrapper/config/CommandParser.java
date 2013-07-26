
//*****************************************************************************
//*
//* (c) Copyright 2005. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: CommandParser.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

import java.io.*;
import java.util.*;

public class CommandParser { 

  private BufferedReader input = null;
  private PrintStream out = null;

  public final static short COMMAND_NOT_FOUND  = -1;

  private static Command[] hiddenCommands = 
    {
      new NoOpCommand(), new AmbiguousCommand()
    };

  private static Command[] commands = 
    {
      new DisplayServerCommand(), new NewServerCommand(), 
      new EditServerCommand(), new DeleteServerCommand(), 
      new CertificateManagerCommand(), new QuitCommand()
    };

  public CommandParser( BufferedReader input, PrintStream out ) {
    this.input = input;
    this.out = out;

    Arrays.sort(getCommands(), new CommandComparator());

    Setup.getCommandDispatcher().addListener( CommandPlayer.class, new CommandPlayer() );
  }

  protected static Command[] getCommands() { return commands; }

  public boolean parse() {
    boolean result = true;

    try {
      Command command;

      String currentLine = input.readLine();

      if ( null == currentLine ) {
        command = new NoOpCommand();
      }
      else {
        command = parseCommand( currentLine );
      }

      short commandId = COMMAND_NOT_FOUND;

      if ( null != command ) {
        commandId = command.getId();
      }

      switch ( commandId ) {
        case CommandID.QUIT_COMMAND_ID:
          result = false;
          break;

        default:
          SecureFTPError err = postCommand( command );
      }
    }
    catch (ParserException pe) {
      out.println("");
      out.println("> Error: " + pe.getMessage());
      out.println("");
    }
    catch (IllegalArgumentException iae) {
      out.println("Usage: " + iae.getMessage());
    }
    catch (IOException ioe) { ioe.printStackTrace(); }
    catch (NullPointerException npe) {}

    return result;
  } 

  private SecureFTPError postCommand( Command command ) {
    return Setup.getCommandDispatcher().fireCommand(this, command);
  }

  protected static Command parseCommand( String input ) throws ParserException {
    Command command = null;

    if ( null == input )
      return command;

    // tokenize input
    StringTokenizer tok = new StringTokenizer( input.trim() );
    String commandToken = null;

    int commandNumber = 0;

    if ( tok.hasMoreTokens() ) {
      commandToken = tok.nextToken().trim().toLowerCase();
      try {
        commandNumber = Integer.parseInt( commandToken );
      }
      catch ( NumberFormatException nfe ) {
        if (commandToken.toLowerCase().equals("q"))
          return new QuitCommand();
        else
          throw new ParserException("!!! Invalid option !!!");
      }
    }
    else {
      return new NoOpCommand();
    }

    if ( commandNumber > commands.length ) {
      throw new ParserException("!!! Invalid selection !!!");
    }

    // get command token by name
    for ( int i = 0; i < commands.length; i++ ) {
      Command currentCommand = commands[i];
      String currentCommandName = currentCommand.getCommandName().toLowerCase();

      if ( currentCommandName.equals(commandToken) ) {
        command = currentCommand;
        break;
      }
 
      boolean found = 
        currentCommandName.startsWith(commandToken);

      if ( found && null == command ) {
        command = currentCommand;
      }
      else if ( found ) {
        command = new AmbiguousCommand();
        return command;
      }
    } 

    // set the args (if any)
    if ( null != command ) {
      ArrayList args = new ArrayList(1);
      StringBuffer argBuffer = null;

      while ( tok.hasMoreTokens() ) {
        String token = tok.nextToken();

        if ( token.startsWith("\"") ) {
          argBuffer = new StringBuffer(token.substring(1, token.length()));
          argBuffer.append(" ");
          continue;
        }
        else if ( token.endsWith("\"") ) {
          argBuffer.append(token.substring(0, token.length() - 1));
          token = argBuffer.toString();
          argBuffer = null;
        }

        if ( null == argBuffer ) {
          args.add( token );
        }
        else {
          argBuffer.append(token + " ");
        }
      }

      if ( argBuffer != null ) {
        throw new ParserException("Unbalanced quotes.");
      }

      command.setArgs( args );
    }

    return command;
  }

}

class CommandComparator implements Comparator {
  public int compare( Object o1, Object o2 ) {
    return ((Command)o1).getCommandName().compareTo(
                                                ((Command)o2).getCommandName());
  }
}

