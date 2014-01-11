package com.mumfrey.liteloader.core;

import java.io.File;
import java.util.LinkedList;
import java.util.logging.Logger;

import net.minecraft.launchwrapper.LaunchClassLoader;

import com.mumfrey.liteloader.LiteMod;

/**
 * Enumerator module which searches for mods on the classpath
 * 
 * @author Adam Mummery-Smith
 */
public class EnumeratorModuleClassPath implements EnumeratorModule<File>
{
	/**
	 * Local logger reference
	 */
	private static Logger logger = Logger.getLogger("liteloader");

	/**
	 * Array of class path entries specified to the JVM instance 
	 */
	private final String[] classPathEntries;
	
	private boolean loadTweaks;

	/**
	 * @param parent
	 * @param searchProtectionDomain
	 * @param searchClassPath
	 * @param loadTweaks
	 */
	public EnumeratorModuleClassPath(boolean loadTweaks)
	{
		// Read the JVM class path into the local array
		this.classPathEntries = this.readClassPath();
		
		this.loadTweaks = loadTweaks;
	}
	
	@Override
	public String toString()
	{
		return "<Java Class Path>";
	}
	
	@Override
	public void init(PluggableEnumerator enumerator)
	{
	}

	@Override
	public void writeSettings(PluggableEnumerator enumerator)
	{
	}

	/**
	 * Reads the class path entries that were supplied to the JVM and returns them as an array
	 */
	private String[] readClassPath()
	{
		EnumeratorModuleClassPath.logInfo("Enumerating class path...");
		
		String classPath = System.getProperty("java.class.path");
		String classPathSeparator = System.getProperty("path.separator");
		String[] classPathEntries = classPath.split(classPathSeparator);
		
		EnumeratorModuleClassPath.logInfo("Class path separator=\"%s\"", classPathSeparator);
		EnumeratorModuleClassPath.logInfo("Class path entries=(\n   classpathEntry=%s\n)", classPath.replace(classPathSeparator, "\n   classpathEntry="));
		return classPathEntries;
	}

	@Override
	public void enumerate(PluggableEnumerator enumerator, EnabledModsList enabledModsList, String profile)
	{
		if (this.loadTweaks)
		{
			EnumeratorModuleClassPath.logInfo("Discovering tweaks on class path...");
			
			for (String classPathPart : this.classPathEntries)
			{
				File packagePath = new File(classPathPart);
				if (packagePath.exists())
				{
					LoadableModClassPath classPathMod = new LoadableModClassPath(packagePath, null);
					
					if (classPathMod.hasTweakClass() || classPathMod.hasClassTransformers())
					{
						enumerator.addTweaksFrom(classPathMod);
					}
				}
			}
		}
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
		EnumeratorModuleClassPath.logInfo("Discovering mods on class path...");
		
		for (String classPathPart : this.classPathEntries)
		{
			EnumeratorModuleClassPath.logInfo("Searching %s...", classPathPart);
			
			File packagePath = new File(classPathPart);
			LinkedList<Class<?>> modClasses = LiteLoaderEnumerator.getSubclassesFor(packagePath, classLoader, LiteMod.class, PluggableEnumerator.MOD_CLASS_PREFIX);
			
			for (Class<?> mod : modClasses)
			{
				LoadableModClassPath container = new LoadableModClassPath(packagePath, mod.getSimpleName().substring(7).toLowerCase());
				enumerator.registerMod((Class<? extends LiteMod>)mod, container);
			}
			
			if (modClasses.size() > 0)
				EnumeratorModuleClassPath.logInfo("Found %s potential matches", modClasses.size());
		}
	}

	private static void logInfo(String string, Object... args)
	{
		EnumeratorModuleClassPath.logger.info(String.format(string, args));
	}
}
