package com.mumfrey.liteloader.update.jarassassin;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import java.awt.Font;

public class JarAssassinLogWindow extends JFrame
{
	private static final long serialVersionUID = 1L;
	
	private JPanel contentPane;
	private JScrollPane scrollPane;
	private JTextPane textPane;
	
	/**
	 * Create the frame.
	 */
	public JarAssassinLogWindow()
	{
		this.setTitle("JarAssassin");
		this.setResizable(false);
		this.setAlwaysOnTop(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setBounds(100, 100, 758, 418);
		this.contentPane = new JPanel();
		this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.contentPane.setLayout(new BorderLayout(0, 0));
		this.setContentPane(this.contentPane);
		
		this.scrollPane = new JScrollPane();
		this.contentPane.add(this.scrollPane, BorderLayout.CENTER);
		
		this.textPane = new JTextPane();
		this.textPane.setFont(new Font("Monospaced", Font.PLAIN, 10));
		this.scrollPane.setViewportView(this.textPane);
	}
	
	public void log(String message)
	{
		this.textPane.setText(this.textPane.getText() + message + "\n");
		this.textPane.setCaretPosition(this.textPane.getText().length());
		this.textPane.setSelectionStart(this.textPane.getText().length());
	}
}
