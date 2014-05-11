package com.mumfrey.liteloader.gui.startup;

import static org.lwjgl.opengl.GL11.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.Display;

import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Crappy implementation of a "Mojang Screen" loading bar
 *
 * @author Adam Mummery-Smith
 */
public class LoadingBar
{
	private static final String LOADING_MESSAGE_1 = "Starting Game...";
	private static final String LOADING_MESSAGE_2 = "Initialising...";
	
	private static int minecraftProgress = 0;
	private static int totalMinecraftProgress = 606;
	
	private static int liteLoaderProgressScale = 3;
	
	private static int liteLoaderProgress = 0;
	private static int totalLiteLoaderProgress = 0;
	
	private static ResourceLocation textureLocation = new ResourceLocation("textures/gui/title/mojang.png");
	
	private static String minecraftMessage = LoadingBar.LOADING_MESSAGE_1;
	private static String message = "";
	
	private static Minecraft minecraft;
	private static TextureManager textureManager;
	private static FontRenderer fontRenderer;
	
	private static Framebuffer fbo;
	
	private static boolean enabled = true;
	private static boolean errored;
	
	private static boolean calculatedColour = false;
	private static int barLuma = 0, r2 = 246, g2 = 136, b2 = 62;
	
	private static int logIndex = 0;
	private static List<String> logTail = new ArrayList<String>();;
	
	public static void setEnabled(boolean enabled)
	{
		LoadingBar.enabled = enabled;
	}
	
	public static void dispose()
	{
		LoadingBar.minecraft = null;
		LoadingBar.textureManager = null;
		LoadingBar.fontRenderer = null;
		
		LoadingBar.disposeFbo();
	}
	
	private static void disposeFbo()
	{
		if (LoadingBar.fbo != null)
		{
			LoadingBar.fbo.deleteFramebuffer();
			LoadingBar.fbo = null;
		}
	}
	
	public static void incrementProgress()
	{
		LoadingBar.message = LoadingBar.minecraftMessage;
		
		LoadingBar.minecraftProgress++;
		LoadingBar.render();
	}
	
	public static void initTextures()
	{
		LoadingBar.minecraftMessage = LoadingBar.LOADING_MESSAGE_2;		
	}
	
	public static void incLiteLoaderProgress()
	{
		LoadingBar.liteLoaderProgress += LoadingBar.liteLoaderProgressScale;
		LoadingBar.render();
	}
	
	public static void setMessage(String format, String... args)
	{
		LoadingBar.message = String.format(format, args);
		LoadingBar.render();
	}
	
	public static void setMessage(String message)
	{
		LoadingBar.message = message;
		LoadingBar.render();
	}
	
	public static void incLiteLoaderProgress(String format, String... args)
	{
		LoadingBar.incLiteLoaderProgress(String.format(format, args));
	}
	
	public static void incLiteLoaderProgress(String message)
	{
		LoadingBar.message = message;
		LoadingBar.liteLoaderProgress += LoadingBar.liteLoaderProgressScale ;
		LoadingBar.render();
	}
	
	public static void incTotalLiteLoaderProgress(int by)
	{
		LoadingBar.totalLiteLoaderProgress += (by * LoadingBar.liteLoaderProgressScale);
		LoadingBar.render();
	}
	
	/**
	 * 
	 */
	private static void render()
	{
		if (!LoadingBar.enabled || LoadingBar.errored) return;
		
		try
		{
			if (LoadingBar.minecraft == null) LoadingBar.minecraft = Minecraft.getMinecraft();
			if (LoadingBar.textureManager == null) LoadingBar.textureManager = LoadingBar.minecraft.getTextureManager();
			
			if (Display.isCreated() && LoadingBar.textureManager != null)
			{
				if (LoadingBar.fontRenderer == null)
				{
					LoadingBar.fontRenderer = new FontRenderer(LoadingBar.minecraft.gameSettings, new ResourceLocation("textures/font/ascii.png"), LoadingBar.textureManager, false);
					LoadingBar.fontRenderer.onResourceManagerReload(LoadingBar.minecraft.getResourceManager());
				}
				
				double totalProgress = LoadingBar.totalMinecraftProgress + LoadingBar.totalLiteLoaderProgress;
				double progress = (LoadingBar.minecraftProgress + LoadingBar.liteLoaderProgress) / totalProgress;
				
//				if (progress >= 1.0) LoadingBar.message = "Preparing...";
				
				LoadingBar.render(progress);
			}
		}
		catch (Exception ex)
		{
			// Disable the loading bar if ANY errors occur
			LoadingBar.errored = true;
		}
	}
	
