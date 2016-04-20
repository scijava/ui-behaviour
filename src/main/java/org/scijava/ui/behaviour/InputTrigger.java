package org.scijava.ui.behaviour;

import java.awt.AWTKeyStroke;
import java.awt.event.InputEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.KeyStroke;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * A combination of keys, mouse buttons, and/or mouse scrolling that can trigger a {@link Behaviour}.
 *
 * <p>
 * Parsing String in {@link #getFromString(String)} borrows heavily from {@link AWTKeyStroke}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class InputTrigger
{
	public static final int DOUBLE_CLICK_MASK = 1 << 20;

	public static final int SCROLL_MASK = 1 << 21;

	public static final int WIN_DOWN_MASK = 1 << 22;

	public static final int IGNORE_MASK = -1;

	public static final InputTrigger NOT_MAPPED = new InputTrigger( IGNORE_MASK, new TIntHashSet(), null );

	/**
	 * Word to use to specify a double-click modifier.
	 */
	private static final String DOUBLE_CLICK_TEXT = "double-click";

	/**
	 * Word to use to specify scrolling.
	 */
	private static final String SCROLL_TEXT = "scroll";

	/**
	 * Word used to specify the windows key.
	 */
	private static final String WINDOWS_TEXT= "win";

	private final int mask;

	private final TIntSet pressedKeys;

	private final KeyStroke keyStroke;

	private final int hashcode;

	public static InputTrigger getFromString( final String s ) throws IllegalArgumentException
	{
		if ( s == null || s.length() == 0 )
			return null;

		if ( s.equals( "not mapped" ) )
			return NOT_MAPPED;

        final StringTokenizer st = new StringTokenizer(s, " ");
        final Map<String, Integer> modifierKeywords = getModifierKeywords();

		int mask = 0;
		final TIntSet pressedKeys = new TIntHashSet();

		final int count = st.countTokens();

		for ( int i = 1; i <= count; i++ )
		{
			final String token = st.nextToken();
			if ( token.equals( "released" ) )
			{}
			else if ( token.equals( "pressed" ) )
			{}
			else if ( token.equals( "typed" ) )
			{}
			else
			{
				final Integer tokenMask = modifierKeywords.get( token );
				if ( tokenMask != null )
				{
					mask |= tokenMask.intValue();
				}
				else
				{
					final KeyStroke ks = KeyStroke.getKeyStroke( token );
					if ( ks == null || ks.getKeyCode() == 0 )
						throw new IllegalArgumentException( "InputTrigger String \"" + s + "\" is formatted incorrectly" );
					pressedKeys.add( ks.getKeyCode() );
				}
			}
		}

		KeyStroke keyStroke = null;
		if ( ( mask & ( DOUBLE_CLICK_MASK | InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK ) ) == 0 )
		{
			// no mouse keys, no double-click -- pure keystroke
			keyStroke = KeyStroke.getKeyStroke( s );
		}

		return getCached( new InputTrigger( mask, pressedKeys, keyStroke ) );
	}

	private InputTrigger( final int mask, final TIntSet pressedKeys, final KeyStroke keyStroke )
	{
		this.mask = mask;
		this.pressedKeys = pressedKeys;
		this.keyStroke = keyStroke;

		int value = 17;
		value = 31 * value + mask;
		value = 31 * value + pressedKeys.hashCode();
		hashcode = value;
	}

	public int getMask()
	{
		return mask;
	}

	public TIntCollection getPressedKeys()
	{
		return pressedKeys;
	}

	public boolean isKeyTriggered()
	{
		return ( mask & ( InputEvent.BUTTON1_DOWN_MASK |  InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK | SCROLL_MASK ) ) == 0;
	}

	public boolean isKeyStroke()
	{
		return keyStroke != null;
	}

	public KeyStroke getKeyStroke()
	{
		return keyStroke;
	}

	public boolean matches( final int mask, final TIntSet keys )
	{
		if ( this.mask != mask )
			return false;
		else
			return keys.equals( pressedKeys );
	}

	@Override
	public int hashCode()
	{
		return hashcode;
	}

	@Override
	public boolean equals( final Object obj )
	{
		if ( obj == this )
			return true;

		if ( ! InputTrigger.class.isInstance( obj ) )
			return false;

		final InputTrigger o = ( InputTrigger ) obj;
		return mask == o.mask && pressedKeys.equals( o.pressedKeys );
	}

    @Override
	public String toString()
    {
		final StringBuilder buf = new StringBuilder();

		addModifierText( InputEvent.SHIFT_DOWN_MASK, "shift", buf );
		addModifierText( InputEvent.CTRL_DOWN_MASK, "ctrl", buf );
		addModifierText( InputEvent.META_DOWN_MASK, "meta", buf );
		addModifierText( InputEvent.ALT_DOWN_MASK, "alt", buf );
		addModifierText( InputEvent.ALT_GRAPH_DOWN_MASK, "altGraph", buf );
		addModifierText( InputEvent.BUTTON1_DOWN_MASK, "button1", buf );
		addModifierText( InputEvent.BUTTON2_DOWN_MASK, "button2", buf );
		addModifierText( InputEvent.BUTTON3_DOWN_MASK, "button3", buf );
		addModifierText( DOUBLE_CLICK_MASK, DOUBLE_CLICK_TEXT, buf );
		addModifierText( SCROLL_MASK, SCROLL_TEXT, buf );
		addModifierText( WIN_DOWN_MASK, WINDOWS_TEXT, buf );

		final TIntIterator iter = pressedKeys.iterator();
		while ( iter.hasNext() )
		{
			final int key = iter.next();
			if ( buf.length() > 0 )
				buf.append( " " );
			final String vkName = KeyStroke.getKeyStroke( key, 0 ).toString();
			buf.append( vkName.replace( "pressed ", "" ) );
		}

		return buf.toString();
    }

    private void addModifierText( final int flag, final String name, final StringBuilder buf )
    {
		if ( ( mask & flag ) != 0 )
		{
			if ( buf.length() > 0 )
				buf.append( " " );
			buf.append( name );
		}
    }

	/*
	 * Caching
	 */

	private static Map< String, Integer > modifierKeywords;

    private synchronized static Map<String, Integer> getModifierKeywords()
    {
		if ( modifierKeywords == null )
		{
			final Map< String, Integer > uninitializedMap = new HashMap<>( 8, 1.0f );
			uninitializedMap.put( "shift",
					Integer.valueOf( InputEvent.SHIFT_DOWN_MASK ) );
			uninitializedMap.put( "control",
					Integer.valueOf( InputEvent.CTRL_DOWN_MASK ) );
			uninitializedMap.put( "ctrl",
					Integer.valueOf( InputEvent.CTRL_DOWN_MASK ) );
			uninitializedMap.put( "meta",
					Integer.valueOf( InputEvent.META_DOWN_MASK ) );
			uninitializedMap.put( "alt",
					Integer.valueOf( InputEvent.ALT_DOWN_MASK ) );
			uninitializedMap.put( "altGraph",
					Integer.valueOf( InputEvent.ALT_GRAPH_DOWN_MASK ) );
			uninitializedMap.put( "button1",
					Integer.valueOf( InputEvent.BUTTON1_DOWN_MASK ) );
			uninitializedMap.put( "button2",
					Integer.valueOf( InputEvent.BUTTON2_DOWN_MASK ) );
			uninitializedMap.put( "button3",
					Integer.valueOf( InputEvent.BUTTON3_DOWN_MASK ) );
			uninitializedMap.put( DOUBLE_CLICK_TEXT,
					Integer.valueOf( DOUBLE_CLICK_MASK ) );
			uninitializedMap.put( SCROLL_TEXT,
					Integer.valueOf( SCROLL_MASK ) );
			uninitializedMap.put( WINDOWS_TEXT,
					Integer.valueOf( WIN_DOWN_MASK ) );
			modifierKeywords =
					Collections.synchronizedMap( uninitializedMap );
		}
		return modifierKeywords;
    }

	private static Map< InputTrigger, InputTrigger > cache;

	private synchronized static InputTrigger getCached( final InputTrigger buttonsAndKeys )
	{
		if ( cache == null )
			cache = new HashMap<>();

		final InputTrigger b = cache.get( buttonsAndKeys );
		if ( b == null )
		{
			cache.put( buttonsAndKeys, buttonsAndKeys );
			return buttonsAndKeys;
		}
		else
			return b;
	}
}
