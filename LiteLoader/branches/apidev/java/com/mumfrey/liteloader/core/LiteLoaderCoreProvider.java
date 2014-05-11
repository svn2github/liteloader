package com.mumfrey.liteloader.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.world.World;

import com.mumfrey.liteloader.api.CoreProvider;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.resources.InternalResourcePack;

/**
 * CoreProvider which fixes SoundManager derping up at startup
 * 
 * @author Adam Mummery-Smith
 */
public class LiteLoaderCoreProvider implements CoreProvider
{
	private static final String OPTION_SOUND_MANAGER_FIX = "soundManagerFix";
	
	/**
	 * Loader Properties adapter 
	 */
	private final LoaderProperties properties;
	
	/**
	 * Read from the properties file, if true we will inhibit the sound manager reload during startup to avoid getting in trouble with OpenAL
	 */
	private boolean inhibitSoundManagerReload = true;
	
	/**
	 * If inhibit is enabled, this object is used to reflectively inhibit the sound manager's reload process during startup by removing it from the reloadables list
	 */
	private SoundHandlerReloadInhibitor soundHandlerReloadInhibitor;
	
	public LiteLoaderCoreProvider(LoaderProperties properties)
	{
		this.properties = properties;
	}

	@Override
	public void onInit()
	{
		this.inhibitSoundManagerReload = this.properties.getAndStoreBooleanProperty(LiteLoaderCoreProvider.OPTION_SOUND_MANAGER_FIX, true);
	}
	
	@Override
	public void onPostInit(Minecraft minecraft)
	{
		this.soundHandlerReloadInhibitor = new SoundHandlerReloadInhibitor((SimpleReloadableResourceManager)minecraft.getResourceManager(), minecraft.getSoundHandler());
		
		if (this.inhibitSoundManagerReload)
		{
			this.soundHandlerReloadInhibitor.inhibit();
		}

		// Add self as a resource pack for texture/lang resources
		LiteLoader.getInstance().registerModResourcePack(new InternalResourcePack("LiteLoader", LiteLoader.class, "liteloader"));
	}
	
	@Override
	public void onPostInitComplete(LiteLoaderMods mods)
	{
	}
	
	@Override
	public void onStartupComplete()
	{
		// Set the loader branding in ClientBrandRetriever using reflection
		LiteLoaderBootstrap.setBranding("LiteLoader");
		
		if (this.soundHandlerReloadInhibitor != null && this.soundHandlerReloadInhibitor.isInhibited())
		{
			this.soundHandlerReloadInhibitor.unInhibit(true);
		}
	}
	
	@Override
	public void onJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
	}
	
	@Override
	public void onPostRender(int mouseX, int mouseY, float partialTicks)
	{
	}
	
	@Override
	public void onTick(boolean clock, float partialTicks, boolean inGame)
	{
	}
	
	@Override
	public void onWorldChanged(World world)
	{
	}
	
	@Override
	public void onShutDown()
	{
	}
}
