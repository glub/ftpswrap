package com.ice.syslog;

import java.io.*;
import java.net.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * The Syslog class implements the UNIX syslog protocol allowing Java
 * to log messages to a specified UNIX host. Care has been taken to
 * preserve as much of the UNIX implementation as possible.
 * <br>
 * To use Syslog, simply create an instance, and use the Syslog() method
 * to log your message. The class provides all the expected syslog constants.
 * For example, LOG_ERR is Syslog.LOG_ERR.
 * <br>
 * Version: 1.2 - July 27, 1998<br>
 * Version: 1.0 - August 14, 1996<br>
 * HomePage: <a href="http://www.ice.com/java/syslog/index.shtml">Syslog.java</a>
 *
 * @author <a href="http://www.ice.com/~time/">Tim Endres</a><br>
 * @see DatagramSocket
 * @see	InetAddress
 */

public class Syslog extends Object
	{
	// CLASS CONSTANTS

	// CLASS VARIABLES
	
	static private Syslog	logger = null;
	
	// INSTANCE VARIABLES
	
	private String			logName;
	private String			hostName;
	private int				portNum;
	private int				flags;
	private boolean			includeDate;

	private InetAddress		boundAddress;
	private DatagramSocket	socket;

	private SimpleDateFormat	date1Format;
	private SimpleDateFormat	date2Format;


	// CLASS METHODS
	
	/**
	 * Binds the Syslog class to a specified host for further logging.
	 * See the Syslog constructor for details on the parameters.
	 */
	static public void
	open( String hostname, String name, int flags )
		throws SyslogException
		{
		try {
			Syslog.logger =
				new Syslog
					( hostname, SyslogDefs.DEFAULT_PORT, name, flags );

			// Why do we provide this flag?
			// Because catching exceptions from 'log()' would be
			// too onerous for the programmer. By using this flag
			// the programmer can catch any possible exceptions that
			// might come up during logging, except for the syslog
			// server going away during execution.

			if ( (flags & SyslogDefs.LOG_START) != 0 )
				{
				Syslog.logger.syslog
					( SyslogDefs.LOG_SYSLOG, SyslogDefs.LOG_INFO,
						"syslog opened" );
				}
			}
		catch ( SyslogException ex )
			{
			throw ex;
			}
		}
	
	/**
	 * Performs a syslog to the currently bound syslog host.
	 */
	static public void
	log( int fac, int lvl, String msg )
		{
		try {
			Syslog.logger.syslog( fac, lvl, msg );
			}
		catch ( SyslogException ex )
			{
			if ( (Syslog.logger.flags & SyslogDefs.LOG_PERROR) != 0 )
				ex.printStackTrace( System.err );
			}
		}
	
	/**
	 * Unbinds the current syslog host.
	 */
	static public void
	close()
		{
		logger = null;
		}
	

	/**
	 * Creates a Syslog object instance, targeted for the UNIX host
	 * with the hostname 'hostname' on the syslog port 'port'.
	 * The only flags recognized are 'LOG_PERROR', which will log the
	 * message to Java's 'System.err'.
	 */
	public
	Syslog( String name, int flags )
		throws SyslogException
		{
		super();
		
		this.logName = name;
		this.hostName = null;
		this.portNum = SyslogDefs.DEFAULT_PORT;
		this.flags = flags;

		this.initialize();
		}
		
	/**
	 * Creates a Syslog object instance, targeted for the UNIX host
	 * with the hostname 'hostname' on the syslog port 'port'.
	 * The only flags recognized are 'LOG_PERROR', which will log the
	 * message to Java's 'System.err'.
	 */
	public
	Syslog( String hostname, int port, String name, int flags )
		throws SyslogException
		{
		super();
		
		this.logName = name;
		this.hostName = hostname;
		this.portNum = port;
		this.flags = flags;

		try {
			this.boundAddress = InetAddress.getByName( hostname );
			}
		catch ( UnknownHostException ex )
			{
			String message =
				"error locating host named '" + hostname
					+ "': " + ex.getMessage();
			
			throw new SyslogException( message );
			}

		this.initialize();
		}
	
	private void
	initialize()
		throws SyslogException
		{
		try {
			this.socket = new DatagramSocket();
			}
		catch ( SocketException ex )
			{
			String message =
				"error creating syslog udp socket: "
					+ ex.getMessage();

			throw new SyslogException( message );
			}

		// This determines if the timestamp is added to the message
		// in this, the client, or if it is left off for the server
		// to fill in. Adding it in the client is the "standard" way,
		// but most servers support the "old style" (no timestamp)
		// messages as well. I leave this choice, since some servers
		// may not work without it, and because some JDK's may not
		// support SimpleDateFormat or TimeZone correctly.

		this.includeDate = true;

		if ( this.includeDate )
			{
			// We need two separate formatters here, since there is
			// no way to get the single digit date (day of month) to
			// pad with a space instead of a zero.
			this.date1Format =
				new SimpleDateFormat
					( "MMM  d HH:mm:ss ", Locale.US );

			this.date2Format =
				new SimpleDateFormat
					( "MMM dd HH:mm:ss ", Locale.US );

			this.date1Format.setTimeZone
				( TimeZone.getDefault() );

			this.date2Format.setTimeZone
				( TimeZone.getDefault() );
			}
		}

	/**
	 * Use this method to log your syslog messages. The facility and
	 * level are the same as their UNIX counterparts, and the Syslog
	 * class provides constants for these fields. The msg is what is
	 * actually logged.
	 */
	public void
	syslog( int fac, int pri, String msg )
		throws SyslogException
		{
		this.syslog( this.boundAddress, this.portNum, fac, pri, msg );
		}

	/**
	 * Use this method to log your syslog messages. The facility and
	 * level are the same as their UNIX counterparts, and the Syslog
	 * class provides constants for these fields. The msg is what is
	 * actually logged.
	 */
	public void
	syslog( InetAddress addr, int fac, int pri, String msg )
		throws SyslogException
		{
		this.syslog( addr, this.portNum, fac, pri, msg );
		}

	/**
	 * Use this method to log your syslog messages. The facility and
	 * level are the same as their UNIX counterparts, and the Syslog
	 * class provides constants for these fields. The msg is what is
	 * actually logged.
	 */
	public void
	syslog( String hostname, int fac, int pri, String msg )
		throws SyslogException
		{
		try {
			InetAddress address = InetAddress.getByName( hostname );
			this.syslog( address, this.portNum, fac, pri, msg );
			}
		catch ( UnknownHostException ex )
			{
			String message =
				"error locating host named '" + hostname
					+ "': " + ex.getMessage();
			
			throw new SyslogException( message );
			}
		}

	/**
	 * Use this method to log your syslog messages. The facility and
	 * level are the same as their UNIX counterparts, and the Syslog
	 * class provides constants for these fields. The msg is what is
	 * actually logged.
	 */
	public void
	syslog( String hostname, int port, int fac, int pri, String msg )
		throws SyslogException
		{
		try {
			InetAddress address = InetAddress.getByName( hostname );
			this.syslog( address, port, fac, pri, msg );
			}
		catch ( UnknownHostException ex )
			{
			String message =
				"error locating host named '" + hostname
					+ "': " + ex.getMessage();
			
			throw new SyslogException( message );
			}
		}

	/**
	 * Use this method to log your syslog messages. The facility and
	 * level are the same as their UNIX counterparts, and the Syslog
	 * class provides constants for these fields. The msg is what is
	 * actually logged.
	 */
	public void
	syslog( InetAddress addr, int port, int fac, int pri, String msg )
		throws SyslogException
		{
		int				pricode;
		int				length;
		int				idx, sidx, nidx;
		StringBuffer	buffer;
		byte[]			data;
		byte[]			sBytes;
		byte[]			numbuf = new byte[32];
		String			nmObj;
		String			strObj;
		
		pricode = SyslogDefs.computeCode( fac, pri );
		Integer priObj = new Integer( pricode );
		
		if ( this.logName != null )
			{
			nmObj = new String( this.logName );
			}
		else
			{
			nmObj = new String( Thread.currentThread().getName() );
			}

		length = 4 + nmObj.length() + msg.length() + 1;
		length += (pricode > 99) ? 3 : ( (pricode > 9) ? 2 : 1 );

		String dStr = null;
		if ( this.includeDate )
			{
			// See note above on why we have two formats...
			Calendar now = Calendar.getInstance();
			if ( now.get( Calendar.DAY_OF_MONTH ) < 10 )
				dStr = this.date1Format.format( now.getTime() );
			else
				dStr = this.date2Format.format( now.getTime() );

			length += dStr.length();
			}

		data = new byte[length];
		
		idx = 0;
		data[idx++] = (byte)'<';
		
		strObj = priObj.toString( priObj.intValue() );
		sBytes = strObj.getBytes();
		System.arraycopy
			( sBytes, 0, data, idx, sBytes.length );
		idx += sBytes.length;
		
		data[idx++] = (byte)'>';

		if ( this.includeDate )
			{
			sBytes = dStr.getBytes();
			System.arraycopy
				( sBytes, 0, data, idx, sBytes.length );
			idx += sBytes.length;
			}

		sBytes = nmObj.getBytes();
		System.arraycopy
			( sBytes, 0, data, idx, sBytes.length );
		idx += sBytes.length;
		
		data[idx++] = (byte)':';
		data[idx++] = (byte)' ';

		sBytes = msg.getBytes();
		System.arraycopy
			( sBytes, 0, data, idx, sBytes.length );
		idx += sBytes.length;
		
		data[idx] = 0;

		DatagramPacket packet =
			new DatagramPacket( data, length, addr, port );
		
		try {
			socket.send( packet );
			}
		catch ( IOException ex )
			{
			String message =
				"error sending message: '" + ex.getMessage() + "'";

			System.err.println( message );
			throw new SyslogException( message );
			}

		if ( (this.flags & SyslogDefs.LOG_PERROR) != 0 )
			{
			if ( this.logName != null )
				{
				System.err.print( this.logName + ": " );
				}
			else
				{
				System.err.print
					( Thread.currentThread().getName() + ": " );
				}

			System.err.println( msg );
			}
		}
	}


