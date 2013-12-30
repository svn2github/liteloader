package com.mumfrey.liteloader.core;

import java.util.HashSet;
import java.util.Set;

/**
 * LiteLoader version table
 *
 * @author Adam Mummery-Smith
 * @version 1.7.2_01
 */
public enum LiteLoaderVersion
{
	LEGACY(0, "-", "-", "-"),
	
	MC_1_5_2_R1(9,  "1.5.2", "1.5.2",    "1.5.2"          ),
	MC_1_6_1_R0(11, "1.6.1", "1.6.1",    "1.6.1", "1.6.r1"),
	MC_1_6_1_R1(11, "1.6.1", "1.6.1",    "1.6.1", "1.6.r1"),
	MC_1_6_2_R0(12, "1.6.2", "1.6.2",    "1.6.2", "1.6.r2"),
	MC_1_6_2_R1(12, "1.6.2", "1.6.2_01", "1.6.2", "1.6.r2"),
	MC_1_6_2_R2(13, "1.6.2", "1.6.2_02", "1.6.2", "1.6.r2"),
	MC_1_6_2_R3(14, "1.6.2", "1.6.2_03", "1.6.2", "1.6.r2"),
	MC_1_6_2_R4(15, "1.6.2", "1.6.2_04", "1.6.2", "1.6.r2"),
	MC_1_6_3_R0(16, "1.6.3", "1.6.3",    "1.6.3", "1.6.r3"),
	MC_1_6_4_R0(17, "1.6.4", "1.6.4",    "1.6.4", "1.6.r4"),
	MC_1_6_4_R1(18, "1.6.4", "1.6.4_01", "1.6.4", "1.6.r4"),
	MC_1_6_4_R2(19, "1.6.4", "1.6.4_02", "1.6.4", "1.6.r4"),
	MC_1_7_2_R0(20, "1.7.2", "1.7.2",    "1.7.2", "1.7.r1"),
	MC_1_7_2_R1(21, "1.7.2", "1.7.2_01", "1.7.2_01");
	
	private int revision;
	
	private String minecraftVersion;
	
	private String loaderVersion;
	
	private Set<String> supportedVersions = new HashSet<String>();

	private LiteLoaderVersion(int revision, String minecraftVersion, String loaderVersion, String... supportedVersions)
	{
		this.revision = revision;
		this.minecraftVersion = minecraftVersion;
		this.loaderVersion = loaderVersion;
		
		for (String supportedVersion : supportedVersions)
			this.supportedVersions.add(supportedVersion);
	}

	public int getLoaderRevision()
	{
		return this.revision;
	}

	public String getMinecraftVersion()
	{
		return this.minecraftVersion;
	}

	public String getLoaderVersion()
	{
		return this.loaderVersion;
	}
	
	public static LiteLoaderVersion getVersionFromRevision(int revision)
	{
		for (LiteLoaderVersion version : LiteLoaderVersion.values())
		{
			if (version.getLoaderRevision() == revision)
				return version;
		}
		
		return LiteLoaderVersion.LEGACY;
	}

	public boolean isVersionSupported(String version)
	{
		return this.supportedVersions.contains(version);
	}
	
	@Override
	public String toString()
	{
		return this == LiteLoaderVersion.LEGACY ? "Unknown" : this.loaderVersion;
	}
}
