package com.mumfrey.liteloader.launch;

public interface LoaderProperties
{
	public abstract boolean loadTweaksEnabled();
	
	public abstract String getBranding();

	public abstract void setBooleanProperty(String propertyName, boolean value);

	public abstract boolean getBooleanProperty(String propertyName);

	public abstract boolean getAndStoreBooleanProperty(String propertyName, boolean defaultValue);
	
	public abstract int getLastKnownModRevision(String modKey);
	
	public abstract void storeLastKnownModRevision(String modKey);
	
	public abstract void writeProperties();
}
