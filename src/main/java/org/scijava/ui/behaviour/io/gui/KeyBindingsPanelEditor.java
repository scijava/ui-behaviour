package org.scijava.ui.behaviour.io.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

public class KeyBindingsPanelEditor extends JPanel
{

	@FunctionalInterface
	public static interface KeyBindingsChangeListener
	{
		public void keyBindingsChanged();
	}

	private static final long serialVersionUID = 1L;

	private static final String COMMIT_ACTION = "commit";

	protected final List< String > selectedKeys;

	protected final List< KeyItem > keyItems;

	private final JTextField textField;

	private final boolean editable;

	private final HashSet< KeyBindingsChangeListener > listeners;

	public KeyBindingsPanelEditor( final boolean editable )
	{
		this.editable = editable;
		this.selectedKeys = new ArrayList<>();
		this.keyItems = new ArrayList<>();
		this.listeners = new HashSet<>();

		setPreferredSize( new Dimension( 400, 26 ) );
		setMinimumSize( new Dimension( 26, 26 ) );
		setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
		setBackground( Color.white );
		setBorder( new JTextField().getBorder() );

		this.textField = new JTextField();
		textField.setColumns( 10 );
		textField.setBorder( null );
		textField.setOpaque( false );
		textField.setEditable( editable );

		if ( editable )
		{
			final Autocomplete autoComplete = new Autocomplete();
			textField.getDocument().addDocumentListener( autoComplete );
			textField.getInputMap().put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 ), COMMIT_ACTION );
			textField.getInputMap().put( KeyStroke.getKeyStroke( KeyEvent.VK_SPACE, 0 ), COMMIT_ACTION );
			textField.getActionMap().put( COMMIT_ACTION, autoComplete.new CommitAction() );
			textField.addKeyListener( new KeyAdapter()
			{
				@Override
				public void keyPressed( final KeyEvent e )
				{
					/*
					 * We have to use a key listener to deal separately with
					 * character removal and tag removal.
					 */
					if ( e.getKeyCode() == KeyEvent.VK_BACK_SPACE && textField.getText().isEmpty() && !selectedKeys.isEmpty() )
					{
						removeTag( selectedKeys.get( selectedKeys.size() - 1 ) );
						e.consume();
					}
				}
			} );
		}

		add( textField );
		add( Box.createHorizontalGlue() );
	}

	public void setKeys( final Collection< String > tags )
	{
		for ( final KeyItem keyItem : keyItems )
			remove( keyItem );
		selectedKeys.clear();
		keyItems.clear();

		for ( final String tag : tags )
			addKey( tag );
		revalidate();
		repaint();
	}

	protected void addKey( final String key )
	{
		final KeyItem tagp = new KeyItem( key, INPUT_TRIGGER_SYNTAX_TAGS.contains( key ) );
		selectedKeys.add( key );
		keyItems.add( tagp );
		add( tagp, getComponentCount() - 2 );
	}

	private void removeTag( final String tag )
	{
		final int index = selectedKeys.indexOf( tag );
		if ( index < 0 )
			return;

		selectedKeys.remove( index );
		final KeyItem tagPanel = keyItems.remove( index );
		notifyListeners();
		remove( tagPanel );
		revalidate();
		repaint();
	}

	/*
	 * INNER CLASSES
	 */

	/**
	 * Adapted from
	 * http://stackabuse.com/example-adding-autocomplete-to-jtextfield/
	 */
	private class Autocomplete implements DocumentListener
	{

		@Override
		public void changedUpdate( final DocumentEvent ev )
		{}

		@Override
		public void removeUpdate( final DocumentEvent ev )
		{}

		@Override
		public void insertUpdate( final DocumentEvent ev )
		{
			if ( ev.getLength() != 1 )
				return;

			final int pos = ev.getOffset();
			String content = null;
			try
			{
				content = textField.getText( 0, pos + 1 );
			}
			catch ( final BadLocationException e )
			{
				e.printStackTrace();
			}

			// Find where the word starts
			int w;
			for ( w = pos; w >= 0; w-- )
			{
				if ( !Character.isLetter( content.charAt( w ) ) )
				{
					break;
				}
			}

			// Too few chars
			if ( pos - w < 2 )
				return;

			final String prefix = content.substring( w + 1 ).toLowerCase();
			final int n = Collections.binarySearch( INPUT_TRIGGER_SYNTAX_TAGS, prefix );
			if ( n < 0 && -n <= INPUT_TRIGGER_SYNTAX_TAGS.size() )
			{
				final String match = INPUT_TRIGGER_SYNTAX_TAGS.get( -n - 1 );
				if ( match.startsWith( prefix ) )
				{
					// A completion is found
					final String completion = match.substring( pos - w );
					// We cannot modify Document from within notification,
					// so we submit a task that does the change later
					SwingUtilities.invokeLater( new CompletionTask( completion, pos + 1 ) );
				}
			}
		}

		public class CommitAction extends AbstractAction
		{
			private static final long serialVersionUID = 5794543109646743416L;

			@Override
			public void actionPerformed( final ActionEvent ev )
			{
				final String key = textField.getText().trim().toUpperCase();
				if ( selectedKeys.contains( key ) )
				{
					// Do not allow more than 1 tag instance.
					textField.setText( "" );
					return;
				}

				addKey( key );
				textField.setText( "" );
				notifyListeners();
				revalidate();
				repaint();
			}
		}

		private class CompletionTask implements Runnable
		{
			private String completion;

			private int position;

			CompletionTask( final String completion, final int position )
			{
				this.completion = completion;
				this.position = position;
			}

			@Override
			public void run()
			{
				final StringBuffer sb = new StringBuffer( textField.getText() );
				sb.insert( position, completion );
				textField.setText( sb.toString() );
				textField.setCaretPosition( position + completion.length() );
				textField.moveCaretPosition( position );
			}
		}

	}

	final class KeyItem extends JPanel
	{

		private static final long serialVersionUID = 1L;

		public KeyItem( final String tag, final boolean valid )
		{
			final Font parentFont = KeyBindingsPanelEditor.this.getFont();
			final Font font = parentFont.deriveFont( parentFont.getSize2D() - 2f );
			final String str = TRIGGER_SYMBOLS.containsKey( tag ) ? TRIGGER_SYMBOLS.get( tag ) : tag;
			final JLabel txt = new JLabel( str );
			txt.setFont( font );
			txt.setOpaque( true );
			if ( !valid )
				txt.setBackground( Color.PINK );
			txt.setBorder( new RoundBorder( getBackground().darker(), KeyBindingsPanelEditor.this, 3 ) );

			final JLabel close = new JLabel( "\u00D7" );
			close.setOpaque( true );
			close.setBackground( getBackground().darker().darker() );
			close.setFont( font );
			close.addMouseListener( new java.awt.event.MouseAdapter()
			{
				@Override
				public void mousePressed( final java.awt.event.MouseEvent evt )
				{
					removeTag( tag );
				}
			} );

			setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
			if ( editable )
				add( close );
			add( Box.createHorizontalStrut( 1 ) );
			add( txt );
			add( Box.createHorizontalStrut( 4 ) );
			setOpaque( false );
		}
	}

	private void notifyListeners()
	{
		for ( final KeyBindingsChangeListener listener : listeners )
			listener.keyBindingsChanged();
	}

	public void addTagSelectionChangeListener( final KeyBindingsChangeListener listener )
	{
		listeners.add( listener );
	}

	public void removeTagSelectionChangeListener( final KeyBindingsChangeListener listener )
	{
		listeners.remove( listener );
	}

	private static final List< String > INPUT_TRIGGER_SYNTAX_TAGS = new ArrayList<>();

	private static final Map< String, String > TRIGGER_SYMBOLS = new HashMap<>();

	static
	{
		for ( int i = 0; i < 26; i++ )
		{
			INPUT_TRIGGER_SYNTAX_TAGS.add( String.valueOf( ( char ) ( 'A' + i ) ) );
			INPUT_TRIGGER_SYNTAX_TAGS.add( String.valueOf( ( char ) ( 'a' + i ) ) );
			// Show small letters as upper case.
			TRIGGER_SYMBOLS.put( String.valueOf( ( char ) ( 'a' + i ) ), String.valueOf( ( char ) ( 'A' + i ) ) );
		}
		for ( int i = 0; i < 10; i++ )
			INPUT_TRIGGER_SYNTAX_TAGS.add( String.valueOf( ( char ) ( '0' + i ) ) );

		INPUT_TRIGGER_SYNTAX_TAGS.addAll(
				Arrays.asList( new String[] {
						"ENTER",
						"BACK_SPACE",
						"TAB",
						"CANCEL",
						"CLEAR",
						"COMPOSE",
						"PAUSE",
						"CAPS_LOCK",
						"ESCAPE",
						"SPACE",
						"PAGE_UP",
						"PAGE_DOWN",
						"END",
						"HOME",
						"BEGIN",
						"COMMA",
						"PERIOD",
						"SLASH",
						"SEMICOLON",
						"EQUALS",
						"OPEN_BRACKET",
						"BACK_SLASH",
						"CLOSE_BRACKET",
						"LEFT",
						"UP",
						"RIGHT",
						"DOWN",
						"NUMPAD0",
						"NUMPAD1",
						"NUMPAD2",
						"NUMPAD3",
						"NUMPAD4",
						"NUMPAD5",
						"NUMPAD6",
						"NUMPAD7",
						"NUMPAD8",
						"NUMPAD9",
						"MULTIPLY",
						"ADD",
						"SEPARATOR",
						"SUBTRACT",
						"DECIMAL",
						"DIVIDE",
						"DELETE",
						"NUM_LOCK",
						"SCROLL_LOCK",
						"ctrl",
						"alt",
						"altGraph",
						"shift",
						"meta",
						"win",
						"double-click",
						"button1 ",
						"button2 ",
						"button3 ",
						"scroll"
				} ) );
		INPUT_TRIGGER_SYNTAX_TAGS.sort( null );

		TRIGGER_SYMBOLS.put( "ENTER", "\u23CE" );
		TRIGGER_SYMBOLS.put( "BACK_SPACE", "\u232B" );
		TRIGGER_SYMBOLS.put( "DELETE", "\u2326" );
		TRIGGER_SYMBOLS.put( "TAB", "\u2B7E" );
		TRIGGER_SYMBOLS.put( "PAUSE", "\u23F8" );
		TRIGGER_SYMBOLS.put( "CAPS_LOCK", "\u21EA" );
		TRIGGER_SYMBOLS.put( "PAGE_UP", "\u21DE" );
		TRIGGER_SYMBOLS.put( "PAGE_DOWN", "\u21DF" );
		TRIGGER_SYMBOLS.put( "END", "\u2198" );
		TRIGGER_SYMBOLS.put( "HOME", "\u2196" );
		TRIGGER_SYMBOLS.put( "ESCAPE", "\u238b" );
		TRIGGER_SYMBOLS.put( "LEFT", "\u2190" );
		TRIGGER_SYMBOLS.put( "UP", "\u2191" );
		TRIGGER_SYMBOLS.put( "RIGHT", "\u2192" );
		TRIGGER_SYMBOLS.put( "DOWN", "\u2193" );
		TRIGGER_SYMBOLS.put( "NUMPAD0", "\u24ea" );
		TRIGGER_SYMBOLS.put( "NUMPAD1", "\u2460" );
		TRIGGER_SYMBOLS.put( "NUMPAD2", "\u2461" );
		TRIGGER_SYMBOLS.put( "NUMPAD3", "\u2462" );
		TRIGGER_SYMBOLS.put( "NUMPAD4", "\u2463" );
		TRIGGER_SYMBOLS.put( "NUMPAD5", "\u2464" );
		TRIGGER_SYMBOLS.put( "NUMPAD6", "\u2465" );
		TRIGGER_SYMBOLS.put( "NUMPAD7", "\u2466" );
		TRIGGER_SYMBOLS.put( "NUMPAD8", "\u2467" );
		TRIGGER_SYMBOLS.put( "NUMPAD9", "\u2468" );
		TRIGGER_SYMBOLS.put( "MULTIPLY", "\u00d7" );
		TRIGGER_SYMBOLS.put( "DIVIDE", "\u00f7" );
		TRIGGER_SYMBOLS.put( "ADD", "\u00d7" );
		TRIGGER_SYMBOLS.put( "ctrl", "\u2303" );
		TRIGGER_SYMBOLS.put( "alt", "\u2387" );
		TRIGGER_SYMBOLS.put( "shift", "\u21e7" );
		TRIGGER_SYMBOLS.put( "meta", "\u25c6" );
		TRIGGER_SYMBOLS.put( "win", "\u2756" );
		// Vertical bar is special
		TRIGGER_SYMBOLS.put( "|", "  |  " );
	}

}
