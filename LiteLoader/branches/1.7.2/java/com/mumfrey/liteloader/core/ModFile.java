package com.mumfrey.liteloader.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import joptsimple.internal.Strings;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mumfrey.liteloader.launch.InjectionStrategy;
import com.mumfrey.liteloader.resources.ModResourcePack;

/**
 * Wrapper for file which represents a mod file to load with associated version information and
 * metadata. Retrieve this from litemod.xml at enumeration time. We also override comparable to 
 * provide our own custom sorting logic based on version info.
 *
 * @author Adam Mummery-Smith
 */
public class ModFile extends LoadableFile implements LoadableMod<File>
{
	private static final long serialVersionUID = -7952147161905688459L;

	private static final Logger logger = Logger.getLogger("liteloader");

	/**
	 * Gson parser for JSON
	 */
	protected static Gson gson = new Gson();
	
	/**
	 * True if the metadata information is parsed successfully, the mod will be added
	 */
	protected boolean valid = false;
	
	/**
	 * Name of the mod specified in the JSON file, this can be any string but should be the same between mod versions
	 */
	protected String modName;
	
	/**
	 * Loader version
	 */
	protected String targetVersion;
	
	/**
	 * Name of the class transof
	 */
	protected List<String> classTransformerClassNames = new ArrayList<String>();
	
	/**
	 * File time stamp, used as sorting criteria when no revision information is found
	 */
	protected long timeStamp;
	
	/**
	 * Revision number from the json file
	 */
	protected float revision = 0.0F;
	
	/**
	 * True if the revision number was successfully read, used as a semaphore so that we know when revision is a valid number
	 */
	protected boolean hasRevision = false;
	
	/**
	 * Resource pack we have registered with minecraft
	 */
	protected Object resourcePack = null;
	
	/**
	 * ALL of the parsed metadata from the file, associated with the mod later on for retrieval via the loader
	 */
	protected Map<String, String> metaData = new HashMap<String, String>();
	
	/**
	 * @param file
	 * @param strVersion
	 */
	ModFile(File file, String strVersion)
	{
		super(file.getAbsolutePath());
		
		this.timeStamp = this.lastModified();
		this.parseVersionFile(strVersion);
	}

	@SuppressWarnings("unchecked")
	protected void parseVersionFile(String strVersionData)
	{
		if (Strings.isNullOrEmpty(strVersionData)) return;
		
		try
		{
			this.metaData = ModFile.gson.fromJson(strVersionData, HashMap.class);
		}
		catch (JsonSyntaxException jsx)
		{
			ModFile.logger.warning("Error reading litemod.json in " + this.getAbsolutePath() + ", JSON syntax exception: " + jsx.getMessage());
			return;
		}
		
		this.modName = this.getMetaValue("name", this.getDefaultName());
		this.version = this.getMetaValue("version", "Unknown");
		this.author = this.getMetaValue("author", "Unknown");
		this.targetVersion = this.metaData.get("mcversion");
		if (this.targetVersion == null)
		{
			ModFile.logger.warning("Mod in " + this.getAbsolutePath() + " has no loader version number reading litemod.json");
			return;
		}
		
		try
		{
			this.revision = Float.parseFloat(this.metaData.get("revision"));
			this.hasRevision = true;
		}
		catch (NullPointerException ex) {}
		catch (Exception ex)
		{
			ModFile.logger.warning("Mod in " + this.getAbsolutePath() + " has an invalid revision number reading litemod.json");
		}

		this.valid = true;
		
		this.tweakClassName = this.metaData.get("tweakClass");
		this.tweakPriority = 0;
		
		for (String name : this.getMetaValues("classTransformerClasses", ","))
		{
			if (!Strings.isNullOrEmpty(name))
				this.classTransformerClassNames.add(name);
		}
		
		this.injectionStrategy = InjectionStrategy.parseStrategy(this.metaData.get("injectAt"));
	}

	protected String getDefaultName()
	{
		return this.getName().replaceAll("[^a-zA-Z]", "");
	}
	
	@Override
	public String getModName()
	{
		return this.modName;
	}
	
	@Override
	public String getIdentifier()
	{
		return this.modName.toLowerCase();
	}
	
	@Override
	public String getDisplayName()
	{
		return this.getName();
	}
	
	@Override
	public boolean isExternalJar()
	{
		return false;
	}
	
	@Override
	public boolean isToggleable()
	{
		return true;
	}
	
	@Override
	public boolean hasValidMetaData()
	{
		return this.valid;
	}
	
	@Override
	public String getTargetVersion()
	{
		return this.targetVersion;
	}
	
	@Override
	public float getRevision()
	{
		return this.revision;
	}
	
	@Override
	public String getMetaValue(String metaKey, String defaultValue)
	{
		return this.metaData.containsKey(metaKey) ? this.metaData.get(metaKey) : defaultValue;
	}
	
	public String[] getMetaValues(String metaKey, String separator)
	{
		return this.metaData.containsKey(metaKey) ? this.metaData.get(metaKey).split(separator) : new String[0];
	}

	@Override
	public Set<String> getMetaDataKeys()
	{
		return Collections.unmodifiableSet(this.metaData.keySet());
	}
	
	@Override
	public boolean hasClassTransformers()
	{
		return this.classTransformerClassNames.size() > 0;
	}
	
	@Override
	public List<String> getClassTransformerClassNames()
	{
		return this.classTransformerClassNames;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getResourcePack()
	{
		return (T)this.resourcePack;
	}
	
	/**
	 * Initialise the mod resource pack
	 * 
	 * @param name
	 */
	@Override
	public void initResourcePack(String name)
	{
		if (this.resourcePack == null)
		{
			ModFile.logger.info(String.format("Setting up \"%s\" as mod resource pack with identifier \"%s\"", this.getName(), name));
			this.resourcePack = new ModResourcePack(name, this);
		}
	}
	
	/**
	 * Registers this file as a minecraft resource pack 
	 * 
	 * @param name
	 * @return true if the pack was added
	 */
	@Override
	public boolean hasResourcePack()
	{
		return (this.resourcePack != null);
	}
	
	@Override
	public int compareTo(File other)
	{
		if (other == null || !(other instanceof ModFile)) return -1;
		
		ModFile otherMod = (ModFile)other;
		
		// If the other object has a revision, compare revisions
		if (otherMod.hasRevision)
		{
			return this.hasRevision && this.revision - otherMod.revision > 0 ? -1 : 1;
		}

		// If we have a revision and the other object doesn't, then we are higher
		if (this.hasRevision)
		{
			return -1;
		}

		// Give up and use timestamp
		return (int)(otherMod.timeStamp - this.timeStamp);
	}


	/**
	 * @param zip
	 * @param entry
	 * @return
	 * @throws IOException
	 */
	public static String zipEntryToString(ZipFile zip, ZipEntry entry) throws IOException
	{
		BufferedReader reader = null; 
		StringBuilder sb = new StringBuilder();
		
		try
		{
			InputStream stream = zip.getInputStream(entry);
			reader = new BufferedReader(new InputStreamReader(stream));

			String versionFileLine;
			while ((versionFileLine = reader.readLine()) != null)
				sb.append(versionFileLine);
		}
		finally
		{
			if (reader != null) reader.close();
		}
		
		return sb.toString();
	}
}
