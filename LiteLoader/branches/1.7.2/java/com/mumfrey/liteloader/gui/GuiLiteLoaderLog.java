package com.mumfrey.liteloader.gui;

import static com.mumfrey.liteloader.gui.GuiScreenModInfo.*;
import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;

import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 *
 * @author Adam Mummery-Smith
 */
public class GuiLiteLoaderLog extends ModInfoScreenPanel
{
	private static boolean useNativeRes = false;
	
	/**
	 * Scroll bar for the panel
	 */
	GuiSimpleScrollBar scrollBar = new GuiSimpleScrollBar();
	
	private List<String> logEntries = new ArrayList<String>();
	
	private long logIndex = -1;
	
	/**
	 * Panel's internal height (for scrolling)
	 */
	private int totalHeight = -1;
	
	private GuiCheckbox chkScale;
	
	private float guiScale;
	
	/**
	 * @param parent
	 * @param minecraft
	 * @param panel
	 * @param mod
	 */
	GuiLiteLoaderLog(Minecraft minecraft)
	{
		super(minecraft);
	}

	private void updateLog()
	{
		this.logEntries = LiteLoaderLogger.getLogTail();
		this.logIndex = LiteLoaderLogger.getLogIndex();
		this.totalHeight = (int)(this.logEntries.size() * 10 / (this.chkScale.checked ? this.guiScale : 1.0F));
		this.scrollBar.setMaxValue(this.totalHeight);
		this.scrollBar.setValue(this.totalHeight);
	}

	/**
	 * Callback from parent screen when window is resized
	 * 
	 * @param width
	 * @param height
	 */
	@Override
	void setSize(int width, int height)
	{
		super.setSize(width, height);
		
		this.controls.add(new GuiButton(0, this.width - 99 - MARGIN, this.height - BOTTOM + 9, 100, 20, I18n.format("gui.done")));
		this.controls.add(this.chkScale = new GuiCheckbox(1, MARGIN, this.height - BOTTOM + 15, I18n.format("gui.log.scalecheckbox")));
		
		this.chkScale.checked = GuiLiteLoaderLog.useNativeRes;
		
		ScaledResolution res = new ScaledResolution(this.mc.gameSettings, this.mc.displayWidth, this.mc.displayHeight);
		this.guiScale = res.getScaleFactor();

		this.updateLog();
	}
	
	/**
	 * Callback from parent screen when panel is displayed
	 */
	@Override
	void onShown()
	{
	}
	
	/**
	 * Callback from parent screen when panel is hidden
	 */
	@Override
	void onHidden()
	{
	}
	
	/**
	 * Callback from parent screen every tick
	 */
	@Override
	void onTick()
	{
		if (LiteLoaderLogger.getLogIndex() > this.logIndex)
		{
			this.updateLog();
		}
	}
	
	/**
	 * Draw the panel and chrome
	 * 
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 */
	@Override
	void draw(int mouseX, int mouseY, float partialTicks)
	{
		// Scroll position
		this.innerTop = TOP - this.scrollBar.getValue();
	
		// Draw panel title
		this.mc.fontRenderer.drawString(I18n.format("gui.log.title"), MARGIN, TOP - 14, 0xFFFFFFFF);
		
		// Draw top and bottom horizontal bars
		drawRect(MARGIN, TOP - 4, this.width - MARGIN, TOP - 3, 0xFF999999);
		drawRect(MARGIN, this.height - BOTTOM + 2, this.width - MARGIN, this.height - BOTTOM + 3, 0xFF999999);

		// Clip rect
		glEnableClipping(MARGIN, this.width - MARGIN - 6, TOP, this.height - BOTTOM);
		
		// Offset by scroll
		glPushMatrix();
		glTranslatef(MARGIN, this.innerTop, 0.0F);

		this.drawLog(this.scrollBar.getValue());
		
		// Disable clip rect
		glDisableClipping();
		
		// Restore transform
		glPopMatrix();
		
		// Update and draw scroll bar
		this.scrollBar.setMaxValue(this.totalHeight - this.innerHeight);
		this.scrollBar.drawScrollBar(mouseX, mouseY, partialTicks, this.width - MARGIN - 5, TOP, 5, this.innerHeight, Math.max(this.innerHeight, this.totalHeight));
		
		// Draw other buttons
		super.draw(mouseX, mouseY, partialTicks);
	}

