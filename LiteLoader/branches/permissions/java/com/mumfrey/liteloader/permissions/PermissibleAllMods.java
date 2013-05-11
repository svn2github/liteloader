package com.mumfrey.liteloader.permissions;

import java.util.HashSet;
import java.util.Set;

import com.mumfrey.liteloader.Permissible;

public class PermissibleAllMods implements Permissible
{
	private Set<Permissible> permissibles = new HashSet<Permissible>();
	
	public void addPermissible(Permissible permissible)
	{
		this.permissibles.add(permissible);
	}
	
	@Override
	public String getName()
	{
		return "All Mods";
	}
	
	@Override
	public String getVersion()
	{
		return "0.0";
	}
	
	@Override
	public void init()
	{
	}
	
	@Override
	public String getPermissibleModName()
	{
		return "all";
	}
	
	@Override
	public float getPermissibleModVersion()
	{
		return 0.0F;
	}
	
	@Override
	public void registerPermissions(PermissionsManagerClient permissionsManager)
	{
	}
	
	@Override
	public void onPermissionsCleared(PermissionsManager manager)
	{
		for (Permissible permissible : this.permissibles)
			permissible.onPermissionsCleared(manager);
	}
	
	@Override
	public void onPermissionsChanged(PermissionsManager manager)
	{
		for (Permissible permissible : this.permissibles)
			permissible.onPermissionsChanged(manager);
	}
}
