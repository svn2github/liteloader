package com.mumfrey.liteloader.core;

import java.util.List;

import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;

import com.mumfrey.liteloader.util.PrivateFields;

/**
 * Manager object which handles inhibiting the sound handler's reload notification at startup
 *
 * @author Adam Mummery-Smith
 */
public class SoundHandlerReloadInhibitor
{
	/**
	 * Resource Manager
	 */
	private SimpleReloadableResourceManager resourceManager;
	
	/**
	 * Sound manager
	 */
	private SoundHandler soundHandler;
	
	/**
	 * True if inhibition is currently active
	 */
	private boolean inhibited;
	
	/**
	 * So that we can re-insert the sound manager at the same index, we store the index we remove it from
	 */
	private int storedIndex;
	
	SoundHandlerReloadInhibitor(SimpleReloadableResourceManager resourceManager, SoundHandler soundHandler)
	{
		this.resourceManager = resourceManager;
		this.soundHandler = soundHandler;
	}
	
	/**
	 * Inhibit the sound manager reload notification
	 * 
	 * @return true if inhibit was applied
	 */
	public boolean inhibit()
	{
		try
		{
			if (!this.inhibited)
			{
				List<IResourceManagerReloadListener> reloadListeners = PrivateFields.reloadListeners.get(this.resourceManager);
				if (reloadListeners != null)
				{
					this.storedIndex = reloadListeners.indexOf(this.soundHandler);
					if (this.storedIndex > -1)
					{
						LiteLoader.getLogger().info("Inhibiting sound handler reload");
						reloadListeners.remove(this.soundHandler);
						this.inhibited = true;
						return true;
					}
				}
			}
		}
		catch (Exception ex)
		{
			LiteLoader.getLogger().warning("Error inhibiting sound handler reload");
		}
		
		return false;
	}
	
	/**
	 * Remove the sound manager reload inhibit
	 * 
	 * @param reload True to reload the sound manager now
	 * @return true if the sound manager was successfully restored
	 */
	public boolean unInhibit(boolean reload)
	{
		try
		{
			if (this.inhibited)
			{
				List<IResourceManagerReloadListener> reloadListeners = PrivateFields.reloadListeners.get(this.resourceManager);
				if (reloadListeners != null)
				{
					if (this.storedIndex > -1)
					{
						reloadListeners.add(this.storedIndex, this.soundHandler);
					}
					else
					{
						reloadListeners.add(this.soundHandler);
					}

					LiteLoader.getLogger().info("Sound handler reload inhibit removed");
					
					if (reload)
					{
						LiteLoader.getLogger().info("Reloading sound handler");
						this.soundHandler.onResourceManagerReload(this.resourceManager);
					}

					this.inhibited = false;
					return true;
				}
			}
		}
		catch (Exception ex)
		{
			LiteLoader.getLogger().warning("Error removing sound handler reload inhibit");
		}
		
		return false;
	}

	public boolean isInhibited()
	{
		return this.inhibited;
	}
}
