package com.mumfrey.liteloader.update.jarassassin;

import java.io.File;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class JarAssassin extends Thread
{
	private final File jarFile;
	
	private final long startTime;
	
	private final long lifeSpan;

	private final boolean debug;
	
	private final JarAssassinLogWindow frame;

	public JarAssassin(File jarFile, long lifeSpan, boolean debug)
	{
		this.jarFile = jarFile;
		this.startTime = System.currentTimeMillis();
		this.lifeSpan = System.currentTimeMillis() + (lifeSpan * 1000);
		this.debug = debug;
		
		this.frame = new JarAssassinLogWindow();
	}
	
	public long getStartTime()
	{
		return this.startTime;
	}
	
	public long getLifeSpan()
	{
		return this.lifeSpan;
	}
	
	@Override
	public void run()
	{
		if (this.debug)
		{
			this.frame.setVisible(true);
		}
		
		this.log("Attempting to delete " + this.jarFile.getAbsolutePath());
		while (!this.jarFile.delete() && this.jarFile.isFile())
		{
			if (System.currentTimeMillis() > this.lifeSpan)
			{
				this.log("Timeout");
			}
			
			try
			{
				this.log("Waiting...");
				Thread.sleep(1000);
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
			}

			this.log("Attempting to delete " + this.jarFile.getAbsolutePath());
		}

		this.frame.setVisible(false);
		this.log("Ending");
		System.exit(0);
	}

	public static void main(String[] args)
	{
        OptionParser optionParser = new OptionParser();
        optionParser.allowsUnrecognizedOptions();
        ArgumentAcceptingOptionSpec<File> jarFileOption = optionParser.accepts("jarFile").withRequiredArg().ofType(File.class).required();
        ArgumentAcceptingOptionSpec<Integer> lifeSpanOption = optionParser.accepts("lifeSpan").withRequiredArg().ofType(Integer.class).defaultsTo(300);
		try
		{
			OptionSet options = optionParser.parse(args);
	        JarAssassin instance = new JarAssassin(options.valueOf(jarFileOption), options.valueOf(lifeSpanOption), options.has("debug"));
	        instance.start();
		}
		catch (OptionException ex)
		{
			System.err.println(ex.getMessage());
			System.exit(-1);
		}
	}

	private void log(String message)
	{
		System.err.println(message);
		this.frame.log(message);
	}
}
