package com.mumfrey.liteloader.launch;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderTweaker implements ITweaker
{
	public static final String VERSION = "1.6.4";
	
	private static Logger logger = Logger.getLogger("liteloader");
	
	private static boolean preInit = true;
	
	private static File gameDirectory;
	
	private static File assetsDirectory;
	
	private static String profile;
	
	private static List<String> modsToLoad;

	private List<String> singularLaunchArgs = new ArrayList<String>();
	
	private Map<String, String> launchArgs;
	
	private ArgumentAcceptingOptionSpec<String> modsOption;
	private OptionSet parsedOptions;
	
	private List<String> passThroughArgs;
	
	@SuppressWarnings("unchecked")
	@Override
	public void acceptOptions(List<String> args, File gameDirectory, File assetsDirectory, String profile)
	{
		LiteLoaderTweaker.gameDirectory = gameDirectory;
		LiteLoaderTweaker.assetsDirectory = assetsDirectory;
		LiteLoaderTweaker.profile = profile;
		
		OptionParser optionParser = new OptionParser();
		this.modsOption = optionParser.accepts("mods", "Comma-separated list of mods to load").withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		optionParser.allowsUnrecognizedOptions();
		NonOptionArgumentSpec<String> nonOptions = optionParser.nonOptions();
		
		this.parsedOptions = optionParser.parse(args.toArray(new String[args.size()]));
		this.passThroughArgs = this.parsedOptions.valuesOf(nonOptions);
		
		this.launchArgs = (Map<String, String>)Launch.blackboard.get("launchArgs");
		if (this.launchArgs == null)
		{
			this.launchArgs = new HashMap<String, String>();			
			Launch.blackboard.put("launchArgs", this.launchArgs);
		}
		
		// Parse out the arguments ourself because joptsimple doesn't really provide a good way to
		// add arguments to the unparsed argument list after parsing
		this.parseArgs(this.passThroughArgs);
		
		// Put required arguments to the blackboard if they don't already exist there
		this.provideRequiredArgs(gameDirectory, assetsDirectory);
		
		if (this.parsedOptions.has(this.modsOption))
		{
			LiteLoaderTweaker.modsToLoad = this.modsOption.values(this.parsedOptions);
		}
		
//		LiteLoaderTweaker.preInitLoader(); // for future version with tweak support
	}

	/**
	 * @param gameDirectory
	 * @param assetsDirectory
	 */
	public void provideRequiredArgs(File gameDirectory, File assetsDirectory)
	{
		if (!this.launchArgs.containsKey("--version"))
			this.addClassifiedArg("--version", LiteLoaderTweaker.VERSION);
		
		if (!this.launchArgs.containsKey("--gameDir") && gameDirectory != null)
			this.addClassifiedArg("--gameDir", gameDirectory.getAbsolutePath());
		
		if (!this.launchArgs.containsKey("--assetsDir") && assetsDirectory != null)
			this.addClassifiedArg("--assetsDir", assetsDirectory.getAbsolutePath());
	}

	private void parseArgs(List<String> args)
	{
		String classifier = null;
		
		for (String arg : args)
		{
			if (arg.startsWith("-"))
			{
				if (classifier != null)
					classifier = this.addClassifiedArg(classifier, "");
				else if (arg.contains("="))
					classifier = this.addClassifiedArg(arg.substring(0, arg.indexOf('=')), arg.substring(arg.indexOf('=') + 1));
				else
					classifier = arg;
			}
			else
			{
				if (classifier != null)
					classifier = this.addClassifiedArg(classifier, arg);
				else
					this.singularLaunchArgs.add(arg);
			}
		}
	}

	private String addClassifiedArg(String classifiedArg, String arg)
	{
		this.launchArgs.put(classifiedArg, arg);
		return null;
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader)
	{
		LiteLoaderTweaker.logger.info("Injecting LiteLoader Class Transformer");
		classLoader.registerTransformer(LiteLoaderTransformer.class.getName());
	}

	@Override
	public String getLaunchTarget()
	{
		return "net.minecraft.client.main.Main";
	}

	@Override
	public String[] getLaunchArguments()
	{
		List<String> args = new ArrayList<String>();
		
		for (String singularArg : this.singularLaunchArgs)
			args.add(singularArg);
		
		for (Entry<String, String> launchArg : this.launchArgs.entrySet())
		{
			args.add(launchArg.getKey().trim());
			args.add(launchArg.getValue().trim());
		}
		
		this.singularLaunchArgs.clear();
		this.launchArgs.clear();
		
		return args.toArray(new String[args.size()]);
	}

	public File getGameDirectory()
	{
		return LiteLoaderTweaker.gameDirectory;
	}

	public File getAssetsDirectory()
	{
		return LiteLoaderTweaker.assetsDirectory;
	}
	
	public String getProfile()
	{
		return LiteLoaderTweaker.profile;
	}
	
	public static boolean addTweaker(URL tweakSource, String tweakClass)
	{
		if (LiteLoaderTweaker.preInit)
		{
			@SuppressWarnings("unchecked")
			List<String> tweakers = (List<String>)Launch.blackboard.get("TweakClasses");
			if (tweakers != null)
			{
				if (LiteLoaderTweaker.addURLToParentClassLoader(tweakSource))
				{
					tweakers.add(tweakClass);
					return true;
				}
			}
		}
		else
		{
			LiteLoaderTweaker.logger.warning(String.format("Failed to add tweak class %s from %s because preInit is already complete", tweakClass, tweakSource));
		}
		
		return false;
	}
	
	/**
	 * Do the first stage of loader startup, which enumerates mod sources and finds tweakers
	 */
	protected static void preInitLoader()
	{
		if (!LiteLoaderTweaker.preInit) throw new IllegalStateException("Attempt to perform LiteLoader PreInit but PreInit was already completed");
		LiteLoaderTweaker.logger.info("Beginning LiteLoader PreInit...");
		
		try
		{
			Class<?> loaderClass = Class.forName("com.mumfrey.liteloader.core.LiteLoader", false, Launch.classLoader);
			Method mPreInit = loaderClass.getDeclaredMethod("preInit", File.class, File.class, String.class, List.class, LaunchClassLoader.class, Boolean.TYPE);
			mPreInit.setAccessible(true);
			mPreInit.invoke(null, LiteLoaderTweaker.gameDirectory, LiteLoaderTweaker.assetsDirectory, LiteLoaderTweaker.profile, LiteLoaderTweaker.modsToLoad, Launch.classLoader, false);

			LiteLoaderTweaker.preInit = false;
		}
		catch (Throwable th)
		{
			LiteLoaderTweaker.logger.log(Level.SEVERE, String.format("Error during LiteLoader PreInit: %s", th.getMessage()), th);
		}
	}
	
	/**
	 * Do the second stage of loader startup
	 */
	protected static void postInitLoader()
	{
		if (LiteLoaderTweaker.preInit) throw new IllegalStateException("Attempt to perform LiteLoader PostInit but PreInit was not completed");
		LiteLoaderTweaker.logger.info("Beginning LiteLoader PostInit...");
		
		try
		{
			Class<?> loaderClass = Class.forName("com.mumfrey.liteloader.core.LiteLoader", false, Launch.classLoader);
			Method mPostInit = loaderClass.getDeclaredMethod("postInit");
			mPostInit.setAccessible(true);
			mPostInit.invoke(null);
		}
		catch (Throwable th)
		{
			th.printStackTrace(System.out);
			LiteLoaderTweaker.logger.log(Level.SEVERE, String.format("Error during LiteLoader PostInit: %s", th.getMessage()), th);
		}
	}

	/**
	 * @param url URL to add
	 */
	public static boolean addURLToParentClassLoader(URL url)
	{
		if (LiteLoaderTweaker.preInit)
		{
			try
			{
				URLClassLoader classLoader = (URLClassLoader)Launch.class.getClassLoader();
				Method mAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
				mAddUrl.setAccessible(true);
				mAddUrl.invoke(classLoader, url);
				
				return true;
			}
			catch (Exception ex)
			{
				LiteLoaderTweaker.logger.log(Level.WARNING, String.format("addURLToParentClassLoader failed: %s", ex.getMessage()), ex);
			}
		}
			
		return false;
	}
}