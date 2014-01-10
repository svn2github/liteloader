package com.mumfrey.liteloader.core;

import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Interface for objects which can enumerate mods in places
 * 
 * @author Adam Mummery-Smith
 *
 * @param <T>
 */
public interface EnumeratorModule<T>
{
	/**
	 * Find loadable mods in this enumerator's domain
	 * 
	 * @param enabledModsList
	 * @param profile
	 */
	public abstract void enumerate(EnabledModsList enabledModsList, String profile);
	
	/**
	 * @param classLoader
	 */
	public abstract void injectIntoClassLoader(LaunchClassLoader classLoader);

	/**
	 * @param classLoader
	 */
	public abstract void registerMods(LaunchClassLoader classLoader);

	/**
	 * 
	 */
	public abstract void writeSettings();
}