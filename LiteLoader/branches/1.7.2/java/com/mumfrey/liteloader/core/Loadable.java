package com.mumfrey.liteloader.core;

/**
 * Interface for things which are loadable, essentially mods and tweaks
 * 
 * @author Adam Mummery-Smith
 *
 * @param <L> base class type for Comparable<?> so that implementors can specify their Comparable type 
 */
public interface Loadable<L> extends Comparable<L>
{
	/**
	 * Get the target resource
	 */
	public abstract L getTarget();
	
	/**
	 * Get the name of the loadable (usually the file name)
	 */
	public abstract String getName();
	
	/**
	 * Get the name to use when displaying this loadable, such as file name, meta name or friendly name
	 */
	public abstract String getDisplayName();
	
	/**
	 * Get the location (path or URL) of this loadable
	 */
	public abstract String getLocation();
	
	/**
	 * Get the identifier (meta name) of this loadable, used as the exclusivity key for mods and also the metadata key
	 */
	public abstract String getIdentifier();
	
	/**
	 * Get the version specified in the metadata or other location
	 */
	public abstract String getVersion();
	
	/**
	 * Get the author specified in the metadata
	 */
	public abstract String getAuthor();
	
	/**
	 * Returns true if this is an external jar containing a tweak rather than a mod
	 */
	public abstract boolean isExternalJar();
	
	/**
	 * Returns true if this loadable supports being enabled and disabled via the GUI
	 */
	public abstract boolean isToggleable();

	/**
	 * Get whether this loadable is currently enabled in the context of the supplied mods list
	 * 
	 * @param enabledModsList
	 * @param profile
	 * @return
	 */
	public abstract boolean isEnabled(EnabledModsList enabledModsList, String profile);
}
