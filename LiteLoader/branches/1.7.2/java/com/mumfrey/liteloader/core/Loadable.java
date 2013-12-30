package com.mumfrey.liteloader.core;

public interface Loadable<L> extends Comparable<L>
{
	public abstract String getName();
	
	public abstract String getDisplayName();
	
	public abstract String getIdentifier();

	public abstract String getVersion();
	
	public abstract float getRevision();
	
	public abstract boolean isExternalJar();
	
	public abstract boolean isToggleable();

	public abstract boolean isEnabled(EnabledModsList enabledModsList, String profile);
}
