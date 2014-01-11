package com.mumfrey.liteloader.util.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

/**
 * Gateway class for the log4j logger
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderLogger
{
	private static Logger logger = (Logger)LogManager.getLogger("LiteLoader");;
	
	public static Logger getLogger()
	{
		return LiteLoaderLogger.logger;
	}
	
	private static void log(Level level, String format, Object... data)
	{
		LiteLoaderLogger.logger.log(level, String.format(format, data));
	}
	
	private static void log(Level level, Throwable th, String format, Object... data)
	{
		LiteLoaderLogger.logger.log(level, String.format(format, data), th);
	}
	
	public static void severe(String format, Object... data)
	{
		LiteLoaderLogger.log(Level.ERROR, format, data);
	}
	
	public static void severe(Throwable th, String format, Object... data)
	{
		LiteLoaderLogger.log(Level.ERROR, th, format, data);
	}
	
	public static void warning(String format, Object... data)
	{
		LiteLoaderLogger.log(Level.WARN, format, data);
	}
	
	public static void warning(Throwable th, String format, Object... data)
	{
		LiteLoaderLogger.log(Level.WARN, th, format, data);
	}
	
	public static void info(String format, Object... data)
	{
		LiteLoaderLogger.log(Level.INFO, format, data);
	}
}