package com.mumfrey.liteloader.core;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.logging.Logger;

import net.minecraft.launchwrapper.LaunchClassLoader;

import com.mumfrey.liteloader.LiteMod;

/**
 * Enumerator module which searches for mods on the classpath
 * 
 * @author Adam Mummery-Smith
 */
public class EnumeratorModuleProtectionDomain implements EnumeratorModule<File>
{
	/**
	 * Local logger reference
	 */
	private static Logger logger = Logger.getLogger("liteloader");

	private File packagePath;

	/**
	 * @param parent
	 * @param searchProtectionDomain
	 * @param searchClassPath
	 * @param loadTweaks
	 */
	public EnumeratorModuleProtectionDomain(boolean loadTweaks)
	{
		this.initPackagePath();
	}
	
	@Override
	public String toString()
	{
		return this.packagePath.getAbsolutePath();
	}
	
	private void initPackagePath()
	{
		try
		{
			this.packagePath = null;
			
			URL protectionDomainLocation = EnumeratorModuleProtectionDomain.class.getProtectionDomain().getCodeSource().getLocation();
			if (protectionDomainLocation != null)
			{
				if (protectionDomainLocation.toString().indexOf('!') > -1 && protectionDomainLocation.toString().startsWith("jar:"))
				{
					protectionDomainLocation = new URL(protectionDomainLocation.toString().substring(4, protectionDomainLocation.toString().indexOf('!')));
				}
				
				this.packagePath = new File(protectionDomainLocation.toURI());
			}
			else
			{
				// Fix (?) for forge and other mods which mangle the protection domain
				String reflectionClassPath = EnumeratorModuleProtectionDomain.class.getResource("/com/mumfrey/liteloader/core/EnumeratorModuleProtectionDomain.class").getPath();
				
				if (reflectionClassPath.indexOf('!') > -1)
				{
					reflectionClassPath = URLDecoder.decode(reflectionClassPath, "UTF-8");
					this.packagePath = new File(reflectionClassPath.substring(5, reflectionClassPath.indexOf('!')));
				}
			}
		}
		catch (Throwable th)
		{
			EnumeratorModuleProtectionDomain.logWarning("Error determining local protection domain: %s", th.getMessage());
		}
	}
	
	@Override
	public void init(PluggableEnumerator enumerator)
	{
	}

	@Override
	public void writeSettings(PluggableEnumerator enumerator)
	{
	}

	@Override
	public void enumerate(PluggableEnumerator enumerator, EnabledModsList enabledModsList, String profile)
	{
	}
	
	@Override
	public void injectIntoClassLoader(PluggableEnumerator enumerator, LaunchClassLoader classLoader)
	{
	}

	/**
	 * @param classLoader
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void registerMods(PluggableEnumerator enumerator, LaunchClassLoader classLoader)
	{
		EnumeratorModuleProtectionDomain.logInfo("Searching protection domain code source...");

		try
		{
			if (this.packagePath != null)
			{
				LinkedList<Class<?>> modClasses = LiteLoaderEnumerator.getSubclassesFor(this.packagePath, classLoader, LiteMod.class, PluggableEnumerator.MOD_CLASS_PREFIX);
				
				for (Class<?> mod : modClasses)
				{
					enumerator.registerMod((Class<? extends LiteMod>)mod, null);
				}
				
				if (modClasses.size() > 0)
					EnumeratorModuleProtectionDomain.logInfo("Found %s potential matches", modClasses.size());
			}
		}
		catch (Throwable th)
		{
			EnumeratorModuleProtectionDomain.logWarning("Error loading from local class path: %s", th.getMessage());
		}
	}

	private static void logInfo(String string, Object... args)
	{
		EnumeratorModuleProtectionDomain.logger.info(String.format(string, args));
	}

	private static void logWarning(String string, Object... args)
	{
		EnumeratorModuleProtectionDomain.logger.warning(String.format(string, args));
	}
}