	private void drawLog(int offset)
	{
		int yPos = 0;
		int height = this.innerHeight;
		
		if (this.chkScale.checked)
		{
			float scale = 1.0F / this.guiScale;
			glScalef(scale, scale, scale);
			
			height = (int)(height * this.guiScale);
			offset = (int)(offset * this.guiScale);
		}
		
		for (String logLine : this.logEntries)
		{
			if (yPos > offset - 10 && yPos <= offset + height)
			{
				this.mc.fontRenderer.drawString(logLine, 0, yPos, this.getMessageColour(logLine.toLowerCase()));
			}
			yPos += 10;
		}
	}

	private int getMessageColour(String logLine)
	{
		if (logLine.startsWith("liteloader")) return 0xFFFFFF;
		if (logLine.startsWith("active pack:")) return 0xFFFF55;
		if (logLine.startsWith("success")) return 0x55FF55;
		if (logLine.startsWith("discovering")) return 0xFFFF55;
		if (logLine.startsWith("searching")) return 0x00AA00;
		if (logLine.startsWith("considering")) return 0xFFAA00;
		if (logLine.startsWith("not adding")) return 0xFF5555;
		if (logLine.startsWith("mod in")) return 0xAA0000;
		if (logLine.startsWith("error")) return 0xAA0000;
		if (logLine.startsWith("adding newest")) return 0x5555FF;
		if (logLine.startsWith("found")) return 0xFFFF55;
		if (logLine.startsWith("discovered")) return 0xFFFF55;
		if (logLine.startsWith("setting up")) return 0xAA00AA;
		if (logLine.startsWith("adding \"")) return 0xAA00AA;
		if (logLine.startsWith("injecting")) return 0xFF55FF;
		if (logLine.startsWith("loading")) return 0x5555FF;
		if (logLine.startsWith("initialising")) return 0x55FFFF;
		if (logLine.startsWith("calling late")) return 0x00AAAA;
		if (logLine.startsWith("dependency check")) return 0xFFAA00;
		if (logLine.startsWith("dependency")) return 0xFF5500;
		
		return 0xCCCCCC;
	}

	/**
	 * @param control
	 */
	@Override
	void actionPerformed(GuiButton control)
	{
		if (control.id == 0) this.close();
		
		if (control.id == 1 && this.chkScale != null)
		{
			this.chkScale.checked = !this.chkScale.checked;
			GuiLiteLoaderLog.useNativeRes = this.chkScale.checked;
			this.updateLog();
		}
	}

	/**
	 * @param mouseWheelDelta
	 */
	@Override
	void mouseWheelScrolled(int mouseWheelDelta)
	{
		this.scrollBar.offsetValue(-mouseWheelDelta / 8);
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @param mouseButton
	 */
	@Override
	void mousePressed(int mouseX, int mouseY, int mouseButton)
	{
		if (mouseButton == 0)
		{
			if (this.scrollBar.wasMouseOver())
				this.scrollBar.setDragging(true);
		}
		
		super.mousePressed(mouseX, mouseY, mouseButton);
	}
	
	/**
	 * @param mouseX
	 * @param mouseY
	 * @param mouseButton
	 */
	@Override
	void mouseReleased(int mouseX, int mouseY, int mouseButton)
	{
		if (mouseButton == 0)
		{
			this.scrollBar.setDragging(false);
		}
	}
	
	/**
	 * @param mouseX
	 * @param mouseY
	 */
	@Override
	void mouseMoved(int mouseX, int mouseY)
	{
	}
	
	/**
	 * @param keyChar
	 * @param keyCode
	 */
	@Override
	void keyPressed(char keyChar, int keyCode)
	{
		if (keyCode == Keyboard.KEY_ESCAPE) this.close();
		
		if (keyCode == Keyboard.KEY_SPACE) this.actionPerformed(this.chkScale);
		
		if (keyCode == Keyboard.KEY_UP) this.scrollBar.offsetValue(-10);
		if (keyCode == Keyboard.KEY_DOWN) this.scrollBar.offsetValue(10);
		if (keyCode == Keyboard.KEY_PRIOR) this.scrollBar.offsetValue(-this.innerHeight + 10);
		if (keyCode == Keyboard.KEY_NEXT) this.scrollBar.offsetValue(this.innerHeight - 10);
		if (keyCode == Keyboard.KEY_HOME) this.scrollBar.setValue(0);
		if (keyCode == Keyboard.KEY_END) this.scrollBar.setValue(this.totalHeight);
	}
}