	/**
	 * @param progress
	 */
	private static void render(double progress)
	{
		if (LoadingBar.totalMinecraftProgress == -1)
		{
			LoadingBar.totalMinecraftProgress = 606 - LoadingBar.minecraftProgress;
			LoadingBar.minecraftProgress = 0;
		}
		
		// Calculate the bar colour if we haven't already done that
		if (!LoadingBar.calculatedColour)
		{
			LoadingBar.calculatedColour = true;
			ITextureObject texture = LoadingBar.textureManager.getTexture(LoadingBar.textureLocation);
			if (texture == null)
			{
				try
				{
					DynamicTexture textureData = LoadingBar.loadTexture(LoadingBar.minecraft.getResourceManager(), LoadingBar.textureLocation);
					LoadingBar.textureLocation = LoadingBar.minecraft.getTextureManager().getDynamicTextureLocation("loadingScreen", textureData);
					LoadingBar.findMostCommonColour(textureData.getTextureData());
					textureData.updateDynamicTexture();
				}
				catch (IOException ex)
				{
					ex.printStackTrace();
				}
			}
		}
		
		ScaledResolution scaledResolution = new ScaledResolution(LoadingBar.minecraft.gameSettings, LoadingBar.minecraft.displayWidth, LoadingBar.minecraft.displayHeight);
		int scaleFactor = scaledResolution.getScaleFactor();
		int scaledWidth = scaledResolution.getScaledWidth();
		int scaledHeight = scaledResolution.getScaledHeight();
		
		int fboWidth = scaledWidth * scaleFactor;
		int fboHeight = scaledHeight * scaleFactor;
		
		if (LoadingBar.fbo == null)
		{
			LoadingBar.fbo = new Framebuffer(fboWidth, fboHeight, true);
		}
		else if (LoadingBar.fbo.framebufferWidth != fboWidth || LoadingBar.fbo.framebufferHeight != fboHeight)
		{
			LoadingBar.fbo.createBindFramebuffer(fboWidth, fboHeight);
		}
		
		LoadingBar.fbo.bindFramebuffer(false);
		
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0.0D, scaledWidth, scaledHeight, 0.0D, 1000.0D, 3000.0D);
		
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glTranslatef(0.0F, 0.0F, -2000.0F);
		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		glDisable(GL_LIGHTING);
		glDisable(GL_FOG);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_TEXTURE_2D);
		
		textureManager.bindTexture(LoadingBar.textureLocation);
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.setColorOpaque_I(0xFFFFFFFF);
		tessellator.addVertexWithUV(0.0D,        scaledHeight, 0.0D, 0.0D, 0.0D);
		tessellator.addVertexWithUV(scaledWidth, scaledHeight, 0.0D, 0.0D, 0.0D);
		tessellator.addVertexWithUV(scaledWidth, 0.0D,         0.0D, 0.0D, 0.0D);
		tessellator.addVertexWithUV(0.0D,        0.0D,         0.0D, 0.0D, 0.0D);
		tessellator.draw();
		
		glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		
		int left = (scaledWidth - 256) / 2;
		int top = (scaledHeight - 256) / 2;
		int u1 = 0;
		int v1 = 0;
		int u2 = 256;
		int v2 = 256;
		
		float texMapScale = 0.00390625F;
		tessellator.startDrawingQuads();
		tessellator.setColorOpaque_I(0xFFFFFFFF);
		tessellator.addVertexWithUV(left + 0,  top + v2, 0.0D, (u1 + 0)  * texMapScale, (v1 + v2) * texMapScale);
		tessellator.addVertexWithUV(left + u2, top + v2, 0.0D, (u1 + u2) * texMapScale, (v1 + v2) * texMapScale);
		tessellator.addVertexWithUV(left + u2, top + 0, 0.0D,  (u1 + u2) * texMapScale, (v1 + 0)  * texMapScale);
		tessellator.addVertexWithUV(left + 0,  top + 0, 0.0D,  (u1 + 0)  * texMapScale, (v1 + 0)  * texMapScale);
		tessellator.draw();
		
		glEnable(GL_COLOR_LOGIC_OP);
		glLogicOp(GL_OR_REVERSE);
		LoadingBar.fontRenderer.drawString(LoadingBar.message, 1, scaledHeight - 19, 0xFF000000);
		
		if (LiteLoaderLogger.DEBUG)
		{
			int logBottom = LoadingBar.minecraft.displayHeight - (20 * scaleFactor) - 2;

			glPushMatrix();
			glScalef(1.0F / scaleFactor, 1.0F / scaleFactor, 1.0F);
			LoadingBar.renderLogTail(logBottom);
			glPopMatrix();
		}
		
		glDisable(GL_COLOR_LOGIC_OP);
		glEnable(GL_TEXTURE_2D);
		
		double barHeight = 10.0D;
		
		double barWidth = scaledResolution.getScaledWidth_double() - 2.0D;
		
		glDisable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);
		glEnable(GL_ALPHA_TEST);
		glAlphaFunc(GL_GREATER, 0.0F);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
