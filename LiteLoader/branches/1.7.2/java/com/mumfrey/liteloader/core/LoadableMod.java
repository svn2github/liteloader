package com.mumfrey.liteloader.core;

import java.util.Map;

public interface LoadableMod<L> extends Loadable<L>
{
	public abstract String getModName();
	
	public abstract boolean isValid();

	public abstract String getMetaValue(String metaKey, String defaultValue);

	public abstract Map<String, String> getMetaData();

	public abstract <T> T getResourcePack();
	
	public abstract void initResourcePack(String name);
	
	public abstract boolean hasResourcePack();
}
