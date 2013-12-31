package com.mumfrey.liteloader.core;

import java.util.Set;

/**
 * Interface for containers which can be loaded as mods
 * 
 * @author Adam Mummery-Smith
 *
 * @param <L> base class type for Comparable<?> so that implementors can specify their Comparable type
 */
public interface LoadableMod<L> extends Loadable<L>, Injectable
{
	/**
	 * Get the name of the mod
	 */
	public abstract String getModName();

	/**
	 * Get the target loader version for this mod
	 */
	public abstract String getTargetVersion();
	
	/**
	 * Get the revision number for this mod
	 */
	public abstract float getRevision();
	
	/**
	 * Get whether this mod's metadata is valid
	 */
	public abstract boolean hasValidMetaData();

	/**
	 * Get the specified metadata value and return the default value if not present
	 * 
	 * @param metaKey metadata key
	 * @param defaultValue metadata value
	 * @return
	 */
	public abstract String getMetaValue(String metaKey, String defaultValue);

	/**
	 * Get the mod metadata key set
	 */
	public abstract Set<String> getMetaDataKeys();
	
	/**
	 * Returns true if this mod can be added as a resource pack
	 */
	public abstract boolean hasResourcePack();

	/**
	 * Returns the resource pack for this mod, duck typed via generic to avoid ResourcePack being in the method signature
	 */
	public abstract <T> T getResourcePack();
	
	/**
	 * Initialise the resource pack with the specified name
	 */
	public abstract void initResourcePack(String name);
}
