package com.mumfrey.liteloader.util.log;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class ConsoleHandler extends StreamHandler
{
	/**
	 * Create a <tt>ConsoleHandler</tt> for <tt>System.err</tt>.
	 * <p>
	 * The <tt>ConsoleHandler</tt> is configured based on <tt>LogManager</tt>
	 * properties (or their default values).
	 * 
	 */
	public ConsoleHandler()
	{
		setOutputStream(System.out);
	}
	
	/**
	 * Publish a <tt>LogRecord</tt>.
	 * <p>
	 * The logging request was made initially to a <tt>Logger</tt> object, which
	 * initialized the <tt>LogRecord</tt> and forwarded it here.
	 * <p>
	 * 
	 * @param record description of the log event. A null record is silently ignored and is not published
	 */
	@Override
	public synchronized void publish(LogRecord record)
	{
		super.publish(record);
		this.flush();
	}
	
	/**
	 * Override <tt>StreamHandler.close</tt> to do a flush but not to close the
	 * output stream. That is, we do <b>not</b> close <tt>System.err</tt>.
	 */
	@Override
	public synchronized void close()
	{
		this.flush();
	}
}
