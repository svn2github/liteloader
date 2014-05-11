package com.mumfrey.liteloader.core;

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.activity.InvalidActivityException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReport;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.world.World;

import org.apache.logging.log4j.Logger;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.api.CoreProvider;
import com.mumfrey.liteloader.api.LiteAPI;
import com.mumfrey.liteloader.api.PostRenderObserver;
import com.mumfrey.liteloader.api.ShutdownObserver;
import com.mumfrey.liteloader.api.TickObserver;
import com.mumfrey.liteloader.api.WorldObserver;
import com.mumfrey.liteloader.api.manager.APIAdapter;
import com.mumfrey.liteloader.api.manager.APIProvider;
import com.mumfrey.liteloader.core.overlays.IMinecraft;
import com.mumfrey.liteloader.crashreport.CallableLaunchWrapper;
import com.mumfrey.liteloader.crashreport.CallableLiteLoaderBrand;
import com.mumfrey.liteloader.crashreport.CallableLiteLoaderMods;
import com.mumfrey.liteloader.gui.startup.LoadingBar;
import com.mumfrey.liteloader.interfaces.Loadable;
import com.mumfrey.liteloader.interfaces.LoadableMod;
import com.mumfrey.liteloader.interfaces.LoaderEnumerator;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.modconfig.ConfigManager;
import com.mumfrey.liteloader.modconfig.Exposable;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;
import com.mumfrey.liteloader.util.Input;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * LiteLoader is a simple loader which loads and provides useful callbacks to
 * lightweight mods
 * 
 * @author Adam Mummery-Smith
 */
public final class LiteLoader
{
	/**
	 * LiteLoader is a singleton, this is the singleton instance
	 */
	private static LiteLoader instance;
	
	/**
	 * Logger for LiteLoader events
	 */
	private static final Logger logger = LiteLoaderLogger.getLogger();
	
	/**
	 * Tweak system class loader 
	 */
	private static LaunchClassLoader classLoader;
	
	/**
	 * Reference to the Minecraft game instance
	 */
	private Minecraft minecraft;
	
	/**
	 * Loader environment instance 
	 */
	private final LoaderEnvironment environment;
	
	/**
	 * Loader Properties adapter 
	 */
	private final LoaderProperties properties;
	
	/**
	 * Mod enumerator instance
	 */
	private final LoaderEnumerator enumerator;
	
	/**
	 * Registered resource packs 
	 */
	private final Map<String, IResourcePack> registeredResourcePacks = new HashMap<String, IResourcePack>();

	/**
	 * Mods
	 */
	protected final LiteLoaderMods mods;
	
	/**
	 * API Provider instance 
	 */
	private final APIProvider apiProvider;
	
	/**
	 * API Adapter instance
	 */
	private final APIAdapter apiAdapter;
	
	/**
	 * Core providers
	 */
	private final List<CoreProvider> coreProviders = new LinkedList<CoreProvider>();
	
	/**
	 * 
	 */
	private final List<TickObserver> tickObservers = new LinkedList<TickObserver>();
	
	/**
	 * 
	 */
	private final List<WorldObserver> worldObservers = new LinkedList<WorldObserver>();
	
	/**
	 * 
	 */
	private final List<ShutdownObserver> shutdownObservers = new LinkedList<ShutdownObserver>();
	
	/**
	 * 
	 */
	private final List<PostRenderObserver> postRenderObservers = new LinkedList<PostRenderObserver>();
	
	protected final LiteLoaderModPanelManager modPanelManager;
	
	/**
	 * Interface Manager
	 */
	private LiteLoaderInterfaceManager interfaceManager;
	
	/**
	 * Event manager
	 */
	private Events events;

	/**
	 * Plugin channel manager 
	 */
	private final ClientPluginChannels clientPluginChannels = new ClientPluginChannels();
	
	/**
	 * Server channel manager 
	 */
	private final ServerPluginChannels serverPluginChannels = new ServerPluginChannels();
	
	/**
	 * Permission Manager
	 */
	private final PermissionsManagerClient permissionsManager = PermissionsManagerClient.getInstance();
	
	/**
	 * Mod configuration manager
	 */
	private final ConfigManager configManager;
	
	/**
	 * Flag which keeps track of whether late initialisation has completed
	 */
	private boolean modInitComplete;

