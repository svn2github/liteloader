package com.mumfrey.liteloader.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class GuiHoverLabel extends GuiButton
{
	private FontRenderer fontRenderer;
	private int colour;
	private int hoverColour;

	public GuiHoverLabel(int id, int xPosition, int yPosition, FontRenderer fontRenderer, String displayText)
	{
		this(id, xPosition, yPosition, fontRenderer, displayText, 0xFF4785D1, 0xFFFFFFAA);
	}
	
	public GuiHoverLabel(int id, int xPosition, int yPosition, FontRenderer fontRenderer, String displayText, int colour, int hoverColour)
	{
		super(id, xPosition, yPosition, GuiHoverLabel.getStringWidth(fontRenderer, displayText), 8, displayText);
		
		this.fontRenderer = fontRenderer;
		this.colour = colour;
		this.hoverColour = hoverColour;
	}
	
	@Override
	public void drawButton(Minecraft minecraft, int mouseX, int mouseY)
	{
		if (this.field_146125_m)
		{
			this.field_146123_n = mouseX >= this.field_146128_h && mouseY >= this.field_146129_i && mouseX < this.field_146128_h + this.field_146120_f && mouseY < this.field_146129_i + this.field_146121_g;
			this.fontRenderer.drawString(this.displayString, this.field_146128_h, this.field_146129_i, this.field_146123_n ? this.hoverColour : this.colour);
		}
		else
		{
			this.field_146123_n = false;
		}
	}
	
	private static int getStringWidth(FontRenderer fontRenderer, String text)
	{
		return fontRenderer.getStringWidth(text);
	}
}
