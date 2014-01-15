package com.mumfrey.liteloader.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.minecraft.launchwrapper.LaunchClassLoader;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.core.exceptions.OutdatedLoaderException;
import com.mumfrey.liteloader.launch.LiteLoaderTweaker;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * The enumerator performs all mod discovery functions for LiteLoader, this includes locating mod files to load
 * as well as searching for mod classes within the class path and discovered mod files.
 *
 * @author Adam Mummery-Smith
 */
class LiteLoaderEnumerator implements PluggableEnumerator
{
	private static final String OPTION_SEARCH_MODS      = "search.mods";
	private static final String OPTION_SEARCH_JAR       = "search.jar";
	private static final String OPTION_SEARCH_CLASSPATH = "search.classpath";

	/**
	 * Maximum recursion depth for mod discovery
	 */
	private static final int MAX_DISCOVERY_DEPTH = 16;

	/**
	 * Reference to the bootstrap agent 
	 */
	private final LiteLoaderBootstrap bootstrap;

	/**
	 * Reference to the launch classloader
	 */
	private final LaunchClassLoader classLoader;

	/**
	 * 
	 */
	private final EnabledModsList enabledModsList;
	
	/**
	 * Classes to load, mapped by class name 
	 */
	private final Map<String, Class<? extends LiteMod>> modsToLoad = new HashMap<String, Class<? extends LiteMod>>();
	
	/**
	 * Mapping of mods to mod containers 
	 */
	private final Map<String, LoadableMod<?>> modContainers = new HashMap<String, LoadableMod<?>>();
	
	/**
	 * Other tweak-containing jars which we have injected 
	 */
	private final List<Loadable<File>> injectedTweaks = new ArrayList<Loadable<File>>();
	
	/**
	 * 
	 */
	private final List<EnumeratorModule<?>> modules = new ArrayList<EnumeratorModule<?>>();
	
	private boolean searchModsFolder = true;
	private boolean searchProtectionDomain = true;
	private boolean searchClassPath = true;
	
	/**
	 * @param classLoader
	 * @param enabledModsList 
	 * @param loadTweaks
	 * @param properties
	 * @param gameFolder
	 */
	public LiteLoaderEnumerator(LiteLoaderBootstrap bootstrap, LaunchClassLoader classLoader, EnabledModsList enabledModsList, boolean loadTweaks)
	{
		this.bootstrap       = bootstrap;
		this.classLoader     = classLoader;
		this.enabledModsList = enabledModsList;
		
		this.initModules(loadTweaks);
	}
	
	/**
	 * Initialise the discovery modules
	 * 
	 * @param loadTweaks 
	 */
	private void initModules(boolean loadTweaks)
	{
		// Read the discovery settings from the properties 
		this.readSettings();
		
		if (this.searchClassPath)
		{
			this.registerModule(new EnumeratorModuleClassPath(loadTweaks));
		}
		
		if (this.searchProtectionDomain)
		{
			this.registerModule(new EnumeratorModuleProtectionDomain(loadTweaks));
		}
		
		if (this.searchModsFolder)
		{
			File modsFolder = this.bootstrap.getModsFolder();
			this.registerModule(new EnumeratorModuleFolder(modsFolder, loadTweaks, false));
			
			File versionedModsFolder = this.bootstrap.getVersionedModsFolder();
			this.registerModule(new EnumeratorModuleFolder(versionedModsFolder, loadTweaks, true));
		}
		
		this.writeSettings();
	}

	/**
	 * Get the discovery settings from the properties file
	 */
	private void readSettings()
	{
		this.searchModsFolder       = this.bootstrap.getAndStoreBooleanProperty(OPTION_SEARCH_MODS,      true);
		this.searchProtectionDomain = this.bootstrap.getAndStoreBooleanProperty(OPTION_SEARCH_JAR,       true);
		this.searchClassPath        = this.bootstrap.getAndStoreBooleanProperty(OPTION_SEARCH_CLASSPATH, true);
		
		if (!this.searchModsFolder && !this.searchProtectionDomain && !this.searchClassPath)
		{
			LiteLoaderLogger.warning("Invalid configuration, no search locations defined. Enabling all search locations.");
			
			this.searchModsFolder       = true;
			this.searchProtectionDomain = true;
			this.searchClassPath        = true;
		}
	}

