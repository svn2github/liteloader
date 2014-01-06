package com.mumfrey.liteloader.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.mumfrey.liteloader.launch.InjectionStrategy;

import net.minecraft.launchwrapper.LaunchClassLoader;

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
	
	/**
	 * Container returned instead of null when a mod does not actually have a container or a container is requested for
	 * a mod which doesn't exist
	 */
	public static final LoadableMod<File> NONE = new EmptyModContainer();
	
	/**
	 * Mod container for a mod which doesn't have a container 
	 *
	 * @author Adam Mummery-Smith
	 */
	public class EmptyModContainer implements LoadableMod<File>
	{
		EmptyModContainer() {}
		
		@Override
		public File getTarget()
		{
			return null;
		}

		@Override
		public String getName()
		{
			return "Unknown";
		}

		@Override
		public String getDisplayName()
		{
			return "Unknown";
		}

		@Override
		public String getLocation()
		{
			return ".";
		}

		@Override
		public String getIdentifier()
		{
			return "Unknown";
		}

		@Override
		public String getVersion()
		{
			return "Unknown";
		}

		@Override
		public String getAuthor()
		{
			return "Unknown";
		}

		@Override
		public boolean isExternalJar()
		{
			return false;
		}

		@Override
		public boolean isToggleable()
		{
			return false;
		}

		@Override
		public boolean isEnabled(EnabledModsList enabledModsList, String profile)
		{
			return true;
		}

		@Override
		public int compareTo(File other)
		{
			return 0;
		}

		@Override
		public URL getURL() throws MalformedURLException
		{
			throw new MalformedURLException("Attempted to get the URL of an empty mod");
		}

		@Override
		public boolean isInjected()
		{
			return false;
		}

		@Override
		public boolean injectIntoClassPath(LaunchClassLoader classLoader, boolean injectIntoParent) throws MalformedURLException
		{
			return false;
		}
		
		@Override
		public InjectionStrategy getInjectionStrategy()
		{
			return null;
		}

		@Override
		public String getModName()
		{
			return "Unknown";
		}

		@Override
		public String getTargetVersion()
		{
			return "";
		}

		@Override
		public float getRevision()
		{
			return 0;
		}

		@Override
		public boolean hasValidMetaData()
		{
			return false;
		}

		@Override
		public String getMetaValue(String metaKey, String defaultValue)
		{
			return defaultValue;
		}

		@Override
		public Set<String> getMetaDataKeys()
		{
			return new HashSet<String>();
		}

		@Override
		public boolean hasResourcePack()
		{
			return false;
		}

		@Override
		public <T> T getResourcePack()
		{
			return null;
		}

		@Override
		public void initResourcePack(String name)
		{
		}
	}
}
