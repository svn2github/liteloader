package com.mumfrey.liteloader.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.mumfrey.liteloader.api.CoreProvider;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.LiteLoaderMods;
import com.mumfrey.liteloader.util.jinput.ComponentRegistry;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.world.World;

/**
 * Mod input class, aggregates functionality from LiteLoader's mod key registration functions and JInputLib
 *
 * @author Adam Mummery-Smith
 */
public final class Input implements CoreProvider
{
	/**
	 * 
	 */
	private Minecraft minecraft;

	/**
	 * File in which we will store mod key mappings
	 */
	private final File keyMapSettingsFile;
	
	/**
	 * Properties object which stores mod key mappings
	 */
	private final Properties keyMapSettings = new Properties();
	
	/**
	 * List of all registered mod keys
	 */
	private final List<KeyBinding> modKeyBindings = new ArrayList<KeyBinding>();
	
	/**
	 * Map of mod key bindings to their key codes, stored so that we don't need to cast from
	 * string in the properties file every tick
	 */
	private final Map<KeyBinding, Integer> storedModKeyBindings = new HashMap<KeyBinding, Integer>();
	
	/**
	 * JInput component registry 
	 */
	private final ComponentRegistry jInputComponentRegistry;
	
	/**
	 * List of handlers for JInput components
	 */
	private final Map<Component, InputEvent> componentEvents = new HashMap<Component, InputEvent>();
	
	/**
	 * JInput Controllers to poll
	 */
	private Controller[] pollControllers = new Controller[0];
	
	/**
	 * 
	 */
	public Input(File keyMapSettingsFile)
	{
		if (LiteLoader.getInstance() != null && LiteLoader.getInput() != null)
		{
			throw new IllegalStateException("Only one instance of Input is allowed, use LiteLoader.getInput() to get the active instance");
		}
		
		this.keyMapSettingsFile = keyMapSettingsFile;
		this.jInputComponentRegistry = new ComponentRegistry();
		this.jInputComponentRegistry.enumerate();
	}
	
	@Override
	public void onInit()
	{
		if (this.keyMapSettingsFile.exists())
		{
			try
			{
				this.keyMapSettings.load(new FileReader(this.keyMapSettingsFile));
			}
			catch (Exception ex) {}
		}
	}
	
	@Override
	public void onPostInit(Minecraft minecraft)
	{
		this.minecraft = minecraft;
	}

	@Override
	public void onPostInitComplete(LiteLoaderMods mods)
	{
	}
	
	@Override
	public void onStartupComplete()
	{
	}
	