	/**
	 * True while initialising mods if we need to do a resource manager reload once the process is completed
	 */
	private boolean pendingResourceReload;
	
	/**
	 * 
	 */
	private Input input;
	
	/**
	 * LiteLoader constructor
	 * @param profile 
	 * @param modNameFilter 
	 */
	private LiteLoader(LoaderEnvironment environment, LoaderProperties properties)
	{
		this.environment = environment;
		this.properties = properties;
		this.enumerator = environment.getEnumerator();
		
		this.configManager = new ConfigManager();
		this.input = new Input(new File(environment.getCommonConfigFolder(), "liteloader.keys.properties"));

		this.mods = new LiteLoaderMods(this, environment, properties, this.configManager);
		
		this.apiProvider = environment.getAPIProvider();
		this.apiAdapter = environment.getAPIAdapter();
		
		this.modPanelManager = new LiteLoaderModPanelManager(environment, properties, this.mods, this.configManager);
	}
	
	/**
	 * Set up reflection methods required by the loader
	 */
	private void onInit()
	{
		try
		{
			this.coreProviders.addAll(this.apiAdapter.getCoreProviders());
			this.tickObservers.addAll(this.apiAdapter.getAllObservers(TickObserver.class));
			this.worldObservers.addAll(this.apiAdapter.getAllObservers(WorldObserver.class));
			this.shutdownObservers.addAll(this.apiAdapter.getAllObservers(ShutdownObserver.class));
			this.postRenderObservers.addAll(this.apiAdapter.getAllObservers(PostRenderObserver.class));
			
			for (CoreProvider coreProvider : this.coreProviders)
			{
				coreProvider.onInit();
			}
			
			this.enumerator.onInit();
			this.mods.init();
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.severe("Error initialising LiteLoader", th);
		}
	}
	
