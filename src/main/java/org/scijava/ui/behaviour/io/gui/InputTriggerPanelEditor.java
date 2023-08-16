/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2023 Max Planck Institute of Molecular Cell Biology
 * and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.scijava.ui.behaviour.io.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.scijava.ui.behaviour.InputTrigger;

import static org.scijava.ui.behaviour.io.gui.TagPanelEditor.mix;

public class InputTriggerPanelEditor extends JPanel
{

	@FunctionalInterface
	public static interface InputTriggerChangeListener
	{
		public void inputTriggerChanged();
	}

	private static final long serialVersionUID = 1L;

	private static final String COMMIT_ACTION = "commit";

	private final List< KeyItem > keyItems;

	private final JTextField textField;

	private final HashSet< InputTriggerChangeListener > listeners;

	private InputTrigger trigger = InputTrigger.NOT_MAPPED;

	private InputTrigger lastValidInputTrigger = InputTrigger.NOT_MAPPED;

	private String invalidTriggerStr = null;

	public InputTriggerPanelEditor( final boolean editable )
	{
		this.keyItems = new ArrayList<>();
		this.listeners = new HashSet<>();

		setPreferredSize( new Dimension( 400, 26 ) );
		setMinimumSize( new Dimension( 26, 26 ) );
		setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );

		this.textField = new JTextField();
		textField.setColumns( 10 );
		textField.setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		textField.setOpaque( false );
		textField.setEditable( editable );

