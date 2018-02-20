package org.scijava.ui.behaviour.io.gui;

import java.awt.EventQueue;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerDescription;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

public class VisualEditorPanelDemo
{
	/*
	 * DEMO METHODS.
	 */

	private static InputTriggerConfig getDemoConfig()
	{
		final StringReader reader = new StringReader( "---\n" +
				"- !mapping" + "\n" +
				"  action: fluke" + "\n" +
				"  contexts: [all]" + "\n" +
				"  triggers: [F]" + "\n" +
				"- !mapping" + "\n" +
				"  action: drag1" + "\n" +
				"  contexts: [all]" + "\n" +
				"  triggers: [button1, win G]" + "\n" +
				"- !mapping" + "\n" +
				"  action: scroll1" + "\n" +
				"  contexts: [all]" + "\n" +
				"  triggers: [scroll]" + "\n" +
				"- !mapping" + "\n" +
				"  action: scroll1" + "\n" +
				"  contexts: [trackscheme, mamut]" + "\n" +
				"  triggers: [shift D]" + "\n" +
				"- !mapping" + "\n" +
				"  action: destroy the world" + "\n" +
				"  contexts: [mamut]" + "\n" +
				"  triggers: [control A]" + "\n" +
				"" );
		final List< InputTriggerDescription > triggers = YamlConfigIO.read( reader );
		final InputTriggerConfig config = new InputTriggerConfig( triggers );
		return config;
	}

	private static Map< Command, String > getDemoCommands()
	{
		return new CommandDescriptionBuilder()
				.addCommand( "drag1", "mamut", "Move an item around the editor." )
				.addCommand( "drag1", "trackscheme", "Move an item around the editor." )
				.addCommand( "drag1", "other", "Move an item around the editor." )
				.addCommand( "Elude", "other", "Refuse to answer the question." )
				.addCommand( "scroll1", "mamut", null )
				.addCommand( "destroy the world", "all", "Make a disgusting coffee for breakfast. \n"
						+ "For this one, you are by yourself. Good luck and know that we are with you. This is a long line. Hopefully long engouh.\n"
						+ "Hey, what about we add:\n"
						+ "tabulation1\ttabulation2\n"
						+ "lalallala\ttrollololo." )
				.addCommand( "ride the dragon", "all", "Go to work by bike." )
				.addCommand( "Punish", "all", "Go to work by parisian metro." )
				.addCommand( "make some coffee", "mamut", null )
				.addCommand( "make some coffee", "trackscheme", "Make a decent coffee." )
				.get();
	}

	/**
	 * Launch the application.
	 *
	 * @param args
	 *
	 * @throws UnsupportedLookAndFeelException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		EventQueue.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					final JFrame frame = new JFrame( "Behaviour Key bindings editor" );
					final VisualEditorPanel editorPanel = new VisualEditorPanel( getDemoConfig(), getDemoCommands() );
					editorPanel.addConfigChangeListener( () -> System.out.println( "Config changed @ " + new java.util.Date().toString() ) );
					SwingUtilities.updateComponentTreeUI( VisualEditorPanel.fileChooser );
					frame.getContentPane().add( editorPanel );
					frame.pack();
					frame.setVisible( true );
				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		} );
	}
}
