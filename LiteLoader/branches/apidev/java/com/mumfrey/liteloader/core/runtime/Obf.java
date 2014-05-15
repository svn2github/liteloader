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
public class Obf
{
	// Non-obfuscated references, here for convenience
	// -----------------------------------------------------------------------------------------
	public static final Obf        InjectedCallbackProxy = new Obf("com.mumfrey.liteloader.core.transformers.InjectedCallbackProxy"    );
	public static final Obf                   LoadingBar = new Obf("com.mumfrey.liteloader.gui.startup.LoadingBar"                     );
	public static final Obf                  GameProfile = new Obf("com.mojang.authlib.GameProfile"                                    );
	public static final Obf                MinecraftMain = new Obf("net.minecraft.client.main.Main"                                    );
	public static final Obf                  constructor = new Obf("<init>"                                                            );

	// Classes
	// -----------------------------------------------------------------------------------------
	public static final Obf                    Minecraft = new Obf("net.minecraft.client.Minecraft",                             "azd" );
	public static final Obf               EntityRenderer = new Obf("net.minecraft.client.renderer.EntityRenderer",               "bll" );
	public static final Obf                    GuiIngame = new Obf("net.minecraft.client.gui.GuiIngame",                         "bah" );
	public static final Obf                     Profiler = new Obf("net.minecraft.profiler.Profiler",                            "ov"  );
	public static final Obf                CrashReport$6 = new Obf("net.minecraft.crash.CrashReport$6",                          "i"   );
	public static final Obf            S01PacketJoinGame = new Obf("net.minecraft.network.play.server.S01PacketJoinGame",        "gu"  );
	public static final Obf        S02PacketLoginSuccess = new Obf("net.minecraft.network.login.server.S02PacketLoginSuccess",   "jg"  );
	public static final Obf                S02PacketChat = new Obf("net.minecraft.network.play.server.S02PacketChat",            "ga"  );
	public static final Obf       S3FPacketCustomPayload = new Obf("net.minecraft.network.play.server.S3FPacketCustomPayload",   "gi"  );
	public static final Obf                  INetHandler = new Obf("net.minecraft.network.INetHandler",                          "es"  );
	public static final Obf         C01PacketChatMessage = new Obf("net.minecraft.network.play.client.C01PacketChatMessage",     "ie"  );
	public static final Obf       C17PacketCustomPayload = new Obf("net.minecraft.network.play.client.C17PacketCustomPayload",   "in"  );
	public static final Obf             IntegratedServer = new Obf("net.minecraft.server.integrated.IntegratedServer",           "bsk" );
	public static final Obf                WorldSettings = new Obf("net.minecraft.world.WorldSettings",                          "afv" );
	public static final Obf   ServerConfigurationManager = new Obf("net.minecraft.server.management.ServerConfigurationManager", "ld"  );
	public static final Obf               EntityPlayerMP = new Obf("net.minecraft.entity.player.EntityPlayerMP",                 "mm"  );
	public static final Obf               NetworkManager = new Obf("net.minecraft.network.NetworkManager",                       "ef"  );

	// Fields
	// -----------------------------------------------------------------------------------------
	public static final Obf            minecraftProfiler = new Obf("field_71424_I",                                              "A"   ); // Minecraft/mcProfiler
	public static final Obf              entityRenderMap = new Obf("field_78729_o",                                              "q"   ); // RenderManager/entityRenderMap
	public static final Obf              reloadListeners = new Obf("field_110546_b",                                             "d"   ); // SimpleReloadableResourceManager/reloadListeners
	public static final Obf                   netManager = new Obf("field_147393_d",                                             "d"   ); // NetHandlerLoginClient/field_147393_d

	// Methods
	// -----------------------------------------------------------------------------------------
	public static final Obf                processPacket = new Obf("func_148833_a",                                              "a"   );
	public static final Obf                  runGameLoop = new Obf("func_71411_J",                                               "ad"  );
	public static final Obf                      runTick = new Obf("func_71407_l",                                               "o"   ); 
	public static final Obf        updateCameraAndRender = new Obf("func_78480_b",                                               "b"   ); 
	public static final Obf                  renderWorld = new Obf("func_78471_a",                                               "a"   ); 
	public static final Obf            renderGameOverlay = new Obf("func_73830_a",                                               "a"   ); 
	public static final Obf                 startSection = new Obf("func_76320_a",                                               "a"   ); 
	public static final Obf                   endSection = new Obf("func_76319_b",                                               "b"   ); 
	public static final Obf              endStartSection = new Obf("func_76318_c",                                               "c"   );  
	public static final Obf                  spawnPlayer = new Obf("func_148545_a",                                              "a"   );
	public static final Obf                respawnPlayer = new Obf("func_72368_a",                                               "a"   );
	public static final Obf initializeConnectionToPlayer = new Obf("func_72355_a",                                               "a"   );
	public static final Obf               playerLoggedIn = new Obf("func_72377_c",                                               "c"   );
	public static final Obf              playerLoggedOut = new Obf("func_72367_e",                                               "e"   );
	public static final Obf                    startGame = new Obf("func_71384_a",                                               "Z"   );

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
	protected Obf(String seargeName, String obfName)
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
	protected Obf(String mcpName)
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