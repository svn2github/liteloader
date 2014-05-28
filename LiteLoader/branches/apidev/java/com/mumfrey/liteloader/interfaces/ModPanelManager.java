package com.mumfrey.liteloader.interfaces;

import com.mumfrey.liteloader.api.PostRenderObserver;
import com.mumfrey.liteloader.api.TickObserver;
import com.mumfrey.liteloader.core.LiteLoaderMods;
import com.mumfrey.liteloader.modconfig.ConfigManager;

/**
 * Interface for the mod info panel manager, abstracted because we don't have the class GuiScreen on the server
 * 
 * @author Adam Mummery-Smith
 *
 * @param <TGuiScreen> GuiScreen class, must be generic because we don't have GuiScreen on the server side
 */
public interface ModPanelManager<TGuiScreen> extends TickObserver, PostRenderObserver
{
	/**
	 * @param mods
	 * @param configManager
	 */
	public abstract void init(LiteLoaderMods mods, ConfigManager configManager);

	/**
	 * 
	 */
	public abstract void hideTab();

	/**
	 * @param show
	 */
	public abstract void setTabVisible(boolean show);

	/**
	 * @return
	 */
	public abstract boolean isTabVisible();

	/**
	 * @param parentScreen
	 */
	public abstract void displayModInfoScreen(TGuiScreen parentScreen);
}
