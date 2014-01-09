package com.mumfrey.liteloader.core.gen;

//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import net.minecraft.client.Minecraft;
//import net.minecraft.client.settings.GameSettings;
import net.minecraft.profiler.Profiler;

import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.exceptions.ProfilerCrossThreadAccessException;
import com.mumfrey.liteloader.core.exceptions.ProfilerStackCorruptionException;

/**
 * Profiler used for generating callback signatures for the callback injector
 *
 * @author Adam Mummery-Smith
 */
public class GenProfiler extends Profiler
{
	/**
	 * Cross-thread sanity check 
	 */
	private final Thread minecraftThread;
	
	/**
	 * Logger instance
	 */
	private static Logger logger;
	
	/**
	 * Section list, used as a kind of stack to determine where we are in the profiler stack
	 */
	private LinkedList<String> sectionStack = new LinkedList<String>();

	/**
	 * Optifine compatibility, pointer to the "Profiler" setting so we can enable it if it's disabled
	 */
//	private Field ofProfiler;
	
	/**
	 * Minecraft reference, only set if optifine compatibility is enabled 
	 */
	private Minecraft mc;
	
	private static Map<String, String> eventSignatures = new HashMap<String, String>();
	
	private static Set<String> conflictedEvents = new HashSet<String>();

	private static String lastEvent = null;
	
	private static String lastPosition = null;
	
	/**
	 * .ctor
	 * 
	 * @param events LiteLoader object which will get callbacks
	 * @param logger Logger instance
	 */
	public GenProfiler()
	{
		this.mc = Minecraft.getMinecraft();

		GenProfiler.logger = LiteLoader.getLogger();
		
		// Detect optifine (duh!)
//		this.detectOptifine();
		
		this.minecraftThread = Thread.currentThread();
	}

	/**
	 * Try to detect optifine using reflection
	 * 
	 * @param logger
	 */
//	private void detectOptifine()
//	{
//		try
//		{
//			this.ofProfiler = GameSettings.class.getDeclaredField("ofProfiler");
//		}
//		catch (SecurityException ex) {}
//		catch (NoSuchFieldException ex)
//		{
//			this.logger.info("Optifine not detected, skipping compatibility check");
//		}
//		finally
//		{
//			if (this.ofProfiler != null)
//			{
//				this.logger.info(String.format("Optifine version %s detected, enabling compatibility check", this.getOptifineVersion()));
//			}
//		}
//	}
	
