package com.examplemod;

import java.io.File;

import org.lwjgl.input.Keyboard;

import net.minecraft.src.KeyBinding;
import net.minecraft.src.Minecraft;

import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.util.ModUtilities;

/**
 * This is a very simple example LiteMod, it draws an analogue clock on the minecraft HUD using
 * a traditional onTick hook supplied by LiteLoader's "Tickable" interface.
 *
 * @author Adam Mummery-Smith
 */
public class LiteModExample implements Tickable
{
	/**
	 * This is our instance of Clock which we will draw every tick
	 */
	private Clock clock = new Clock(10, 10, 64);;
	
	/**
	 * This is a keybinding that we will register with the game and use to toggle the clock
	 */
	private static KeyBinding clockKeyBinding = new KeyBinding("Toggle Clock", Keyboard.KEY_F12);
	
	/**
	 * Default constructor. All LiteMods must have a default constructor. In general you should do very little
	 * in the mod constructor EXCEPT for initialising any non-game-interfacing components or performing
	 * sanity checking prior to initialisation
	 */
	public LiteModExample()
	{
	}
	
	/**
	 * getName() should be used to return the display name of your mod and MUST NOT return null
	 * 
	 * @see com.mumfrey.liteloader.LiteMod#getName()
	 */
	@Override
	public String getName()
	{
		return "Example Mod";
	}
	
	/**
	 * getVersion() should return the same version string present in the mod metadata, although this is
	 * not a strict requirement.
	 * 
	 * @see com.mumfrey.liteloader.LiteMod#getVersion()
	 */
	@Override
	public String getVersion()
	{
		return "0.0.0";
	}
	
	/**
	 * init() is called very early in the initialisation cycle, before the game is fully initialised, this
	 * means that it is important that your mod does not interact with the game in any way at this point.
	 * 
	 * @see com.mumfrey.liteloader.LiteMod#init(java.io.File)
	 */
	@Override
	public void init(File configPath)
	{
		// The key binding declared above won't do anything unless we register it, ModUtilties provides 
		// a convenience method for this
		ModUtilities.registerKey(clockKeyBinding);
	}
	
	/**
	 * upgradeSettings is used to notify a mod that its version-specific settings are being migrated
	 * 
	 * @see com.mumfrey.liteloader.LiteMod#upgradeSettings(java.lang.String, java.io.File, java.io.File)
	 */
	@Override
	public void upgradeSettings(String version, File configPath, File oldConfigPath)
	{
	}
	
	@Override
	public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock)
	{
		// The three checks here are critical to ensure that we only draw the clock as part of the "HUD"
		// and don't draw it over active GUI's or other elements
		if (inGame && minecraft.currentScreen == null && Minecraft.isGuiEnabled())
		{
			if (LiteModExample.clockKeyBinding.isPressed())
			{
				if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
				{
					this.clock.setSize((this.clock.getSize() << 1) & 0x1FF);
				}
				else
				{
					this.clock.setVisible(!this.clock.isVisible());
				}	
			}
			
			this.clock.render(minecraft);
		}
	}
}
