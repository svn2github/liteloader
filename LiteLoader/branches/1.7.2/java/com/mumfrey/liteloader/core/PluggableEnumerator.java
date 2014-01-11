package com.mumfrey.liteloader.core;

import java.io.File;

import com.mumfrey.liteloader.LiteMod;

/**
 * Interface for the mod enumerator
 *
 * @author Adam Mummery-Smith
 */
interface PluggableEnumerator
{
	public static final String MOD_CLASS_PREFIX = "LiteMod";
	
	/**
	 * @param module
	 */
	public abstract void registerModule(EnumeratorModule<?> module);
	
	/**
	 * @param container
	 */
	public abstract void addTweaksFrom(TweakContainer<File> container);
	
	/**
	 * @param mod
	 * @param container
	 */
	public abstract void registerMod(Class<? extends LiteMod> mod, LoadableMod<?> container);

	/**
	 * @param propertyName
	 * @param value
	 */
	public abstract void setBooleanProperty(String propertyName, boolean value);

	/**
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	public abstract boolean getAndStoreBooleanProperty(String propertyName, boolean defaultValue);
}