		if ( editable )
		{
			final Autocomplete autoComplete = new Autocomplete();
			textField.getDocument().addDocumentListener( autoComplete );
			textField.getInputMap().put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 ), COMMIT_ACTION );
			textField.getInputMap().put( KeyStroke.getKeyStroke( ' ' ), COMMIT_ACTION );
			textField.setFocusTraversalKeys( KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.emptySet() );
			textField.getInputMap().put( KeyStroke.getKeyStroke( KeyEvent.VK_TAB, 0 ), COMMIT_ACTION );
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
					if ( e.getKeyCode() == KeyEvent.VK_BACK_SPACE && textField.getText().isEmpty() && !keyItems.isEmpty() )
					{
						removeLastKeyItem();
						e.consume();
					}
				}
			} );
		}

		add( textField );
		add( Box.createHorizontalGlue() );
	}

	@Override
	public void updateUI()
	{
		super.updateUI();
		setBorder( UIManager.getBorder( "TextField.border" ) );
		setBackground( UIManager.getColor( "TextField.background" ) );
	}

	public void setInputTrigger( final InputTrigger trigger )
	{
		this.trigger = trigger;
		this.lastValidInputTrigger = trigger;
		this.invalidTriggerStr = null;
		regenKeyPanels();
	}

	public InputTrigger getInputTrigger()
	{
		return trigger;
	}

	public InputTrigger getLastValidInputTrigger()
	{
		return lastValidInputTrigger;
	}

	private void checkAndAppendKey( final String key )
	{
		final String trimmed = key.trim();
		if ( trimmed.equals( "|" ) )
		{
			/*
			 * Special case: the user wants to enter keys to ignore.
			 */

			final String str = ( null == trigger || trigger == InputTrigger.NOT_MAPPED )
					? trimmed
							: trigger.toString() + " " + trimmed;

			this.trigger = null;
			this.invalidTriggerStr = str.trim();
			regenKeyPanels();
			return;
		}

		// Find its proper case equivalent in the syntax list.
		String properCapKey = null;
		for ( int i = 0; i < INPUT_TRIGGER_SYNTAX_TAGS.size(); i++ )
		{
			if ( INPUT_TRIGGER_SYNTAX_TAGS.get( i ).equalsIgnoreCase( trimmed ) )
			{
				properCapKey = INPUT_TRIGGER_SYNTAX_TAGS.get( i );
				break;
			}
		}

		if ( null != properCapKey )
		{
			// Some tags are replaced for constructing the trigger, e.g., "cmd" is replaces by "meta"
			properCapKey = INPUT_TRIGGER_SYNTAX_TAG_REMAP.getOrDefault( properCapKey, properCapKey );

			// Try to append the key to the trigger.
			final String str = ( null == trigger )
					? invalidTriggerStr + " " + properCapKey
							: ( trigger == InputTrigger.NOT_MAPPED )
							? properCapKey
									: trigger.toString() + " " + properCapKey;

			try
			{
				this.trigger = InputTrigger.getFromString( str );
				this.lastValidInputTrigger = trigger;
				this.invalidTriggerStr = null;
			}
			catch ( final IllegalArgumentException iae )
			{
				this.trigger = null;
				this.invalidTriggerStr = str.trim();
			}
			regenKeyPanels();
		}
	}

	private void removeLastKeyItem()
	{
		// Try to remove the last key item.
		final String[] tokens;
		if ( null == trigger )
		{
			/*
			 * The trigger was invalid. In that case we show what would be the
			 * key sequence in red.
			 */
			tokens = invalidTriggerStr.split( " " );
		}
		else
		{
			/*
			 * The trigger field is valid.
			 */
			tokens = trigger.toString().split( " " );
		}
		if ( tokens.length == 0 )
			return;

		// We have to sort tokens as they are displayed.
		sortTokens( tokens );

		final StringBuilder strBlder = new StringBuilder();
		for ( int i = 0; i < tokens.length - 1; i++ )
			strBlder.append( tokens[ i ] + " " );

		final String str = strBlder.toString();
		try
		{
			this.trigger = str.isEmpty()
					? InputTrigger.NOT_MAPPED
					: InputTrigger.getFromString( str );
			this.lastValidInputTrigger = trigger;
			this.invalidTriggerStr = null;
		}
		catch ( final IllegalArgumentException iae )
		{
			this.trigger = null;
			this.invalidTriggerStr = str.trim();
		}
		regenKeyPanels();
		notifyListeners();
	}

	private void regenKeyPanels()
	{
		// Clear
		for ( final KeyItem keyItem : keyItems )
			remove( keyItem );
		keyItems.clear();

		final String[] tokens;
		boolean valid;
		if ( null == trigger )
		{
			/*
			 * The trigger was invalid. In that case we show what would be the
			 * key sequence in red.
			 */
			tokens = invalidTriggerStr.split( " " );
			valid = false;
		}
		else
		{
			/*
			 * The trigger field is valid.
			 */

			valid = true;
			if ( trigger == InputTrigger.NOT_MAPPED )
				tokens = new String[] {};
			else
				tokens = trigger.toString().split( " " );

		}

		if ( tokens.length == 0 )
		{
			trigger = InputTrigger.NOT_MAPPED;
			lastValidInputTrigger = trigger;
			invalidTriggerStr = null;
		}
		else
		{
			sortTokens( tokens );
			for ( final String key : tokens )
			{
				final KeyItem tagp = new KeyItem( key, valid );
				keyItems.add( tagp );
				add( tagp, getComponentCount() - 2 );
			}
		}

		revalidate();
		repaint();
	}

	@Override
	public boolean requestFocusInWindow()
	{
		return textField.requestFocusInWindow();
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

			final String prefix = content.substring( w + 1 );
			// We search on the lower case list.
			final int n = Collections.binarySearch( INPUT_TRIGGER_SYNTAX_TAGS_SMALL_CAPS, prefix.toLowerCase() );
			if ( n < 0 && -n <= INPUT_TRIGGER_SYNTAX_TAGS_SORTED.size() )
			{
				final String match = INPUT_TRIGGER_SYNTAX_TAGS_SORTED.get( -n - 1 );
				if ( match.toLowerCase().startsWith( prefix.toLowerCase() ) )
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
				final String key = textField.getText().trim();
				if ( key.isEmpty() )
				{
					KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
					return;
				}
				checkAndAppendKey( key );
				textField.setText( "" );
				notifyListeners();
			}
		}

		private class CompletionTask implements Runnable
		{
			private final String completion;

			private final int position;

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

	private final class KeyItem extends JPanel
	{
		private static final long serialVersionUID = 1L;

		private final boolean valid;

		private final JLabel txt;

		public KeyItem( final String tag, final boolean valid )
		{
			this.valid = valid;
			final String str = TRIGGER_SYMBOLS.containsKey( tag ) ? ( " " + TRIGGER_SYMBOLS.get( tag ) + " " ) : ( " " + tag + " " );
			txt = new JLabel( str );
			txt.setOpaque( true );
			updateTxtLook();

			setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
			add( Box.createHorizontalStrut( 1 ) );
			add( txt );
			add( Box.createHorizontalStrut( 1 ) );
			setOpaque( false );
		}

		@Override
		public void updateUI()
		{
			super.updateUI();
			updateTxtLook();
		}

		private void updateTxtLook()
		{
			if ( txt != null )
			{
				final Color tfg = UIManager.getColor( "TextField.foreground" );
				final Color tbg = UIManager.getColor( "TextField.background" );
				final Color bg = valid ? mix( tbg, tfg, 0.95 ) : mix( tbg, Color.red, 0.5 );
				final Color borderColor = mix( bg, tfg, 0.8 );
				txt.setBackground( bg );
				txt.setBorder( new RoundBorder( borderColor, InputTriggerPanelEditor.this, 1 ) );

				Font font = UIManager.getFont( "Label.font" );
				font = font.deriveFont( font.getSize2D() - 2f );
				txt.setFont( font );
			}
		}
	}

	private void notifyListeners()
	{
		for ( final InputTriggerChangeListener listener : listeners )
			listener.inputTriggerChanged();
	}

	public void addInputTriggerChangeListener( final InputTriggerChangeListener listener )
	{
		listeners.add( listener );
	}

	public void removeInputTriggerChangeListener( final InputTriggerChangeListener listener )
	{
		listeners.remove( listener );
	}

	/** Contains the tags in the order we want them to appear in the panel. */
	private static final List< String > INPUT_TRIGGER_SYNTAX_TAGS = new ArrayList<>();
	/** Contains the tags sorted so that they can be searched by the autocomplete process. */
	private static final List< String > INPUT_TRIGGER_SYNTAX_TAGS_SORTED = new ArrayList<>();
	/** Small-caps version of INPUT_TRIGGER_SYNTAX_TAGS_SORTED. */
	private static final List< String > INPUT_TRIGGER_SYNTAX_TAGS_SMALL_CAPS;
	/** Visual replacement for some tags. */
	private static final Map< String, String > TRIGGER_SYMBOLS = new HashMap<>();
	private static final Map<String, String> INPUT_TRIGGER_SYNTAX_TAG_REMAP = new HashMap<>();

	static
	{
		INPUT_TRIGGER_SYNTAX_TAGS.addAll(
				Arrays.asList(
						"all",
						"ctrl",
						"alt",
						"altGraph",
						"shift",
						"meta",
						"command",
						"cmd",
						"win",
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
						"double-click",
						"button1",
						"button2",
						"button3",
						"scroll",
						"|" ) );
		for ( int i = 0; i < 26; i++ )
			INPUT_TRIGGER_SYNTAX_TAGS.add( String.valueOf( ( char ) ( 'A' + i ) ) );
		for ( int i = 0; i < 10; i++ )
			INPUT_TRIGGER_SYNTAX_TAGS.add( "" + i );
		for ( int i = 1; i <= 24; i++ )
			INPUT_TRIGGER_SYNTAX_TAGS.add( "F" + i );

		INPUT_TRIGGER_SYNTAX_TAGS_SORTED.addAll( INPUT_TRIGGER_SYNTAX_TAGS );
		INPUT_TRIGGER_SYNTAX_TAGS_SORTED.sort( String.CASE_INSENSITIVE_ORDER );
		INPUT_TRIGGER_SYNTAX_TAGS_SMALL_CAPS = new ArrayList<>( INPUT_TRIGGER_SYNTAX_TAGS_SORTED.size() );
		for ( final String tag : INPUT_TRIGGER_SYNTAX_TAGS_SORTED )
			INPUT_TRIGGER_SYNTAX_TAGS_SMALL_CAPS.add( tag.toLowerCase() );

		INPUT_TRIGGER_SYNTAX_TAG_REMAP.put( "cmd", "meta" );
		INPUT_TRIGGER_SYNTAX_TAG_REMAP.put( "command", "meta" );
		INPUT_TRIGGER_SYNTAX_TAG_REMAP.put( "windows", "win" );

		TRIGGER_SYMBOLS.put( "ENTER", "\u23CE" );
		TRIGGER_SYMBOLS.put( "BACK_SPACE", "\u232B" );
		TRIGGER_SYMBOLS.put( "DELETE", "\u2326" );
		TRIGGER_SYMBOLS.put( "TAB", "\u21E5" );
		TRIGGER_SYMBOLS.put( "PAUSE", "||" );
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
		TRIGGER_SYMBOLS.put( "ADD", "+" );
		TRIGGER_SYMBOLS.put( "SUBTRACT", "-" );
		TRIGGER_SYMBOLS.put( "COMMA", ",");
		TRIGGER_SYMBOLS.put( "PERIOD", ".");
		TRIGGER_SYMBOLS.put( "SLASH", "/" );
		TRIGGER_SYMBOLS.put( "SEMICOLON", ";" );
		TRIGGER_SYMBOLS.put( "EQUALS", "=");
		TRIGGER_SYMBOLS.put( "OPEN_BRACKET", "[");
		TRIGGER_SYMBOLS.put( "BACK_SLASH", "\\");
		TRIGGER_SYMBOLS.put( "CLOSE_BRACKET", "]");
		TRIGGER_SYMBOLS.put( "ctrl", "\u2303" );
		TRIGGER_SYMBOLS.put( "alt", "\u2387" );
		TRIGGER_SYMBOLS.put( "shift", "\u21e7" );
		TRIGGER_SYMBOLS.put( "meta", isMac() ? "\u2318" : "\u25c6" );
		TRIGGER_SYMBOLS.put( "win", "\u2756" );
		// Vertical bar is special
		TRIGGER_SYMBOLS.put( "|", "    |    " );
	}

	/**
	 * Sort tokens in a visually pleasing way. Makes sure we do not mess with
	 * the '|' syntax.
	 */
	private static final void sortTokens( final String[] tokens )
	{
		int vbarIndex = -1;
		for ( int i = 0; i < tokens.length; i++ )
		{
			if ( tokens[ i ].equals( "|" ) )
			{
				vbarIndex = i;
				break;
			}
		}
		if ( vbarIndex >= 0 )
		{
			Arrays.sort( tokens, 0, vbarIndex, Comparator.comparingInt( INPUT_TRIGGER_SYNTAX_TAGS::indexOf ) );
			Arrays.sort( tokens, vbarIndex + 1, tokens.length, Comparator.comparingInt( INPUT_TRIGGER_SYNTAX_TAGS::indexOf ) );
		}
		else
			Arrays.sort( tokens, Comparator.comparingInt( INPUT_TRIGGER_SYNTAX_TAGS::indexOf ) );
	}

	private static boolean isMac()
	{
		final String OS = System.getProperty( "os.name", "generic" ).toLowerCase( Locale.ENGLISH );
		return ( OS.indexOf( "mac" ) >= 0 ) || ( OS.indexOf( "darwin" ) >= 0 );
	}
}