	/**
	 * 
	 */
	private void onPostInit()
	{
		this.initCoreObjects();
		
		for (CoreProvider coreProvider : this.coreProviders)
		{
			coreProvider.onPostInit(this.minecraft);
		}

		this.interfaceManager.registerInterfaces();
		
		// Spawn mod instances and initialise them
		this.loadAndInitMods();
		
		LoadingBar.setMessage("LiteLoader POSTINIT...");
		
		// Initialises the required hooks for loaded mods
		this.interfaceManager.onPostInit();
		
		this.modInitComplete = true;
		this.mods.onPostInit();

		for (CoreProvider coreProvider : this.coreProviders)
		{
			coreProvider.onPostInitComplete(this.mods);
		}
		
		// Save stuff
		this.properties.writeProperties();
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.ICustomResourcePackManager#registerModResourcePack(net.minecraft.client.resources.ResourcePack)
	 */
	public boolean registerModResourcePack(IResourcePack resourcePack)
	{
		if (!this.registeredResourcePacks.containsKey(resourcePack.getPackName()))
		{
			this.pendingResourceReload = true;

			List<IResourcePack> defaultResourcePacks = ((IMinecraft)this.minecraft).getDefaultResourcePacks();
			if (!defaultResourcePacks.contains(resourcePack))
			{
				defaultResourcePacks.add(resourcePack);
				this.registeredResourcePacks.put(resourcePack.getPackName(), resourcePack);
				return true;
			}
		}
		
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.ICustomResourcePackManager#unRegisterModResourcePack(net.minecraft.client.resources.ResourcePack)
	 */
	public boolean unRegisterModResourcePack(IResourcePack resourcePack)
	{
		if (this.registeredResourcePacks.containsValue(resourcePack))
		{
			this.pendingResourceReload = true;

			List<IResourcePack> defaultResourcePacks = ((IMinecraft)this.minecraft).getDefaultResourcePacks();
			this.registeredResourcePacks.remove(resourcePack.getPackName());
			defaultResourcePacks.remove(resourcePack);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Get the singleton instance of LiteLoader, initialises the loader if
	 * necessary
	 * 
	 * @param locationProvider
	 * @return LiteLoader instance
	 */
	public static final LiteLoader getInstance()
	{
		return LiteLoader.instance;
	}
	
	/**
	 * Get the LiteLoader logger object
	 * 
	 * @return
	 * @deprecated use LiteLoaderLogger instead
	 */
	@Deprecated
	public static final Logger getLogger()
	{
		return LiteLoader.logger;
	}
	
	/**
	 * Get the tweak system classloader
	 * 
	 * @return
	 */
	public static LaunchClassLoader getClassLoader()
	{
		return LiteLoader.classLoader;
	}
	
	/**
	 * Get the output stream which we are using for console output
	 * 
	 * @return System.err
	 * @deprecated use log4j instead
	 */
	@Deprecated
	public static final PrintStream getConsoleStream()
	{
		return System.err;
	}
	
	/**
	 * Get LiteLoader version
	 * 
	 * @return
	 */
	public static final String getVersion()
	{
		return LiteLoaderVersion.CURRENT.getLoaderVersion();
	}
	
	/**
	 * Get LiteLoader version
	 * 
	 * @return
	 */
	public static final String getVersionDisplayString()
	{
		return String.format("LiteLoader %s", LiteLoaderVersion.CURRENT.getLoaderVersion());
	}
	
	/**
	 * Get the loader revision
	 * 
	 * @return
	 */
	public static final int getRevision()
	{
		return LiteLoaderVersion.CURRENT.getLoaderRevision();
	}
	
	/**
	 * @return
	 */
	public static final LiteAPI[] getAPIs()
	{
		LiteAPI[] apis = LiteLoader.instance.apiProvider.getAPIs();
		LiteAPI[] apisCopy = new LiteAPI[apis.length];
		System.arraycopy(apis, 0, apisCopy, 0, apis.length);
		return apisCopy;
	}
	
	/**
	 * @param identifier
	 * @return
	 */
	public static final LiteAPI getAPI(String identifier)
	{
		return LiteLoader.instance.apiProvider.getAPI(identifier);
	}
	
	/**
	 * @param identifier
	 * @return
	 */
	public static boolean isAPIAvailable(String identifier)
	{
		return LiteLoader.getAPI(identifier) != null;
	}
	
	/**
	 * @return
	 */
	public static PermissionsManagerClient getPermissionsManager()
	{
		return LiteLoader.instance.permissionsManager;
	}
	
	/**
	 * @return
	 */
	public static LiteLoaderInterfaceManager getInterfaceManager()
	{
		return LiteLoader.instance.interfaceManager;
	}
	
	/**
	 * Get the event manager
	 * 
	 * @return
	 */
	public static Events getEvents()
	{
		return LiteLoader.instance.events;
	}
	
	/**
	 * Get the plugin channel manager
	 * 
	 * @return
	 * @deprecated use LiteLoader.getClientPluginChannels()
	 */
	@Deprecated
	public static ClientPluginChannels getPluginChannels()
	{
		return LiteLoader.instance.clientPluginChannels;
	}

	/**
	 * Get the client-side plugin channel manager
	 * 
	 * @return
	 */
	public static ClientPluginChannels getClientPluginChannels()
	{
		return LiteLoader.instance.clientPluginChannels;
	}
	
	/**
	 * Get the server-side plugin channel manager
	 * 
	 * @return
	 */
	public static ServerPluginChannels getServerPluginChannels()
	{
		return LiteLoader.instance.serverPluginChannels;
	}
	
	/**
	 * Get the input manager
	 */
	public static Input getInput()
	{
		return LiteLoader.instance.input;
	}
	
	/**
	 * Get the mod panel manager
	 * 
	 * @return
	 */
	public static LiteLoaderModPanelManager getModPanelManager()
	{
		return LiteLoader.instance.modPanelManager;
	}
	
	/**
	 * Get the "mods" folder
	 */
	public static File getModsFolder()
	{
		return LiteLoader.instance.environment.getModsFolder();
	}
	
	/**
	 * Get the common (version-independent) config folder
	 */
	public static File getCommonConfigFolder()
	{
		return LiteLoader.instance.environment.getCommonConfigFolder();
	}
	
	/**
	 * Get the config folder for this version
	 */
	public static File getConfigFolder()
	{
		return LiteLoader.instance.environment.getVersionedConfigFolder();
	}
	
	/**
	 * @return
	 */
	public static File getGameDirectory()
	{
		return LiteLoader.instance.environment.getGameDirectory();
	}
	
	/**
	 * @return
	 */
	public static File getAssetsDirectory()
	{
		return LiteLoader.instance.environment.getAssetsDirectory();
	}
	
	/**
	 * @return
	 */
	public static String getProfile()
	{
		return LiteLoader.instance.environment.getProfile();
	}
	
	/**
	 * Used to get the name of the modpack being used
	 * 
	 * @return name of the modpack in use or null if no pack
	 */
	public static String getBranding()
	{
		return LiteLoader.instance.properties.getBranding();
	}
	
	/**
	 * @return
	 */
	public static boolean isDevelopmentEnvironment()
	{
		return "true".equals(System.getProperty("mcpenv"));
	}
	
	/**
	 * Used for crash reporting, returns a text list of all loaded mods
	 * 
	 * @return List of loaded mods as a string
	 */
	public String getLoadedModsList()
	{
		return this.mods.getLoadedModsList();
	}
	
	/**
	 * Get a list containing all loaded mods
	 */
	public List<LiteMod> getLoadedMods()
	{
		return this.mods.getLoadedMods();
	}
	
	/**
	 * Get a list containing all mod files which were NOT loaded
	 */
	public List<Loadable<?>> getDisabledMods()
	{
		return this.mods.getDisabledMods();
	}
	
	/**
	 * Get the list of injected tweak containers
	 */
	public Collection<Loadable<File>> getInjectedTweaks()
	{
		return this.mods.getInjectedTweaks();
	}

	/**
	 * Get a reference to a loaded mod, if the mod exists
	 * 
	 * @param modName Mod's name, identifier or class name
	 * @return
	 * @throws InvalidActivityException
	 */
	public <T extends LiteMod> T getMod(String modName) throws InvalidActivityException, IllegalArgumentException
	{
		if (!this.modInitComplete)
		{
			throw new InvalidActivityException("Attempted to get a reference to a mod before loader startup is complete");
		}
		
		return this.mods.getMod(modName);
	}
	
	/**
	 * Get a reference to a loaded mod, if the mod exists
	 * 
	 * @param modName Mod's name or class name
	 * @return
	 * @throws InvalidActivityException
	 */
	public <T extends LiteMod> T getMod(Class<T> modClass)
	{
		if (!this.modInitComplete)
		{
			throw new RuntimeException("Attempted to get a reference to a mod before loader startup is complete");
		}
		
		return this.mods.getMod(modClass);
	}
	
	/**
	 * Get whether the specified mod is installed
	 *
	 * @param modName
	 * @return
	 */
	public boolean isModInstalled(String modName)
	{
		if (!this.modInitComplete || modName == null) return false;
		
		return this.mods.isModInstalled(modName);
	}

	/**
	 * Get a metadata value for the specified mod
	 * 
	 * @param modNameOrId
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 * @throws InvalidActivityException Thrown by getMod if init is not complete 
	 * @throws IllegalArgumentException Thrown by getMod if argument is null
	 */
	public String getModMetaData(String modNameOrId, String metaDataKey, String defaultValue) throws InvalidActivityException, IllegalArgumentException
	{
		return this.mods.getModMetaData(modNameOrId, metaDataKey, defaultValue);
	}
	
	/**
	 * Get a metadata value for the specified mod
	 * 
	 * @param mod
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 */
	public String getModMetaData(LiteMod mod, String metaDataKey, String defaultValue)
	{
		return this.mods.getModMetaData(mod, metaDataKey, defaultValue);
	}

	/**
	 * Get a metadata value for the specified mod
	 * 
	 * @param modClass
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 */
	public String getModMetaData(Class<? extends LiteMod> modClass, String metaDataKey, String defaultValue)
	{
		return this.mods.getModMetaData(modClass, metaDataKey, defaultValue);
	}

	/**
	 * Get the mod identifier, this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 * @return
	 */
	public String getModIdentifier(Class<? extends LiteMod> modClass)
	{
		return this.mods.getModIdentifier(modClass);
	}
	
	/**
	 * Get the mod identifier, this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 * @return
	 */
	public String getModIdentifier(LiteMod mod)
	{
		return this.mods.getModIdentifier(mod);
	}
	
	/**
	 * Get the container (mod file, classpath jar or folder) for the specified mod
	 * 
	 * @param modClass
	 * @return
	 */
	public LoadableMod<?> getModContainer(Class<? extends LiteMod> modClass)
	{
		return this.mods.getModContainer(modClass);
	}
	
	/**
	 * Get the container (mod file, classpath jar or folder) for the specified mod
	 * 
	 * @param modClass
	 * @return
	 */
	public LoadableMod<?> getModContainer(LiteMod mod)
	{
		return this.mods.getModContainer(mod);
	}
	
	/**
	 * Get the mod which matches the specified identifier
	 * 
	 * @param identifier
	 * @return
	 */
	public Class<? extends LiteMod> getModFromIdentifier(String identifier)
	{
		return this.mods.getModFromIdentifier(identifier);
	}
	
	/**
	 * @param identifier Identifier of the mod to enable
	 */
	public void enableMod(String identifier)
	{
		this.mods.setModEnabled(identifier, true);
	}

	/**
	 * @param identifier Identifier of the mod to disable
	 */
	public void disableMod(String identifier)
	{
		this.mods.setModEnabled(identifier, false);
	}
	
	/**
	 * @param identifier Identifier of the mod to enable/disable
	 * @param enabled
	 */
	public void setModEnabled(String identifier, boolean enabled)
	{
		this.mods.setModEnabled(identifier, enabled);
	}

	/**
	 * @param modName
	 * @return
	 */
	public boolean isModEnabled(String modName)
	{
		return this.mods.isModEnabled(modName);
	}
	
	/**
	 * @param modName
	 * @return
	 */
	public boolean isModActive(String modName)
	{
		return this.mods.isModActive(modName);
	}
	
	/**
	 * @param exposable
	 */
	public void writeConfig(Exposable exposable)
	{
		this.configManager.invalidateConfig(exposable);
	}
	
	/**
	 * Register an arbitrary Exposable
	 * 
	 * @param exposable Exposable object to register
	 * @param fileName Override config file name to use (leave null to use value from ExposableConfig specified value)
	 */
	public void registerExposable(Exposable exposable, String fileName)
	{
		this.configManager.registerExposable(exposable, fileName, true);
		this.configManager.initConfig(exposable);
	}

	/**
	 * 
	 */
	private void initCoreObjects()
	{
		// Cache local minecraft reference
		this.minecraft = Minecraft.getMinecraft();
		
		// Create the interface manager
		this.interfaceManager = new LiteLoaderInterfaceManager(this.apiAdapter);
		
		// Create the event broker
		this.events = new Events(this, this.minecraft, this.properties);
		
		// Put the minecraft reference into the mod panel manager
		this.modPanelManager.setMinecraft(this.minecraft);
	}

	private void loadAndInitMods()
	{
		int totalMods = this.enumerator.modsToLoadCount();
		LiteLoaderLogger.info("Discovered %d total mod(s)", totalMods);
		
		if (totalMods > 0)
		{
			this.mods.loadMods();
			this.mods.initMods();
		}
		else
		{
			LiteLoaderLogger.info("Mod class discovery failed or no mod classes were found. Not loading any mods.");
		}
	}

	void onPostInitMod(LiteMod mod)
	{
		// add mod to permissions manager if permissible
		this.permissionsManager.registerMod(mod);
	}

	/**
	 * Called before mod late initialisation, refresh the resources that have been added so that mods can use them
	 */
	void refreshResources(boolean force)
	{
		if (this.pendingResourceReload || force)
		{
			LoadingBar.setMessage("Reloading Resources...");
			this.pendingResourceReload = false;
			this.minecraft.refreshResources();
		}
	}
	
	/**
	 * Called after mod late init
	 */
	void onStartupComplete()
	{
		for (CoreProvider coreProvider : this.coreProviders)
		{
			coreProvider.onStartupComplete();
		}
	}

	/**
	 * Called on login
	 * 
	 * @param netHandler
	 * @param loginPacket
	 */
	void onJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
		this.permissionsManager.onJoinGame(netHandler, loginPacket);

		for (CoreProvider coreProvider : this.coreProviders)
		{
			coreProvider.onJoinGame(netHandler, loginPacket);
		}
	}
	
	/**
	 * Called when the world reference is changed
	 * 
	 * @param world
	 */
	void onWorldChanged(World world)
	{
		if (world != null)
		{
			// For bungeecord
			this.permissionsManager.scheduleRefresh();
		}
		
		for (WorldObserver worldObserver : this.worldObservers)
		{
			worldObserver.onWorldChanged(world);
		}
	}
	
	/**
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 */
	void onPostRender(int mouseX, int mouseY, float partialTicks)
	{
		this.minecraft.mcProfiler.startSection("core");
		
		for (PostRenderObserver postRenderObserver : this.postRenderObservers)
		{
			postRenderObserver.onPostRender(mouseX, mouseY, partialTicks);
		}
		
		this.minecraft.mcProfiler.endSection();
	}

	/**
	 * @param clock
	 * @param partialTicks
	 * @param inGame
	 */
	void onTick(boolean clock, float partialTicks, boolean inGame)
	{
		if (clock)
		{
			// Tick the permissions manager
			this.minecraft.mcProfiler.startSection("permissionsmanager");
			this.permissionsManager.onTick(this.minecraft, partialTicks, inGame);
			
			// Tick the config manager
			this.minecraft.mcProfiler.endStartSection("configmanager");
			this.configManager.onTick();
			
			this.minecraft.mcProfiler.endSection();
			
			if (!((IMinecraft)this.minecraft).isRunning())
			{
				this.onShutDown();
				return;
			}
		}

		this.minecraft.mcProfiler.startSection("observers");
		
		for (TickObserver tickObserver : this.tickObservers)
		{
			tickObserver.onTick(clock, partialTicks, inGame);
		}
		
		this.minecraft.mcProfiler.endSection();
	}

	private void onShutDown()
	{
		LiteLoaderLogger.info("LiteLoader is shutting down, shutting down core providers and syncing configuration");
		
		for (ShutdownObserver lifeCycleObserver : this.shutdownObservers)
		{
			lifeCycleObserver.onShutDown();
		}

		this.configManager.syncConfig();
	}

	/**
	 * Register a key for a mod
	 * 
	 * @param binding
	 * @deprecated Deprecated : use LiteLoader.getInput().registerKeyBinding() instead
	 */
	@Deprecated
	public void registerModKey(KeyBinding binding)
	{
		this.input.registerKeyBinding(binding);
	}
	
	/**
	 * Set the "mod info" screen tab to hidden, regardless of the property setting
	 * 
	 * @deprecated use getModPanelManager().hideModInfoScreenTab(); instead
	 */
	@Deprecated
	public void hideModInfoScreenTab()
	{
		this.modPanelManager.hideTab();
	}
	
	/**
	 * Set whether the "mod info" screen tab should be shown in the main menu
	 * 
	 * @deprecated use getModPanelManager().setDisplayModInfoScreenTab(show); instead
	 */
	@Deprecated
	public void setDisplayModInfoScreenTab(boolean show)
	{
		this.modPanelManager.setTabVisible(show);
	}
	
	/**
	 * Get whether the "mod info" screen tab is shown in the main menu
	 * 
	 * @deprecated use getModPanelManager().getDisplayModInfoScreenTab(); instead
	 */
	@Deprecated
	public boolean getDisplayModInfoScreenTab()
	{
		return this.modPanelManager.isTabVisible();
	}

	/**
	 * Display the "mod info" overlay over the specified GUI
	 * 
	 * @param parentScreen
	 * 
	 * @deprecated use getModPanelManager().displayModInfoScreen(parentScreen); instead
	 */
	@Deprecated
	public void displayModInfoScreen(GuiScreen parentScreen)
	{
		this.modPanelManager.displayModInfoScreen(parentScreen);
	}

	/**
	 * @param objCrashReport This is an object so that we don't need to transform the obfuscated name in the transformer
	 */
	public static void populateCrashReport(Object objCrashReport)
	{
		if (objCrashReport instanceof CrashReport)
		{
			CrashReport crashReport = (CrashReport)objCrashReport;
			crashReport.getCategory().addCrashSectionCallable("Mod Pack",        new CallableLiteLoaderBrand(crashReport));
			crashReport.getCategory().addCrashSectionCallable("LiteLoader Mods", new CallableLiteLoaderMods(crashReport));
			crashReport.getCategory().addCrashSectionCallable("LaunchWrapper",   new CallableLaunchWrapper(crashReport));
		}
	}

	static final void createInstance(LoaderEnvironment environment, LoaderProperties properties, LaunchClassLoader classLoader)
	{
		if (LiteLoader.instance == null)
		{
			LiteLoader.classLoader = classLoader;
			LiteLoader.instance = new LiteLoader(environment, properties);
		}
	}
	
	static final void init()
	{
		LiteLoaderLogger.info("LiteLoader begin INIT...");
		
		LiteLoader.instance.onInit();
	}

	static final void postInit()
	{
		LiteLoaderLogger.info("LiteLoader begin POSTINIT...");

		LiteLoader.instance.onPostInit();
	}
}