	/**
	 * Write settings
	 */
	private void writeSettings()
	{
		this.bootstrap.setBooleanProperty(OPTION_SEARCH_MODS,      this.searchModsFolder);
		this.bootstrap.setBooleanProperty(OPTION_SEARCH_JAR,       this.searchProtectionDomain);
		this.bootstrap.setBooleanProperty(OPTION_SEARCH_CLASSPATH, this.searchClassPath);
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.PluggableEnumerator#registerModule(com.mumfrey.liteloader.core.EnumeratorModule)
	 */
	@Override
	public void registerModule(EnumeratorModule<?> module)
	{
		if (module != null && !this.modules.contains(module))
		{
			LiteLoaderLogger.info("Registering %s: %s", module.getClass().getSimpleName(), module);
			this.modules.add(module);
			module.init(this);
		}
	}
	
	/**
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	@Override
	public boolean getAndStoreBooleanProperty(String propertyName, boolean defaultValue)
	{
		return this.bootstrap.getAndStoreBooleanProperty(propertyName, defaultValue);
	}

	/**
	 * @param propertyName
	 * @param value
	 */
	@Override
	public void setBooleanProperty(String propertyName, boolean value)
	{
		this.bootstrap.setBooleanProperty(propertyName, value);
	}

	/**
	 * Get the list of all enumerated mod classes to load
	 */
	public Collection<Class<? extends LiteMod>> getModsToLoad()
	{
		return this.modsToLoad.values();
	}
	
	/**
	 * Get the list of injected tweak containers
	 */
	public List<Loadable<File>> getInjectedTweaks()
	{
		return this.injectedTweaks;
	}

	/**
	 * Get the number of mods to load
	 */
	public int modsToLoadCount()
	{
		return this.modsToLoad.size();
	}
	
	/**
	 * @return
	 */
	public boolean hasModsToLoad()
	{
		return this.modsToLoad.size() > 0;
	}

	/**
	 * Get a metadata value for the specified mod
	 * 
	 * @param modClassName
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 */
	public String getModMetaData(Class<? extends LiteMod> modClass, String metaDataKey, String defaultValue)
	{
		return this.getModContainer(modClass).getMetaValue(metaDataKey, defaultValue);
	}
	
	/**
	 * @param mod
	 * @return
	 */
	public LoadableMod<?> getModContainer(Class<? extends LiteMod> modClass)
	{
		return this.modContainers.containsKey(modClass.getSimpleName()) ? this.modContainers.get(modClass.getSimpleName()) : LoadableMod.NONE;
	}

	/**
	 * Get the mod identifier (metadata key), this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 * @return
	 */
	public String getModIdentifier(Class<? extends LiteMod> modClass)
	{
		String modClassName = modClass.getSimpleName();
		if (!this.modContainers.containsKey(modClassName)) return null;
		return this.modContainers.get(modClassName).getIdentifier();
	}

	/**
	 * Enumerate the "mods" folder to find mod files
	 */
	protected void discoverMods()
	{
		for (EnumeratorModule<?> module : this.modules)
		{
			module.enumerate(this, this.enabledModsList, this.bootstrap.getProfile());
		}
	}
	
	/**
	 * Enumerate class path and discovered mod files to find mod classes
	 */
	protected void discoverModClasses()
	{
		try
		{
			for (EnumeratorModule<?> module : this.modules)
			{
				module.injectIntoClassLoader(this, this.classLoader);
			}

			for (EnumeratorModule<?> module : this.modules)
			{
				module.registerMods(this, this.classLoader);
			}

			LiteLoaderLogger.info("Mod class discovery completed");
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.warning(th, "Mod class discovery failed");
			return;
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.PluggableEnumerator#addTweaksFrom(com.mumfrey.liteloader.core.TweakContainer)
	 */
	@Override
	public void addTweaksFrom(TweakContainer<File> container)
	{
		if (!container.isEnabled(this.enabledModsList, this.bootstrap.getProfile()))
		{
			LiteLoaderLogger.info("Mod %s is disabled for profile %s, not injecting tranformers", container.getIdentifier(), this.bootstrap.getProfile());
			return;
		}
		
		if (container.hasTweakClass())
		{
			this.addTweakFrom(container);
		}
		
		if (container.hasClassTransformers())
		{
			this.addClassTransformersFrom(container, container.getClassTransformerClassNames());
		}
	}

	private void addTweakFrom(TweakContainer<File> container)
	{
		try
		{
			String tweakClass = container.getTweakClassName();
			int tweakPriority = container.getTweakPriority();
			LiteLoaderLogger.info("Mod file '%s' provides tweakClass '%s', adding to Launch queue with priority %d", container.getName(), tweakClass, tweakPriority);
			if (LiteLoaderTweaker.addCascadedTweaker(tweakClass, tweakPriority))
			{
				LiteLoaderLogger.info("tweakClass '%s' was successfully added", tweakClass);
				container.injectIntoClassPath(this.classLoader, true);
				
				if (container.isExternalJar())
				{
					this.injectedTweaks.add(container);
				}
				
				String[] classPathEntries = container.getClassPathEntries();
				if (classPathEntries != null)
				{
					for (String classPathEntry : classPathEntries)
					{
						try
						{
							File classPathJar = new File(this.bootstrap.getGameDirectory(), classPathEntry);
							URL classPathJarUrl = classPathJar.toURI().toURL();
							
							LiteLoaderLogger.info("Adding Class-Path entry: %s", classPathEntry); 
							LiteLoaderTweaker.addURLToParentClassLoader(classPathJarUrl);
							this.classLoader.addURL(classPathJarUrl);
						}
						catch (MalformedURLException ex) {}
					}
				}
			}
		}
		catch (MalformedURLException ex)
		{
		}
	}

	private void addClassTransformersFrom(TweakContainer<File> container, List<String> classTransformerClasses)
	{
		try
		{
			for (String classTransformerClass : classTransformerClasses)
			{
				LiteLoaderLogger.info("Mod file '%s' provides classTransformer '%s', adding to class loader", container.getName(), classTransformerClass);
				if (LiteLoaderTweaker.getTransformerManager().injectTransformer(classTransformerClass))
				{
					LiteLoaderLogger.info("classTransformer '%s' was successfully added", classTransformerClass);
					container.injectIntoClassPath(classLoader, true);
				}
			}
		}
		catch (MalformedURLException ex)
		{
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.PluggableEnumerator#addMod(java.lang.Class, com.mumfrey.liteloader.core.LoadableMod)
	 */
	@Override
	public void registerMod(Class<? extends LiteMod> mod, LoadableMod<?> container)
	{
		if (this.modsToLoad.containsKey(mod.getSimpleName()))
		{
			LiteLoaderLogger.warning("Mod name collision for mod with class '%s', maybe you have more than one copy?", mod.getSimpleName());
		}
		
		this.modsToLoad.put(mod.getSimpleName(), mod);
		if (container != null) this.modContainers.put(mod.getSimpleName(), container);
	}

	/**
	 * Enumerate classes on the classpath which are subclasses of the specified
	 * class
	 * 
	 * @param superClass
	 * @return
	 */
	static LinkedList<Class<?>> getSubclassesFor(File packagePath, ClassLoader classloader, Class<?> superClass, String prefix)
	{
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
		
		if (packagePath != null)
		{
			try
			{
				if (packagePath.isDirectory())
				{
					LiteLoaderEnumerator.enumerateDirectory(prefix, superClass, classloader, classes, packagePath);
				}
				else if (packagePath.isFile())
				{
					LiteLoaderEnumerator.enumerateCompressedPackage(prefix, superClass, classloader, classes, packagePath);
				}
			}
			catch (OutdatedLoaderException ex)
			{
				classes.clear();
				LiteLoaderLogger.info("Error searching in '%s', missing API component '%s', your loader is probably out of date", packagePath, ex.getMessage());
			}
			catch (Throwable th)
			{
				LiteLoaderLogger.warning(th, "Enumeration error");
			}
		}
		
		return classes;
	}

	/**
	 * @param superClass
	 * @param classloader
	 * @param classes
	 * @param packagePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void enumerateCompressedPackage(String prefix, Class<?> superClass, ClassLoader classloader, LinkedList<Class<?>> classes, File packagePath) throws FileNotFoundException, IOException
	{
		FileInputStream fileinputstream = new FileInputStream(packagePath);
		ZipInputStream zipinputstream = new ZipInputStream(fileinputstream);
		
		ZipEntry zipentry = null;
		
		do
		{
			zipentry = zipinputstream.getNextEntry();
			
			if (zipentry != null && zipentry.getName().endsWith(".class"))
			{
				String classFileName = zipentry.getName();
				String className = classFileName.lastIndexOf('/') > -1 ? classFileName.substring(classFileName.lastIndexOf('/') + 1) : classFileName;
				
				if (prefix == null || className.startsWith(prefix))
				{
					try
					{
						String fullClassName = classFileName.substring(0, classFileName.length() - 6).replaceAll("/", ".");
						LiteLoaderEnumerator.checkAndAddClass(classloader, superClass, classes, fullClassName);
					}
					catch (Exception ex) {}
				}
			}
		} while (zipentry != null);
		
		fileinputstream.close();
	}

	/**
	 * Recursive function to enumerate classes inside a classpath folder
	 * 
	 * @param superClass
	 * @param classloader
	 * @param classes
	 * @param packagePath
	 * @param packageName
	 * @throws OutdatedLoaderException 
	 */
	private static void enumerateDirectory(String prefix, Class<?> superClass, ClassLoader classloader, LinkedList<Class<?>> classes, File packagePath) throws OutdatedLoaderException
	{
		LiteLoaderEnumerator.enumerateDirectory(prefix, superClass, classloader, classes, packagePath, "", 0);
	}

	/**
	 * Recursive function to enumerate classes inside a classpath folder
	 * 
	 * @param superClass
	 * @param classloader
	 * @param classes
	 * @param packagePath
	 * @param packageName
	 * @throws OutdatedLoaderException 
	 */
	private static void enumerateDirectory(String prefix, Class<?> superClass, ClassLoader classloader, LinkedList<Class<?>> classes, File packagePath, String packageName, int depth) throws OutdatedLoaderException
	{
		// Prevent crash due to broken recursion
		if (depth > MAX_DISCOVERY_DEPTH)
			return;
		
		File[] classFiles = packagePath.listFiles();
		
		for (File classFile : classFiles)
		{
			if (classFile.isDirectory())
			{
				LiteLoaderEnumerator.enumerateDirectory(prefix, superClass, classloader, classes, classFile, packageName + classFile.getName() + ".", depth + 1);
			}
			else
			{
				if (classFile.getName().endsWith(".class") && (prefix == null || classFile.getName().startsWith(prefix)))
				{
					String classFileName = classFile.getName();
					String className = packageName + classFileName.substring(0, classFileName.length() - 6);
					LiteLoaderEnumerator.checkAndAddClass(classloader, superClass, classes, className);
				}
			}
		}
	}

	/**
	 * @param classloader
	 * @param superClass
	 * @param classes
	 * @param className
	 * @throws OutdatedLoaderException 
	 */
	private static void checkAndAddClass(ClassLoader classloader, Class<?> superClass, LinkedList<Class<?>> classes, String className) throws OutdatedLoaderException
	{
		if (className.indexOf('$') > -1)
			return;
		
		try
		{
			Class<?> subClass = classloader.loadClass(className);
			
			if (subClass != null && !superClass.equals(subClass) && superClass.isAssignableFrom(subClass) && !subClass.isInterface() && !classes.contains(subClass))
			{
				classes.add(subClass);
			}
		}
		catch (Throwable th)
		{
			String missingClassName = th.getCause().getMessage();
			if (th.getCause() instanceof NoClassDefFoundError && missingClassName != null)
			{
				if (missingClassName.startsWith("com/mumfrey/liteloader/"))
				{
					throw new OutdatedLoaderException(missingClassName.substring(missingClassName.lastIndexOf('/') + 1));
				}
			}
			
			LiteLoaderLogger.warning(th, "checkAndAddClass error");
		}
	}
}
