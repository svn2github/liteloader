package com.mumfrey.liteloader.resources;

import java.io.File;
import java.io.IOException;

import net.minecraft.src.FolderResourcePack;
import net.minecraft.src.MetadataSection;
import net.minecraft.src.MetadataSerializer;

/**
 * Resource pack which wraps a mod directory on the classpath
 *
 * @author Adam Mummery-Smith
 */
public class ModResourcePackDir extends FolderResourcePack
{
	/**
	 * Display name, only shows up in debug output 
	 */
	private final String name;
	
	/**
	 * @param name Friendly name
	 * @param modFile
	 */
	public ModResourcePackDir(String name, File modFile)
	{
		super(modFile);
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.src.AbstractResourcePack#getMetadataSection(net.minecraft.src.MetadataSerializer, java.lang.String)
	 */
	@Override
	public MetadataSection getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException
	{
		try
		{
			// This will fail when fetching pack.mcmeta if there isn't one in the mod file, since we don't care we
			// just catch the exception and return null instead
			return super.getPackMetadata(metadataSerializer, metadataSectionName);
		}
		catch (Exception ex) {}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see net.minecraft.src.AbstractResourcePack#getName()
	 */
	@Override
	public String getPackName()
	{
		return this.name;
	}
}
