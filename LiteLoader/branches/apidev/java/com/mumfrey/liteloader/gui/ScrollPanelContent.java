package com.mumfrey.liteloader.gui;

import net.minecraft.client.gui.GuiButton;

public interface ScrollPanelContent
{
	public abstract int getScrollPaneContentHeight(GuiScrollPane source);
	
	public abstract void drawScrollPaneContent(GuiScrollPane source, int mouseX, int mouseY, float partialTicks, int scrollAmount, int visibleHeight);

	public abstract void scrollPaneActionPerformed(GuiScrollPane source, GuiButton control);

	public abstract void scrollPaneMousePressed(GuiScrollPane source, int mouseX, int mouseY, int mouseButton);
}
