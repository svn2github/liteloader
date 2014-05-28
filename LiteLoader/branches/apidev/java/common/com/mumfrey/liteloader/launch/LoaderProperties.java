package com.mumfrey.liteloader.launch;

/**
 * Interface for the object which will manage loader properties (internal and volatile)
 * 
 * @author Adam Mummery-Smith
 */
public interface LoaderProperties
{
	/**
	 * True if the "load tweaks" option is enabled and enumerator modules 
	 */
	public abstract boolean loadTweaksEnabled();
	
	/**
	 * Get the mod pack branding from the non-volatile store
	 */
	public abstract String getBranding();

	/**
	 * Set a boolean property in the properties file
	 * 
	 * @param propertyName
	 * @param value
	 */
	public abstract void setBooleanProperty(String propertyName, boolean value);

	/**
	 * Get a boolean property from the properties file
	 * 
	 * @param propertyName
	 * @return
	 */
	public abstract boolean getBooleanProperty(String propertyName);

	/**
	 * Get a boolean property but write and return the supplied default value if the property doesn't exist
	 * 
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	public abstract boolean getAndStoreBooleanProperty(String propertyName, boolean defaultValue);
	
	/**
	 * Get a stored mod revision number from the properties file
	 * 
	 * @param modKey
	 * @return
	 */
	public abstract int getLastKnownModRevision(String modKey);
	
	/**
	 * Store a mod revision number in the properties file
	 * 
	 * @param modKey
	 */
	public abstract void storeLastKnownModRevision(String modKey);
	
	/**
	 * Write the properties to disk
	 */
	public abstract void writeProperties();
}
