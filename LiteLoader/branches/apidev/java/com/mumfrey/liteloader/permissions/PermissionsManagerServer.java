package com.mumfrey.liteloader.permissions;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

import com.mumfrey.liteloader.Permissible;
import com.mumfrey.liteloader.ServerPluginChannelListener;
import com.mumfrey.liteloader.common.GameEngine;

/**
 * TODO implementation
 * 
 * @author Adam Mummery-Smith
 */
public class PermissionsManagerServer implements PermissionsManager, ServerPluginChannelListener
{
	public PermissionsManagerServer()
	{
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getName()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCustomPayload(EntityPlayerMP sender, String channel, int length, byte[] data)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Permissions getPermissions(Permissible mod)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Long getPermissionUpdateTime(Permissible mod)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onTick(GameEngine<?, ?> engine, float partialTicks, boolean inGame)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public List<String> getChannels()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void registerPermissible(Permissible permissible)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void tamperCheck()
	{
		// TODO Auto-generated method stub
		
	}
}