	@Override
	public void onJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
	}
	
	@Override
	public void onWorldChanged(World world)
	{
	}
	
	@Override
	public void onPostRender(int mouseX, int mouseY, float partialTicks)
	{
	}
	
	/**
	 * Register a key for a mod
	 * 
	 * @param binding
	 */
	public void registerKeyBinding(KeyBinding binding)
	{
		Minecraft minecraft = Minecraft.getMinecraft();
		LinkedList<KeyBinding> keyBindings = new LinkedList<KeyBinding>();
		keyBindings.addAll(Arrays.asList(minecraft.gameSettings.keyBindings));
		
		if (!keyBindings.contains(binding))
		{
			if (this.keyMapSettings.containsKey(binding.getKeyDescription()))
			{
				try
				{
					binding.setKeyCode(Integer.parseInt(this.keyMapSettings.getProperty(binding.getKeyDescription(), String.valueOf(binding.getKeyCode()))));
				}
				catch (NumberFormatException ex) {}
			}

			keyBindings.add(binding);
			minecraft.gameSettings.keyBindings = keyBindings.toArray(new KeyBinding[0]);
			this.modKeyBindings.add(binding);
			
			this.updateBinding(binding);
			this.storeBindings();
			
			KeyBinding.resetKeyBindingArrayAndHash();
		}
	}
	
	/**
	 * Unregisters a registered keybind with the game settings class, thus removing it from the "controls" screen
	 * 
	 * @param binding
	 */
	public void unRegisterKeyBinding(KeyBinding binding)
	{
		Minecraft minecraft = Minecraft.getMinecraft();
		
		LinkedList<KeyBinding> keyBindings = new LinkedList<KeyBinding>();
		keyBindings.addAll(Arrays.asList(minecraft.gameSettings.keyBindings));
		
		if (keyBindings.contains(binding))
		{
			keyBindings.remove(binding);
			minecraft.gameSettings.keyBindings = keyBindings.toArray(new KeyBinding[0]);

			this.modKeyBindings.remove(binding);
			
			KeyBinding.resetKeyBindingArrayAndHash();
		}
	}
	
	/**
	 * Checks for changed mod keybindings and stores any that have changed 
	 */
	@Override
	public void onTick(boolean clock, float partialTicks, boolean inGame)
	{
		this.minecraft.mcProfiler.startSection("keybindings");
		if (clock)
		{
			boolean updated = false;
			
			for (KeyBinding binding : this.modKeyBindings)
			{
				if (binding.getKeyCode() != this.storedModKeyBindings.get(binding))
				{
					this.updateBinding(binding);
					updated = true;
				}
			}
			
			if (updated) this.storeBindings();
		}
		
		this.pollControllers();
		this.minecraft.mcProfiler.endSection();
	}
	
	/**
	 * @param binding
	 */
	private void updateBinding(KeyBinding binding)
	{
		this.keyMapSettings.setProperty(binding.getKeyDescription(), String.valueOf(binding.getKeyCode()));
		this.storedModKeyBindings.put(binding, Integer.valueOf(binding.getKeyCode()));
	}
	
	@Override
	public void onShutDown()
	{
		this.storeBindings();
	}

	/**
	 * Writes mod bindings to disk
	 */
	public void storeBindings()
	{
		try
		{
			this.keyMapSettings.store(new FileWriter(this.keyMapSettingsFile), "Mod key mappings for LiteLoader mods, stored here to avoid losing settings stored in options.txt");
		}
		catch (IOException ex) {}
	}
	
	/**
	 * Gets the underlying JInput component registry
	 */
	public ComponentRegistry getComponentRegistry()
	{
		return this.jInputComponentRegistry;
	}

	/**
	 * Returns a handle to the event described by descriptor (or null if no component is found matching the
	 * descriptor. Retrieving an event via this method adds the controller (if found) to the polling list and
	 * causes it to raise events against the specified handler.
	 * 
	 * This method returns an {@link InputEvent} which is passed as an argument to the relevant callback on
	 * the supplied handler in order to identify the event. For example:
	 * 
	 *    this.joystickButton = input.getEvent(descriptor, this);
	 * 
	 * then in onAxisEvent
	 * 
	 *   if (source == this.joystickButton) // do something with button
	 * 
	 * @param descriptor
	 * @param handler
	 * @return
	 */
	public InputEvent getEvent(String descriptor, InputHandler handler)
	{
		if (handler == null) return null;
		Component component = this.jInputComponentRegistry.getComponent(descriptor);
		Controller controller = this.jInputComponentRegistry.getController(descriptor);
		return this.addEventHandler(controller, component, handler);
	}
	
	/**
	 * Get events for all components which match the supplied descriptor 
	 * 
	 * @param descriptor
	 * @param handler
	 * @return
	 */
	public InputEvent[] getEvents(String descriptor, InputHandler handler)
	{
		List<InputEvent> events = new ArrayList<InputEvent>();
		Controller controller = this.jInputComponentRegistry.getController(descriptor);
		if (controller != null)
		{
			for (Component component : controller.getComponents())
			{
				events.add(this.addEventHandler(controller, component, handler));
			}
		}
		
		return events.toArray(new InputEvent[0]);
	}
	
	/**
	 * @param controller
	 * @param component
	 * @param handler
	 * @return
	 */
	private InputEvent addEventHandler(Controller controller, Component component, InputHandler handler)
	{
		if (controller != null && component != null && handler != null)
		{
			this.addController(controller);
			
			InputEvent event = new InputEvent(controller, component, handler);
			this.componentEvents.put(component, event.link(this.componentEvents.get(component)));
			
			return event;
		}
		
		return null;
	}

	/**
	 * @param controller
	 */
	private void addController(Controller controller)
	{
		Set<Controller> controllers = this.getActiveControllers();
		controllers.add(controller);
		this.setActiveControllers(controllers);
	}

	/**
	 * @return
	 */
	private Set<Controller> getActiveControllers()
	{
		Set<Controller> allControllers = new HashSet<Controller>();
		for (Controller controller : this.pollControllers)
			allControllers.add(controller);
		return allControllers;
	}
	
	/**
	 * @param controllers
	 */
	private void setActiveControllers(Set<Controller> controllers)
	{
		this.pollControllers = controllers.toArray(new Controller[controllers.size()]);
	}

	/**
	 * 
	 */
	private void pollControllers()
	{
		for (Controller controller : this.pollControllers)
		{
			controller.poll();
			EventQueue controllerQueue = controller.getEventQueue();
			
			for (Event event = new Event(); controllerQueue.getNextEvent(event); )
			{
				Component cmp = event.getComponent();
				
				InputEvent inputEvent = this.componentEvents.get(cmp);
				if (inputEvent != null)
				{
					inputEvent.onEvent(event);
				}
			}
		}
	}
}
