package com.mumfrey.liteloader.launch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.mumfrey.liteloader.core.transformers.PacketTransformer;
import com.mumfrey.liteloader.util.SortableValue;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderTweaker implements ITweaker
{
	public static final String VERSION = "1.7.2";
	
	private static LiteLoaderTweaker instance;
	
	private static Logger logger = Logger.getLogger("liteloader");
	
	private static final String OPTION_GENERATE_MAPPINGS = "genMappings";

	private boolean preInit = true;
	
	private boolean init = true;
	
	private File gameDirectory;
	
	private File assetsDirectory;
	
	private File jarFile;
	
	private String profile;
	
	private List<String> modsToLoad;

	private ILoaderBootstrap bootstrap;
	
	private URL jarUrl;
	
	private List<String> singularLaunchArgs = new ArrayList<String>();
	
	private Map<String, String> launchArgs;
	
	private ArgumentAcceptingOptionSpec<String> jarOption;
	private ArgumentAcceptingOptionSpec<String> modsOption;
	private OptionSet parsedOptions;
	
	private List<String> passThroughArgs;
	
	private Set<String> injectTransformers = new HashSet<String>();
	
	private Map<String, TreeSet<SortableValue<String>>> packetTransformers = new HashMap<String, TreeSet<SortableValue<String>>>();
	
	private int tweakOrder = 0;
	
	private Set<String> allTweaks = new HashSet<String>();
	
	private Set<SortableValue<String>> sortedTweaks = new TreeSet<SortableValue<String>>();

	private boolean isPrimary;
	
	private static final String[] requiredTransformers = {
		"com.mumfrey.liteloader.launch.LiteLoaderTransformer",
		"com.mumfrey.liteloader.core.transformers.CrashReportTransformer"
	};
	
	private static final String injectionTransformerClassName = "com.mumfrey.liteloader.core.transformers.CallbackInjectionTransformer";
	
	private static final String genTransformerClassName = "com.mumfrey.liteloader.core.gen.GenProfilerTransformer";

	private static final String[] defaultPacketTransformers = {
		"com.mumfrey.liteloader.core.transformers.LoginSuccessPacketTransformer",
		"com.mumfrey.liteloader.core.transformers.ChatPacketTransformer",
		"com.mumfrey.liteloader.core.transformers.JoinGamePacketTransformer",
		"com.mumfrey.liteloader.core.transformers.CustomPayloadPacketTransformer"
	};
	
	@SuppressWarnings("unchecked")
	@Override
	public void acceptOptions(List<String> args, File gameDirectory, File assetsDirectory, String profile)
	{
		LiteLoaderTweaker.instance = this;
		
		LiteLoaderTweaker.instance.gameDirectory = gameDirectory;
		LiteLoaderTweaker.instance.assetsDirectory = assetsDirectory;
		LiteLoaderTweaker.instance.profile = profile;
		
		OptionParser optionParser = new OptionParser();
		this.jarOption = optionParser.accepts("versionJar", "Minecraft version jar to use").withRequiredArg().ofType(String.class);
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
		
		this.initJarUrl();
		
		// Parse out the arguments ourself because joptsimple doesn't really provide a good way to
		// add arguments to the unparsed argument list after parsing
		this.parseArgs(this.passThroughArgs);
		
		// Put required arguments to the blackboard if they don't already exist there
		this.provideRequiredArgs(gameDirectory, assetsDirectory);
		
		if (this.parsedOptions.has(this.modsOption))
		{
			LiteLoaderTweaker.instance.modsToLoad = this.modsOption.values(this.parsedOptions);
		}
		
		this.injectTransformers.addAll(Arrays.asList(LiteLoaderTweaker.defaultPacketTransformers));
		
		if (this.jarFile != null)
		{
			LiteLoaderTweaker.logger.info(String.format("Injecting version jar '%s'", this.jarFile.getAbsolutePath()));
			Launch.classLoader.addURL(this.jarUrl);
			LiteLoaderTweaker.addURLToParentClassLoader(this.jarUrl);
		}
		
		this.preInit();
	}

	/**
	 * 
	 */
	protected void initJarUrl()
	{
		if (this.parsedOptions.has(this.jarOption))
		{
			try
			{
				String jarPath = this.jarOption.value(this.parsedOptions);
				if (jarPath.matches("^[0-9\\.]+$")) jarPath = String.format("versions/%1$s/%1$s.jar", jarPath);
				LiteLoaderTweaker.logger.info(String.format("Version jar '%s' was specified on the command line", jarPath));
				this.jarFile = new File(jarPath);
				this.jarUrl = this.jarFile.toURI().toURL();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		else
		{
			String resource = "/jarfile.ref";
			InputStream refResource = LiteLoaderTweaker.class.getResourceAsStream(resource);
			File refContainer = ClassPathUtilities.getPathToResource(LiteLoaderTweaker.class, resource);
			if (refResource != null && refContainer != null)
			{
				InputStreamReader refReader = new InputStreamReader(refResource);
				
				try
				{
					@SuppressWarnings("unchecked")
					Map<String, String> refMap = new Gson().fromJson(refReader, HashMap.class);
					if (refMap.containsKey("jarfile"))
					{
						String jarPath = refMap.get("jarfile");
						LiteLoaderTweaker.logger.info(String.format("Version jar '%s' specified via jarfile.ref", jarPath));
						this.jarFile = new File(refContainer.getParentFile(), jarPath);
						this.jarUrl = this.jarFile.toURI().toURL();
						return;
					}
				}
				catch (IOException ex) {}
				finally
				{
					try
					{
						refReader.close();
						refResource.close();
					}
					catch (IOException ex) {}
				}
			}
				
			URL[] urls = Launch.classLoader.getURLs();
			this.jarUrl = urls[urls.length - 1]; // probably?
		}
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
				{
					this.addClassifiedArg(classifier, "");
					classifier = null;
				}
				else if (arg.contains("="))
				{
					this.addClassifiedArg(arg.substring(0, arg.indexOf('=')), arg.substring(arg.indexOf('=') + 1));
				}
				else
				{
					classifier = arg;
				}
			}
			else
			{
				if (classifier != null)
				{
					this.addClassifiedArg(classifier, arg);
					classifier = null;
				}
				else
					this.singularLaunchArgs.add(arg);
			}
		}
	}

	private void addClassifiedArg(String classifiedArg, String arg)
	{
		this.launchArgs.put(classifiedArg, arg);
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader)
	{
		classLoader.addClassLoaderExclusion("com.mumfrey.liteloader.core.runtime.");

		this.sieveAndSortPacketTransformers(classLoader, this.injectTransformers);
		
		for (String requiredTransformerClassName : LiteLoaderTweaker.requiredTransformers)
		{
			LiteLoaderTweaker.logger.info(String.format("Injecting required class transformer '%s'", requiredTransformerClassName));
			classLoader.registerTransformer(requiredTransformerClassName);
		}
		
		for (Entry<String, TreeSet<SortableValue<String>>> packetClassTransformers : this.packetTransformers.entrySet())
		{
			for (SortableValue<String> transformerInfo : packetClassTransformers.getValue())
			{
				String packetClass = packetClassTransformers.getKey();
				if (packetClass.lastIndexOf('.') != -1) packetClass = packetClass.substring(packetClass.lastIndexOf('.') + 1);
				LiteLoaderTweaker.logger.info(String.format("Injecting packet class transformer '%s' for packet class '%s' with priority %d", transformerInfo.getValue(), packetClass, transformerInfo.getPriority()));
				classLoader.registerTransformer(transformerInfo.getValue());
			}
		}

		if (LiteLoaderTweaker.instance.bootstrap.getBooleanProperty(OPTION_GENERATE_MAPPINGS))
		{
			LiteLoaderTweaker.logger.info(String.format("Injecting gen trasnformer '%s'", LiteLoaderTweaker.genTransformerClassName));
			LiteLoaderTweaker.instance.injectTransformers.add(LiteLoaderTweaker.genTransformerClassName);
		}
		
		LiteLoaderTweaker.logger.info(String.format("Injecting required class transformer '%s'", LiteLoaderTweaker.injectionTransformerClassName));
		LiteLoaderTweaker.instance.injectTransformers.add(LiteLoaderTweaker.injectionTransformerClassName);
	}

	private void injectModTransformers()
	{
		if (LiteLoaderTweaker.instance.injectTransformers.size() > 0)
			LiteLoaderTweaker.logger.info("Injecting downstream transformers");

		for (String transformerClassName : LiteLoaderTweaker.instance.injectTransformers)
		{
			LiteLoaderTweaker.logger.info(String.format("Injecting additional class transformer class '%s'", transformerClassName));
			Launch.classLoader.registerTransformer(transformerClassName);
		}
		
		LiteLoaderTweaker.instance.injectTransformers.clear();
	}
	
	@SuppressWarnings("unchecked")
	private void sieveAndSortPacketTransformers(LaunchClassLoader classLoader, Set<String> transformers)
	{
		LiteLoaderTweaker.logger.info("Sorting registered packet transformers by priority");
		int registeredTransformers = 0;
		
		NonDelegatingClassLoader tempLoader = new NonDelegatingClassLoader(classLoader.getURLs(), this.getClass().getClassLoader());
		tempLoader.addDelegatedClassName("com.mumfrey.liteloader.core.transformers.PacketTransformer");
		tempLoader.addDelegatedClassName("com.mumfrey.liteloader.core.runtime.Obf");
		tempLoader.addDelegatedClassName("net.minecraft.launchwrapper.IClassTransformer");
		tempLoader.addDelegatedPackage("org.objectweb.asm.");

		Iterator<String> iter = transformers.iterator();
		while (iter.hasNext())
		{
			String transformerClassName = iter.next();
			try
			{
				Class<IClassTransformer> transformerClass = (Class<IClassTransformer>)tempLoader.addAndLoadClass(transformerClassName);
				
				if (PacketTransformer.class.isAssignableFrom(transformerClass))
				{
					PacketTransformer transformer = (PacketTransformer)transformerClass.newInstance();
					String packetClass = transformer.getPacketClass();
					if (!this.packetTransformers.containsKey(packetClass))
						this.packetTransformers.put(packetClass, new TreeSet<SortableValue<String>>());
					this.packetTransformers.get(packetClass).add(transformer.getInfo(transformerClassName));
					registeredTransformers++;
					iter.remove();
				}
			}
			catch (NoClassDefFoundError err)
			{
				if (err.getCause() instanceof InvalidTransformerException)
				{
					InvalidTransformerException ex = (InvalidTransformerException)err.getCause();
					LiteLoaderTweaker.logger.warning(String.format("Packet transformer class '%s' references class '%s' which is not allowed. Packet transformers must not contain references to other classes", transformerClassName, ex.getAccessedClass())); 
					ex.printStackTrace();
					iter.remove();
				}
				else
				{
					throw err;
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		
		LiteLoaderTweaker.logger.info(String.format("Added %d packet transformer classes to the transformer list", registeredTransformers));
	}

	@Override
	public String getLaunchTarget()
	{
		this.isPrimary = true;
		LiteLoaderTweaker.preBeginGame();
		
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
	
	public static boolean addTweaker(String tweakClass, int priority)
	{
		return LiteLoaderTweaker.instance.addTweakToSortedList(tweakClass, priority);
	}

	private boolean addTweakToSortedList(String tweakClass, int priority)
	{
		if (tweakClass != null && !this.allTweaks.contains(tweakClass))
		{
			this.allTweaks.add(tweakClass);
			this.sortedTweaks.add(new SortableValue<String>(priority, this.tweakOrder++, tweakClass));
			return true;
		}
		
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private void injectDiscoveredTweakClasses()
	{
		if (!LiteLoaderTweaker.instance.preInit)
		{
			LiteLoaderTweaker.logger.warning("Failed to inject cascaded tweak classes because preInit is already complete");
			return;
		}
		
		if (this.sortedTweaks.size() > 0)
		{
			LiteLoaderTweaker.logger.info("Injecting cascaded tweakers...");

			List<String> tweakClasses = (List<String>)Launch.blackboard.get("TweakClasses");
			List<ITweaker> tweakers = (List<ITweaker>)Launch.blackboard.get("Tweaks");
			if (tweakClasses != null && tweakers != null)
			{
				for (SortableValue<String> tweak : this.sortedTweaks)
				{
					String tweakClass = tweak.getValue();
					LiteLoaderTweaker.logger.info(String.format("Injecting tweak class %s with priority %d", tweakClass, tweak.getPriority()));
					this.injectTweakClass(tweakClass, tweakClasses, tweakers);
				}
			}
			
			// Clear sortedTweaks but not allTweaks
			this.sortedTweaks.clear();
		}
	}

	/**
	 * @param tweakClass
	 * @param tweakClasses
	 * @param tweakers
	 */
	private void injectTweakClass(String tweakClass, List<String> tweakClasses, List<ITweaker> tweakers)
	{
		if (!tweakClasses.contains(tweakClass))
		{
			for (ITweaker existingTweaker : tweakers)
			{
				if (tweakClass.equals(existingTweaker.getClass().getName()))
					return;
			}
			
			tweakClasses.add(tweakClass);
		}
	}

	public static boolean addClassTransformer(String transfomerClass)
	{
		if (!LiteLoaderTweaker.instance.preInit)
		{
			LiteLoaderTweaker.logger.warning(String.format("Failed to add transformer class %s because preInit is already complete", transfomerClass));
			return false;
		}

		LiteLoaderTweaker.instance.injectTransformers.add(transfomerClass);
		return true;
	}
	
	/**
	 * @param url URL to add
	 */
	public static boolean addURLToParentClassLoader(URL url)
	{
		if (LiteLoaderTweaker.instance.preInit)
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

	@SuppressWarnings("unchecked")
	private static void spawnBootstrap() throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Class<? extends ILoaderBootstrap> bootstrapClass = (Class<? extends ILoaderBootstrap>)Class.forName("com.mumfrey.liteloader.core.LiteLoaderBootstrap", false, Launch.classLoader);
		Constructor<? extends ILoaderBootstrap> bootstrapCtor = bootstrapClass.getDeclaredConstructor(File.class, File.class, String.class);
		bootstrapCtor.setAccessible(true);
		
		LiteLoaderTweaker.instance.bootstrap = bootstrapCtor.newInstance(LiteLoaderTweaker.instance.gameDirectory, LiteLoaderTweaker.instance.assetsDirectory, LiteLoaderTweaker.instance.profile);
	}

	/**
	 * Do the first stage of loader startup, which enumerates mod sources and finds tweakers
	 */
	private void preInit()
	{
		try
		{
			LiteLoaderTweaker.logger.info("Bootstrapping LiteLoader " + LiteLoaderTweaker.VERSION);
			LiteLoaderTweaker.spawnBootstrap();
			LiteLoaderTweaker.logger.info("Beginning LiteLoader PreInit...");
			this.bootstrap.preInit(Launch.classLoader, true, this.modsToLoad);
			
			this.injectDiscoveredTweakClasses();
			
			this.preInit = false;
		}
		catch (Throwable th)
		{
			LiteLoaderTweaker.logger.log(Level.SEVERE, String.format("Error during LiteLoader PreInit: %s", th.getMessage()), th);
		}
	}
	
	public static void preBeginGame()
	{
		LiteLoaderTweaker.instance.injectModTransformers();
	}

	/**
	 * Do the second stage of loader startup
	 */
	public static void init()
	{
		if (LiteLoaderTweaker.instance.preInit) throw new IllegalStateException("Attempt to perform LiteLoader Init but PreInit was not completed");
		LiteLoaderTweaker.instance.init = true;
		
		try
		{
			LiteLoaderTweaker.instance.bootstrap.init(Launch.classLoader);
			LiteLoaderTweaker.instance.init = false;
		}
		catch (Throwable th)
		{
			LiteLoaderTweaker.logger.log(Level.SEVERE, String.format("Error during LiteLoader Init: %s", th.getMessage()), th);
		}
	}
	
	/**
	 * Do the second stage of loader startup
	 */
	public static void postInit()
	{
		if (LiteLoaderTweaker.instance.init) throw new IllegalStateException("Attempt to perform LiteLoader PostInit but Init was not completed");

		try
		{
			LiteLoaderTweaker.instance.bootstrap.postInit();
		}
		catch (Throwable th)
		{
			LiteLoaderTweaker.logger.log(Level.SEVERE, String.format("Error during LiteLoader PostInit: %s", th.getMessage()), th);
		}
	}
	
	public static URL getJarUrl()
	{
		return LiteLoaderTweaker.instance.jarUrl;
	}
	
	public static boolean isPrimary()
	{
		return LiteLoaderTweaker.instance.isPrimary;
	}
}