//		tessellator.startDrawingQuads();
//		tessellator.setColorRGBA(0, 0, 0, 32);
//		tessellator.addVertex(0.0D,               scaledHeight,                      0.0D);
//		tessellator.setColorRGBA(0, 0, 0, 180);
//		tessellator.addVertex(0.0D + scaledWidth, scaledHeight,                      0.0D);
//		tessellator.setColorRGBA(0, 0, 0, 0);
//		tessellator.addVertex(0.0D + scaledWidth, (scaledHeight / 10),               0.0D);
//		tessellator.addVertex(0.0D,               scaledHeight - (scaledHeight / 3), 0.0D);
//		tessellator.draw();
		
		tessellator.startDrawingQuads();
		tessellator.setColorRGBA(LoadingBar.barLuma, LoadingBar.barLuma, LoadingBar.barLuma, 128);
		tessellator.addVertex(0.0D,               scaledHeight,             0.0D);
		tessellator.addVertex(0.0D + scaledWidth, scaledHeight,             0.0D);
		tessellator.addVertex(0.0D + scaledWidth, scaledHeight - barHeight, 0.0D);
		tessellator.addVertex(0.0D,               scaledHeight - barHeight, 0.0D);
		tessellator.draw();
		
		barHeight -= 1;
		
		tessellator.startDrawingQuads();
		tessellator.setColorRGBA(LoadingBar.r2, LoadingBar.g2, LoadingBar.b2, 255);
		tessellator.addVertex(1.0D + barWidth * progress, scaledHeight - 1,         1.0D);
		tessellator.addVertex(1.0D + barWidth * progress, scaledHeight - barHeight, 1.0D);
		tessellator.setColorRGBA(0, 0, 0, 255);
		tessellator.addVertex(1.0D,                       scaledHeight - barHeight, 1.0D);
		tessellator.addVertex(1.0D,                       scaledHeight - 1,         1.0D);
		tessellator.draw();
		
		glAlphaFunc(GL_GREATER, 0.1F);
		glDisable(GL_LIGHTING);
		glDisable(GL_FOG);
		LoadingBar.fbo.unbindFramebuffer();
		
		LoadingBar.fbo.framebufferRender(fboWidth, fboHeight);
		
		glEnable(GL_ALPHA_TEST);
		glAlphaFunc(GL_GREATER, 0.1F);
		glFlush();
		
		LoadingBar.minecraft.func_147120_f();
	}
	
	private static void renderLogTail(int yPos)
	{
		if (LoadingBar.logIndex != LiteLoaderLogger.getLogIndex())
		{
			LoadingBar.logTail = LiteLoaderLogger.getLogTail();
		}
		
		for (int logIndex = LoadingBar.logTail.size() - 1; yPos > 10 && logIndex >= 0; logIndex--)
		{
			LoadingBar.fontRenderer.drawString(LoadingBar.logTail.get(logIndex), 10, yPos -= 10, 0xFF000000);
		}
	}

	/**
	 * Find the most common (approx) colour in the image and assign it to the bar, reduces the palette to 9-bit by
	 * stripping the the 5 LSB from each byte to create a 9-bit palette index in the form RRRGGGBBB
	 * 
	 * @param textureData
	 */
	private static void findMostCommonColour(int[] textureData)
	{
		// Array of frequency values, indexed by palette index
		int[] freq = new int[512];
		
		for (int pos = 0; pos < textureData.length; pos++)
		{
			int paletteIndex = ((textureData[pos] >> 21 & 0x7) << 6) + ((textureData[pos] >> 13 & 0x7) << 3) + (textureData[pos] >> 5 & 0x7);
			freq[paletteIndex]++;
		}
		
		int peak = 0;
		
		// Black, white and 0x200000 excluded on purpose
		for (int paletteIndex = 2; paletteIndex < 511; paletteIndex++)
		{
			if (freq[paletteIndex] > peak)
			{
				peak = freq[paletteIndex];
				LoadingBar.setBarColour(paletteIndex);
			}
		}
	}
	
	/**
	 * @param paletteIndex
	 */
	private static void setBarColour(int paletteIndex)
	{
		LoadingBar.r2 = LoadingBar.padComponent((paletteIndex & 0x1C0) >> 1);
		LoadingBar.g2 = LoadingBar.padComponent((paletteIndex & 0x38) << 2);
		LoadingBar.b2 = LoadingBar.padComponent((paletteIndex & 0x7) << 5);
		
		LoadingBar.barLuma = (Math.max(LoadingBar.r2, Math.max(LoadingBar.g2, LoadingBar.b2)) < 64) ? 255 : 0;
	}
	
	/**
	 * Pad LSB with 1's if any MSB are 1 (effectively a bitwise ceil() function)
	 * 
	 * @param component
	 * @return
	 */
	private static int padComponent(int component)
	{
		return (component > 0x1F) ? component | 0x1F : component;
	}
	
	private static DynamicTexture loadTexture(IResourceManager resourceManager, ResourceLocation textureLocation) throws IOException
	{
		InputStream inputStream = null;
		
		try
		{
			IResource resource = resourceManager.getResource(textureLocation);
			inputStream = resource.getInputStream();
			BufferedImage image = ImageIO.read(inputStream);
			return new DynamicTexture(image);
		}
		finally
		{
			if (inputStream != null)
			{
				inputStream.close();
			}
		}
	}
}
