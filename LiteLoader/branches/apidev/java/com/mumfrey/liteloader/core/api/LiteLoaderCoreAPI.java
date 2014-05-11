package com.mumfrey.liteloader.core.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mumfrey.liteloader.api.BrandingProvider;
import com.mumfrey.liteloader.api.CoreProvider;
import com.mumfrey.liteloader.api.EnumeratorModule;
import com.mumfrey.liteloader.api.InterfaceProvider;
import com.mumfrey.liteloader.api.LiteAPI;
import com.mumfrey.liteloader.api.Observer;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.LiteLoaderCoreProvider;
import com.mumfrey.liteloader.core.LiteLoaderVersion;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * LiteLoader's API impl.
 * 
 * @author Adam Mummery-Smith
 */
public class LiteLoaderCoreAPI implements LiteAPI
{
	private static final String OPTION_SEARCH_MODS      = "search.mods";
	private static final String OPTION_SEARCH_JAR       = "search.jar";
	private static final String OPTION_SEARCH_CLASSPATH = "search.classpath";

	private static final String PKG_LITELOADER          = "com.mumfrey.liteloader";
	private static final String PKG_LITELOADER_CORE     = LiteLoaderCoreAPI.PKG_LITELOADER + ".core";

	private static final String[] requiredTransformers = {
		LiteLoaderCoreAPI.PKG_LITELOADER + ".launch.LiteLoaderTransformer",
		LiteLoaderCoreAPI.PKG_LITELOADER_CORE + ".transformers.CrashReportTransformer"
	};
	
	private static final String[] requiredDownstreamTransformers = {
		LiteLoaderCoreAPI.PKG_LITELOADER_CORE + ".transformers.LiteLoaderCallbackInjectionTransformer",
		LiteLoaderCoreAPI.PKG_LITELOADER_CORE + ".transformers.MinecraftOverlayTransformer"
	};
	
	private static final String[] defaultPacketTransformers = {
		LiteLoaderCoreAPI.PKG_LITELOADER_CORE + ".transformers.LoginSuccessPacketTransformer",
		LiteLoaderCoreAPI.PKG_LITELOADER_CORE + ".transformers.ChatPacketTransformer",
		LiteLoaderCoreAPI.PKG_LITELOADER_CORE + ".transformers.JoinGamePacketTransformer",
		LiteLoaderCoreAPI.PKG_LITELOADER_CORE + ".transformers.CustomPayloadPacketTransformer",
		LiteLoaderCoreAPI.PKG_LITELOADER_CORE + ".transformers.ServerChatPacketTransformer",
		LiteLoaderCoreAPI.PKG_LITELOADER_CORE + ".transformers.ServerCustomPayloadPacketTransformer"
	};
	
	private LoaderEnvironment environment;
	
	private LoaderProperties properties;
	
	private boolean searchClassPath;
	private boolean searchProtectionDomain;
	private boolean searchModsFolder;
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getIdentifier()
	 */
	@Override
	public String getIdentifier()
	{
		return "liteloader";
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getName()
	 */
	@Override
	public String getName()
	{
		return "LiteLoader core API";
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getVersion()
	 */
	@Override
	public String getVersion()
	{
		return LiteLoaderVersion.CURRENT.getLoaderVersion();
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getRevision()
	 */
	@Override
	public int getRevision()
	{
		return LiteLoaderVersion.CURRENT.getLoaderRevision();
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getModClassPrefix()
	 */
	@Override
	public String getModClassPrefix()
	{
		return "LiteMod";
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getBrandingProvider()
	 */
	@Override
	public BrandingProvider getBrandingProvider()
	{
		return new LiteLoaderBrandingProvider();
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#init(com.mumfrey.liteloader.launch.LoaderEnvironment, com.mumfrey.liteloader.launch.LoaderProperties)
	 */
	@Override
	public void init(LoaderEnvironment environment, LoaderProperties properties)
	{
		this.environment = environment;
		this.properties = properties;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getRequiredTransformers()
	 */
	@Override
	public String[] getRequiredTransformers()
	{
		return LiteLoaderCoreAPI.requiredTransformers;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getRequiredDownstreamTransformers()
	 */
	@Override
	public String[] getRequiredDownstreamTransformers()
	{
		return LiteLoaderCoreAPI.requiredDownstreamTransformers;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getPacketTransformers()
	 */
	@Override
	public String[] getPacketTransformers()
	{
		return LiteLoaderCoreAPI.defaultPacketTransformers;
	}
	
	/**
	 * Get the discovery settings from the properties file
	 */
	void readDiscoverySettings()
	{
		this.searchModsFolder       = this.properties.getAndStoreBooleanProperty(LiteLoaderCoreAPI.OPTION_SEARCH_MODS,      true);
		this.searchProtectionDomain = this.properties.getAndStoreBooleanProperty(LiteLoaderCoreAPI.OPTION_SEARCH_JAR,       false);
		this.searchClassPath        = this.properties.getAndStoreBooleanProperty(LiteLoaderCoreAPI.OPTION_SEARCH_CLASSPATH, true);
		
		if (!this.searchModsFolder && !this.searchProtectionDomain && !this.searchClassPath)
		{
			LiteLoaderLogger.warning("Invalid configuration, no search locations defined. Enabling all search locations.");
			
			this.searchModsFolder = true;
			this.searchClassPath  = true;
		}
	}

	/**
	 * Write settings
	 */
	void writeDiscoverySettings()
	{
		this.properties.setBooleanProperty(LiteLoaderCoreAPI.OPTION_SEARCH_MODS,      this.searchModsFolder);
		this.properties.setBooleanProperty(LiteLoaderCoreAPI.OPTION_SEARCH_JAR,       this.searchProtectionDomain);
		this.properties.setBooleanProperty(LiteLoaderCoreAPI.OPTION_SEARCH_CLASSPATH, this.searchClassPath);
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getEnumeratorModules()
	 */
	@Override
	public List<EnumeratorModule> getEnumeratorModules()
	{
		this.readDiscoverySettings();
		
		List<EnumeratorModule> enumeratorModules = new ArrayList<EnumeratorModule>();
		
		if (this.searchClassPath)
		{
			enumeratorModules.add(new EnumeratorModuleClassPath());
		}
		
		if (this.searchProtectionDomain)
		{
			LiteLoaderLogger.info("Protection domain searching is no longer required or supported, protection domain search has been disabled");
			this.searchProtectionDomain = false;
		}
		
		if (this.searchModsFolder)
		{
			File modsFolder = this.environment.getModsFolder();
			enumeratorModules.add(new EnumeratorModuleFolder(this, modsFolder, true));
			
			File versionedModsFolder = this.environment.getVersionedModsFolder();
			enumeratorModules.add(new EnumeratorModuleFolder(this, versionedModsFolder, false));
		}
		
		return Collections.unmodifiableList(enumeratorModules);
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getCoreProviders()
	 */
	@Override
	public List<CoreProvider> getCoreProviders()
	{
		return ImmutableList.<CoreProvider>of
		(
			new LiteLoaderCoreProvider(this.properties),
			LiteLoader.getInput()
		);
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getInterfaceProviders()
	 */
	@Override
	public List<InterfaceProvider> getInterfaceProviders()
	{
		return ImmutableList.<InterfaceProvider>of
		(
			LiteLoader.getEvents(),
			LiteLoader.getClientPluginChannels(),
			LiteLoader.getServerPluginChannels()
		);
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getObservers()
	 */
	@Override
	public List<Observer> getObservers()
	{
		return ImmutableList.<Observer>of
		(
			LiteLoader.getModPanelManager()
		);
	}
}
