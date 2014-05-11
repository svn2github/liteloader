package com.mumfrey.liteloader.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;

import com.mumfrey.liteloader.api.PostRenderObserver;
import com.mumfrey.liteloader.api.TickObserver;
import com.mumfrey.liteloader.gui.GuiScreenModInfo;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.modconfig.ConfigManager;

/**
 * Observer which handles the display of the mod panel
 * 
 * @author Adam Mummery-Smith
 */
public class LiteLoaderModPanelManager implements TickObserver, PostRenderObserver
{
	private static final String OPTION_MOD_INFO_SCREEN = "modInfoScreen";

	private final LoaderEnvironment environment;
	
	/**
	 * Loader Properties adapter 
	 */
	private final LoaderProperties properties;

	private final LiteLoaderMods mods;

	private final ConfigManager configManager;

	private Minecraft minecraft;

	/**
	 * Setting which determines whether we show the "mod info" screen tab in the main menu
	 */
	private boolean displayModInfoScreenTab = true;
	
	/**
	 * Override for the "mod info" tab setting, so that mods which want to handle the mod info themselves
	 * can temporarily disable the function without having to change the underlying property
	 */
	private boolean hideModInfoScreenTab = false;
	
	/**
	 * Active "mod info" screen, drawn as an overlay when in the main menu and made the active screen if
	 * the user clicks the tab
	 */
	private GuiScreenModInfo modInfoScreen;
	
	/**
	 * @param environment
	 * @param properties
	 * @param mods
	 * @param configManager
	 */
	public LiteLoaderModPanelManager(LoaderEnvironment environment, LoaderProperties properties, LiteLoaderMods mods, ConfigManager configManager)
	{
		this.environment   = environment;
		this.properties    = properties;
		this.mods          = mods;
		this.configManager = configManager;
		
		this.displayModInfoScreenTab = this.properties.getAndStoreBooleanProperty(LiteLoaderModPanelManager.OPTION_MOD_INFO_SCREEN, true);
	}
	
	/**
	 * @param minecraft
	 */
	void setMinecraft(Minecraft minecraft)
	{
		this.minecraft = minecraft;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.TickObserver#onTick(boolean, float, boolean)
	 */
	@Override
	public void onTick(boolean clock, float partialTicks, boolean inGame)
	{
		if (clock && this.modInfoScreen != null && this.minecraft.currentScreen != this.modInfoScreen)
		{
			this.modInfoScreen.updateScreen();
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.PostRenderObserver#onPostRender(int, int, float)
	 */
	@Override
	public void onPostRender(int mouseX, int mouseY, float partialTicks)
	{
		boolean tabHidden = this.hideModInfoScreenTab && this.minecraft.currentScreen instanceof GuiMainMenu;
		
		if (this.tabSupportedOnScreen(this.minecraft.currentScreen) && ((this.displayModInfoScreenTab && !tabHidden) || (this.modInfoScreen != null && this.modInfoScreen.isTweeningOrOpen())))
		{
			// If we're at the main menu, prepare the overlay
			if (this.modInfoScreen == null || this.modInfoScreen.getScreen() != this.minecraft.currentScreen)
			{
				this.modInfoScreen = new GuiScreenModInfo(this.minecraft, this.minecraft.currentScreen, this.mods, this.environment, this.configManager, !tabHidden);
			}

			this.minecraft.entityRenderer.setupOverlayRendering();
			this.modInfoScreen.drawScreen(mouseX, mouseY, partialTicks);
		}
		else if (this.minecraft.currentScreen != this.modInfoScreen && this.modInfoScreen != null)
		{
			// If we're in any other screen, kill the overlay
			this.modInfoScreen.release();
			this.modInfoScreen = null;
		}
		else if (this.tabSupportedOnScreen(this.minecraft.currentScreen) && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && Keyboard.isKeyDown(Keyboard.KEY_TAB))
		{
			this.displayModInfoScreen(this.minecraft.currentScreen);
		}
	}
	
	/**
	 * Set the "mod info" screen tab to hidden, regardless of the property setting
	 */
	public void hideTab()
	{
		this.hideModInfoScreenTab = true;
	}
	
	/**
	 * Set whether the "mod info" screen tab should be shown in the main menu
	 */
	public void setTabVisible(boolean show)
	{
		this.displayModInfoScreenTab = show;
		this.properties.setBooleanProperty(LiteLoaderModPanelManager.OPTION_MOD_INFO_SCREEN, show);
		this.properties.writeProperties();
	}
	
	/**
	 * Get whether the "mod info" screen tab is shown in the main menu
	 */
	public boolean isTabVisible()
	{
		return this.displayModInfoScreenTab;
	}

	/**
	 * Display the "mod info" overlay over the specified GUI
	 * 
	 * @param parentScreen
	 */
	public void displayModInfoScreen(GuiScreen parentScreen)
	{
		if (this.tabSupportedOnScreen(parentScreen))
		{
			this.modInfoScreen = new GuiScreenModInfo(this.minecraft, parentScreen, this.mods, this.environment, this.configManager, !this.hideModInfoScreenTab);
			this.minecraft.displayGuiScreen(this.modInfoScreen);
		}
	}

	private boolean tabSupportedOnScreen(GuiScreen guiScreen)
	{
		return (
			guiScreen instanceof GuiMainMenu ||
			guiScreen instanceof GuiIngameMenu ||
			guiScreen instanceof GuiOptions
		);
	}
}
