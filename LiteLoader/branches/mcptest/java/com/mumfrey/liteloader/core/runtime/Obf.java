package com.mumfrey.liteloader.core.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralised obfuscation table for LiteLoader
 *
 * @author Adam Mummery-Smith
 * TODO Obfuscation 1.7.2
 */
public enum Obf
{
	// Non-obfuscated references, here for convenience
	// -----------------------------------------------------------------------------------------
	       InjectedCallbackProxy("com.mumfrey.liteloader.core.transformers.InjectedCallbackProxy"    ),
	                 GameProfile("com.mojang.authlib.GameProfile"                                    ),
	               MinecraftMain("net.minecraft.client.main.Main"                                    ),
	                 constructor("<init>"                                                            ),

	// Classes
	// -----------------------------------------------------------------------------------------
	                   Minecraft("net.minecraft.client.Minecraft",                             "azd" ),
	              EntityRenderer("net.minecraft.client.renderer.EntityRenderer",               "bll" ),
	                   GuiIngame("net.minecraft.client.gui.GuiIngame",                         "bah" ),
	                    Profiler("net.minecraft.profiler.Profiler",                            "ov"  ),
	               CrashReport$6("net.minecraft.crash.CrashReport$6",                          "i"   ),
	           S01PacketJoinGame("net.minecraft.network.play.server.S01PacketJoinGame",        "gu"  ),
	       S02PacketLoginSuccess("net.minecraft.network.login.server.S02PacketLoginSuccess",   "jg"  ),
	               S02PacketChat("net.minecraft.network.play.server.S02PacketChat",            "ga"  ),
	      S3FPacketCustomPayload("net.minecraft.network.play.server.S3FPacketCustomPayload",   "gi"  ),
	                 INetHandler("net.minecraft.network.INetHandler",                          "es"  ),
	        C01PacketChatMessage("net.minecraft.network.play.client.C01PacketChatMessage",     "ie"  ),
	      C17PacketCustomPayload("net.minecraft.network.play.client.C17PacketCustomPayload",   "in"  ),
	            IntegratedServer("net.minecraft.server.integrated.IntegratedServer",           "bsk" ),
	               WorldSettings("net.minecraft.world.WorldSettings",                          "afv" ),
	  ServerConfigurationManager("net.minecraft.server.management.ServerConfigurationManager", "ld"  ),
	              EntityPlayerMP("net.minecraft.entity.player.EntityPlayerMP",                 "mm"  ),
	              NetworkManager("net.minecraft.network.NetworkManager",                       "ef"  ),

	// Fields
	// -----------------------------------------------------------------------------------------
	           minecraftProfiler("field_71424_I",                                              "A"   ), // Minecraft/mcProfiler
	             entityRenderMap("field_78729_o",                                              "q"   ), // RenderManager/entityRenderMap
	             reloadListeners("field_110546_b",                                             "d"   ), // SimpleReloadableResourceManager/reloadListeners
	                  netManager("field_147393_d",                                             "d"   ), // NetHandlerLoginClient/field_147393_d

	// Methods
	// -----------------------------------------------------------------------------------------
	               processPacket("func_148833_a",                                              "a"   ),
	                 runGameLoop("func_71411_J",                                               "ad"  ),
	                     runTick("func_71407_l",                                               "o"   ), 
	       updateCameraAndRender("func_78480_b",                                               "b"   ), 
	                 renderWorld("func_78471_a",                                               "a"   ), 
	           renderGameOverlay("func_73830_a",                                               "a"   ), 
	                startSection("func_76320_a",                                               "a"   ), 
	                  endSection("func_76319_b",                                               "b"   ), 
	             endStartSection("func_76318_c",                                               "c"   ),  
	                 spawnPlayer("func_148545_a",                                              "a"   ),
	               respawnPlayer("func_72368_a",                                               "a"   ),
	initializeConnectionToPlayer("func_72355_a",                                               "a"   ),
	              playerLoggedIn("func_72377_c",                                               "c"   ),
	             playerLoggedOut("func_72367_e",                                               "e"   ),
	                   startGame("func_71384_a",                                               "Z"   );

	public static final int MCP = 0;
	public static final int SRG = 1;
	public static final int OBF = 2;
	
	private static Properties mcpNames;

	/**
	 * Array of names, indexed by MCP, SRG, OBF constants
	 */
	public final String[] names;
	   
	/**
	 * Class, field or method name in unobfuscated (MCP) format
	 */
	public final String name;

	/**
	 * Class name in bytecode notation with slashes instead of dots
	 */
	public final String ref;
	
	/**
	 * Class, field or method name in searge format
	 */
	public final String srg;
	
	/**
	 * Class, field or method name in obfuscated (original) format
	 */
	public final String obf;
	
	/**
	 * @param mcpName
	 * @param seargeName
	 * @param obfName
	 */
	private Obf(String seargeName, String obfName)
	{
		this.name = Obf.getDeobfName(seargeName);
		this.ref = this.name.replace('.', '/');
		this.srg = seargeName;
		this.obf = obfName;
		
		this.names = new String[] { this.name, this.srg, this.obf };
	}

	/**
	 * @param mcpName
	 */
	private Obf(String mcpName)
	{
		this(mcpName, mcpName);
	}
	
	/**
	 * @param type
	 * @return
	 */
	public String getDescriptor(int type)
	{
		return String.format("L%s;", this.names[type].replace('.', '/'));
	}
	
	/**
	 * @param seargeName
	 * @return
	 */
	private static String getDeobfName(String seargeName)
	{
		if (Obf.mcpNames == null)
		{
			Obf.mcpNames = new Properties();
			InputStream is = Obf.class.getResourceAsStream("/obfuscation.properties");
			if (is != null)
			{
				try
				{
					Obf.mcpNames.load(is);
				}
				catch (IOException ex) {}
				
				try
				{
					is.close();
				}
				catch (IOException ex) {}
			}
		}
		
		return Obf.mcpNames.getProperty(seargeName, seargeName);
	}
}