	/**
	 * Try to get the optifine version using reflection
	 * 
	 * @return
	 */
//	private String getOptifineVersion()
//	{
//		try
//		{
//			Class<?> config = Class.forName("Config");
//			
//			if (config != null)
//			{
//				Method getVersion = config.getDeclaredMethod("getVersion");
//				
//				if (getVersion != null)
//				{
//					return (String)getVersion.invoke(null);
//				}
//			}
//		}
//		catch (Exception ex) {}
//		
//		return "Unknown";
//	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.profiler.Profiler#startSection(java.lang.String)
	 */
	@Override
	public void startSection(String sectionName)
	{
		if (Thread.currentThread() != this.minecraftThread)
		{
			this.logger.severe("Profiler cross thread access detected, this indicates an error with one of your mods.");
			throw new ProfilerCrossThreadAccessException(Thread.currentThread().getName());
		}
		
		if ("gameRenderer".equals(sectionName) && "root".equals(this.sectionStack.getLast()))
		{
			this.debugEvent("onRender", sectionName);
		}
		else if ("frustrum".equals(sectionName) && "level".equals(this.sectionStack.getLast()))
		{
			this.debugEvent("onSetupCameraTransform", sectionName);
		}
		else if ("litParticles".equals(sectionName))
		{
			this.debugEvent("postRenderEntities", sectionName);
		}
		else if ("tick".equals(sectionName) && "root".equals(this.sectionStack.getLast()))
		{
			this.debugEvent("onTimerUpdate", sectionName);
		}
		else if ("chat".equals(sectionName))
		{
			this.debugEvent("onRenderChat", sectionName);
		}
		else if ("gui".equals(sectionName) && "gameRenderer".equals(this.sectionStack.getLast()))
		{
			this.debugEvent("onRenderHUD", sectionName);
		}
		
		if ("animateTick".equals(sectionName))
		{
			this.debugEvent("onAnimateTick", sectionName);
		}
		
		this.sectionStack.add(sectionName);
		super.startSection(sectionName);

//		if (this.ofProfiler != null)
//		{
//			try
//			{
//				this.ofProfiler.set(this.mc.gameSettings, true);
//			}
//			catch (IllegalArgumentException ex)
//			{
//				this.ofProfiler = null;
//			}
//			catch (IllegalAccessException ex)
//			{
//				this.ofProfiler = null;
//			}
//		}
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.profiler.Profiler#endSection()
	 */
	@Override
	public void endSection()
	{
		if (Thread.currentThread() != this.minecraftThread)
		{
			this.logger.severe("Profiler cross thread access detected, this indicates an error with one of your mods.");
			throw new ProfilerCrossThreadAccessException(Thread.currentThread().getName());
		}
		
		super.endSection();
		
		try
		{
			String endingSection = this.sectionStack.size() > 0 ? this.sectionStack.removeLast() : null;
			String nextSection = this.sectionStack.size() > 0 ? this.sectionStack.getLast() : null;
			String sectionName = "end[" + endingSection + "] next[" + nextSection + "]";
			
			if ("gameRenderer".equals(endingSection) && "root".equals(nextSection))
			{
//				super.startSection("litetick");
				
				this.debugEvent("onTick", sectionName);
				
//				super.endSection();
			}
			else
			{
				if ("gui".equals(endingSection) && "gameRenderer".equals(nextSection) && this.mc.theWorld != null)
				{
					this.debugEvent("postRenderHUDandGUI", sectionName);
				}
				else if ("mouse".equals(endingSection) && "gameRenderer".equals(nextSection) && (this.mc.skipRenderWorld || this.mc.theWorld == null))
				{
					this.debugEvent("preRenderGUI", sectionName);
				}
				else if ("hand".equals(endingSection) && "level".equals(nextSection))
				{
					this.debugEvent("postRender", sectionName);
				}
				else if ("chat".equals(endingSection))
				{
					this.debugEvent("postRenderChat", sectionName);
				}
			}
		}
		catch (NoSuchElementException ex)
		{
			this.logger.severe("Corrupted Profiler stack detected, this indicates an error with one of your mods.");
			throw new ProfilerStackCorruptionException("Corrupted Profiler stack detected");
		}
	}
	
	private void debugEvent(String lastEvent, String profilerPos)
	{
		GenProfiler.lastEvent = lastEvent;
		GenProfiler.lastPosition = profilerPos;
	}

	public static void storeSignature(String signature)
	{
		if (GenProfiler.lastEvent == null) return;
		if (signature.startsWith("// com")) return;
		
		if (!GenProfiler.eventSignatures.containsKey(GenProfiler.lastEvent))
		{
			GenProfiler.eventSignatures.put(GenProfiler.lastEvent, signature);
			System.out.println("\n// " + GenProfiler.lastPosition + "\n" + signature.replace("<event>", GenProfiler.lastEvent));
		}
		else
		{
			if (!GenProfiler.eventSignatures.get(GenProfiler.lastEvent).equals(signature) && !GenProfiler.conflictedEvents.contains(GenProfiler.lastEvent))
			{
				GenProfiler.conflictedEvents.add(GenProfiler.lastEvent);
				System.out.println("\n// CONFLICT\n// " + GenProfiler.lastPosition + "\n" + signature.replace("<event>", GenProfiler.lastEvent));
			}
		}
	}
}