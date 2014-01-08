package com.mumfrey.liteloader.core;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.primitives.Ints;
import com.mumfrey.liteloader.launch.ClassPathUtilities;
import com.mumfrey.liteloader.launch.InjectionStrategy;
import com.mumfrey.liteloader.launch.LiteLoaderTweaker;

import net.minecraft.launchwrapper.LaunchClassLoader;

public class LoadableFile extends File implements TweakContainer<File>
{
	private static final Pattern versionPattern = Pattern.compile("([0-9]+\\.)+[0-9]+([_A-Z0-9]+)?");

	private static final long serialVersionUID = 1L;

	/**
	 * Local logger reference
	 */
	private static Logger logger = Logger.getLogger("liteloader");

	/**
	 * True once this file has been injected into the class path 
	 */
	protected boolean injected;
	
	/**
	 * Position to inject the mod file at in the class path, if blank injects at the bottom as usual, alternatively
	 * the developer can specify "top" to inject at the top, "base" to inject above the game jar, or "above: name" to
	 * inject above a specified other library matching "name".
	 */
	protected InjectionStrategy injectionStrategy = null;

	/**
	 * Name of the tweak class
	 */
	protected String tweakClassName;
	
	/**
	 * Priority for this tweaker 
	 */
	protected int tweakPriority = 1000;
	
	/**
	 * Class path entries read from jar metadata
	 */
	protected String[] classPathEntries = null;

	protected String displayName;

	protected String version = "Unknown";

	protected String author = "Unknown";

	/**
	 * Create a new tweak container wrapping the specified file
	 */
	public LoadableFile(File parent)
	{
		super(parent.getAbsolutePath());
		this.displayName = this.getName();
		this.guessVersionFromName();
		this.readMetaData();
	}

	/**
	 * ctor for subclasses
	 */
	protected LoadableFile(String pathname)
	{
		super(pathname);
		this.displayName = this.getName();
	}
	
	private void guessVersionFromName()
	{
		Matcher versionPatternMatcher = LoadableFile.versionPattern.matcher(this.getName());
		while (versionPatternMatcher.find())
			this.version = versionPatternMatcher.group();
	}
	
	/**
	 * Search for tweaks in this file
	 */
	private void readMetaData()
	{
		JarFile jar = null;
		
		try
		{
			jar = new JarFile(this);
			if (jar.getManifest() != null)
			{
				LoadableFile.logInfo("Searching for tweaks in '%s'", this.getName());
				Attributes manifestAttributes = jar.getManifest().getMainAttributes();
				
				this.tweakClassName = manifestAttributes.getValue("TweakClass");
				if (this.tweakClassName != null)
				{
					String classPath = manifestAttributes.getValue("Class-Path");
					if (classPath != null)
					{
						this.classPathEntries = classPath.split(" ");
					}
				}
				
				Integer tweakOrder = Ints.tryParse(manifestAttributes.getValue("TweakOrder"));
				if (tweakOrder != null)
				{
					this.tweakPriority = tweakOrder.intValue();
				}
				
				if (manifestAttributes.getValue("Implementation-Title") != null)
					this.displayName = manifestAttributes.getValue("Implementation-Title");
				
				if (manifestAttributes.getValue("TweakName") != null)
					this.displayName = manifestAttributes.getValue("TweakName");
				
				if (manifestAttributes.getValue("Implementation-Version") != null)
					this.version = manifestAttributes.getValue("Implementation-Version");
				
				if (manifestAttributes.getValue("TweakVersion") != null)
					this.version = manifestAttributes.getValue("TweakVersion");
				
				if (manifestAttributes.getValue("Implementation-Vendor") != null)
					this.author = manifestAttributes.getValue("Implementation-Vendor");
				
				if (manifestAttributes.getValue("TweakAuthor") != null)
					this.author = manifestAttributes.getValue("TweakAuthor");
				
				this.injectionStrategy = InjectionStrategy.parseStrategy(manifestAttributes.getValue("TweakInjectionStrategy"), InjectionStrategy.TOP);
			}
		}
		catch (Exception ex)
		{
			LoadableFile.logWarning("Error parsing manifest entries in '%s'", this.getAbsolutePath());
		}
		finally
		{
			try
			{
				if (jar != null) jar.close();
			}
			catch (IOException ex) {}
		}
	}
	
	@Override
	public File getTarget()
	{
		return this;
	}
	
	@Override
	public String getLocation()
	{
		return this.getAbsolutePath();
	}
	
	@Override
	public URL getURL() throws MalformedURLException
	{
		return this.toURI().toURL();
	}
	
	@Override
	public String getIdentifier()
	{
		return this.getName().toLowerCase();
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.ITweakContainer#hasTweakClass()
	 */
	@Override
	public boolean hasTweakClass()
	{
		return this.tweakClassName != null;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.ITweakContainer#getTweakClassName()
	 */
	@Override
	public String getTweakClassName()
	{
		return this.tweakClassName;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.TweakContainer#getTweakPriority()
	 */
	@Override
	public int getTweakPriority()
	{
		return this.tweakPriority;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.ITweakContainer#getClassPathEntries()
	 */
	@Override
	public String[] getClassPathEntries()
	{
		return this.classPathEntries;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.ITweakContainer#hasClassTransformers()
	 */
	@Override
	public boolean hasClassTransformers()
	{
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.ITweakContainer#getClassTransformerClassNames()
	 */
	@Override
	public List<String> getClassTransformerClassNames()
	{
		return new ArrayList<String>();
	}

	
	@Override
	public boolean isInjected()
	{
		return this.injected;
	}
	
	@Override
	public boolean injectIntoClassPath(LaunchClassLoader classLoader, boolean injectIntoParent) throws MalformedURLException
	{
		if (!this.injected)
		{
			ClassPathUtilities.injectIntoClassPath(classLoader, this.getURL(), this.getInjectionStrategy());
			
			if (injectIntoParent)
			{
				LiteLoaderTweaker.addURLToParentClassLoader(this.getURL());
			}
			
			this.injected = true;
			return true;
		}
		
		return false;
	}
	
	@Override
	public InjectionStrategy getInjectionStrategy()
	{
		return this.injectionStrategy;
	}

	@Override
	public String getDisplayName()
	{
		return this.displayName;
	}

	@Override
	public String getVersion()
	{
		return this.version;
	}
	
	@Override
	public String getAuthor()
	{
		return this.author;
	}
	
	@Override
	public boolean isExternalJar()
	{
		return true;
	}

	@Override
	public boolean isToggleable()
	{
		return false;
	}
	
	@Override
	public boolean isEnabled(EnabledModsList enabledModsList, String profile)
	{
		return enabledModsList.isEnabled(profile, this.getIdentifier());
	}

	private static void logInfo(String string, Object... args)
	{
		LoadableFile.logger.info(String.format(string, args));
	}

	private static void logWarning(String string, Object... args)
	{
		LoadableFile.logger.warning(String.format(string, args));
	}
}