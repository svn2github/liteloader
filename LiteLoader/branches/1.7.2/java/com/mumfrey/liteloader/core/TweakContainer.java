package com.mumfrey.liteloader.core;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import com.mumfrey.liteloader.launch.LiteLoaderTweaker;

import net.minecraft.launchwrapper.LaunchClassLoader;

public class TweakContainer extends File implements Loadable<File>, Injectable
{
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
	protected String injectAt;

	/**
	 * Name of the tweak class
	 */
	protected String tweakClassName;
	
	/**
	 * Class path entries read from jar metadata
	 */
	protected String[] classPathEntries = null;

	/**
	 * Create a new tweak container wrapping the specified file
	 */
	public TweakContainer(File parent)
	{
		super(parent.getAbsolutePath());
		this.findTweaks();
	}

	/**
	 * ctor for subclasses
	 */
	protected TweakContainer(String pathname)
	{
		super(pathname);
	}
	
	/**
	 * Search for tweaks in this file
	 */
	private void findTweaks()
	{
		JarFile jar = null;
		
		try
		{
			jar = new JarFile(this);
			if (jar.getManifest() != null)
			{
				TweakContainer.logInfo("Searching for tweaks in '%s'", this.getName());
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
			}
		}
		catch (Exception ex)
		{
			TweakContainer.logWarning("Error parsing tweak class manifest entry in '%s'", this.getAbsolutePath());
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
	public String getIdentifier()
	{
		return this.getName().toLowerCase();
	}
	
	public boolean hasTweakClass()
	{
		return this.tweakClassName != null;
	}
	
	public String getTweakClassName()
	{
		return this.tweakClassName;
	}
	
	public String[] getClassPathEntries()
	{
		return this.classPathEntries;
	}
	
	public boolean hasClassTransformers()
	{
		return false;
	}
	
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
			if (injectIntoParent)
			{
				LiteLoaderTweaker.addURLToParentClassLoader(this.toURI().toURL());
			}
			
			classLoader.addURL(this.toURI().toURL());
			this.injected = true;
			return true;
		}
		
		return false;
	}

	@Override
	public String getDisplayName()
	{
		return this.getName();
	}

	@Override
	public String getVersion()
	{
		return "Unknown";
	}

	@Override
	public float getRevision()
	{
		return 0;
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
		TweakContainer.logger.info(String.format(string, args));
	}

	private static void logWarning(String string, Object... args)
	{
		TweakContainer.logger.warning(String.format(string, args));
	}
}