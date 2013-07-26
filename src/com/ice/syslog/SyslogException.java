
package com.ice.syslog;

import java.lang.Exception;

public class SyslogException extends Exception
	{
	SyslogException()
		{
		super();
		}
	
	SyslogException( String msg )
		{
		super( msg );
		}
	}

