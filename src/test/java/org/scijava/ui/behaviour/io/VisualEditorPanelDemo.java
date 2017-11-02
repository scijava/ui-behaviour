package org.scijava.ui.behaviour.io;

import java.awt.EventQueue;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
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
				"  contexts: [unknown context, mamut]" + "\n" +
				"  triggers: [control A]" + "\n" +
				"" );
		final List< InputTriggerDescription > triggers = YamlConfigIO.read( reader );
		final InputTriggerConfig config = new InputTriggerConfig( triggers );
		return config;
	}

	private static Map< String, String > getDemoActions()
	{
		final Map< String, String > actions = new HashMap<>();
		actions.put( "drag1", "Move an item around the editor." );
		actions.put( "scroll1", null );
		actions.put( "destroy the world", "Make a disgusting coffee for breakfast. \n"
				+ "For this one, you are by yourself. Good luck and know that we are with you. This is a long line. Hopefully long engouh.\n"
				+ "Hey, what about we add:\n"
				+ "tabulation1\ttabulation2\n"
				+ "lalallala\ttrollololo." );
		actions.put( "ride the dragon", "Go to work by bike." );
		actions.put( "make some coffee", null );
		return actions;
	}

	private static Set< String > getDemoContexts()
	{
		final Set< String > contexts = new HashSet<>();
		contexts.add( "all" );
		contexts.add( "mamut" );
		contexts.add( "trackscheme" );
		return contexts;
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
					final VisualEditorPanel editorPanel = new VisualEditorPanel( getDemoConfig(), getDemoActions(), getDemoContexts() );
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
