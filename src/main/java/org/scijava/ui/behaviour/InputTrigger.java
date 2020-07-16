/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2020 Max Planck Institute of Molecular Cell Biology
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
	public static final int SHIFT_DOWN_MASK     = 1 << 6;  // == InputEvent.SHIFT_DOWN_MASK
	public static final int CTRL_DOWN_MASK      = 1 << 7;  // == InputEvent.CTRL_DOWN_MASK
	public static final int META_DOWN_MASK      = 1 << 8;  // == InputEvent.META_DOWN_MASK
	public static final int ALT_DOWN_MASK       = 1 << 9;  // == InputEvent.ALT_DOWN_MASK
	public static final int BUTTON1_DOWN_MASK   = 1 << 10; // == InputEvent.BUTTON1_DOWN_MASK
	public static final int BUTTON2_DOWN_MASK   = 1 << 11; // == InputEvent.BUTTON2_DOWN_MASK
	public static final int BUTTON3_DOWN_MASK   = 1 << 12; // == InputEvent.BUTTON3_DOWN_MASK
	public static final int ALT_GRAPH_DOWN_MASK = 1 << 13; // == InputEvent.ALT_GRAPH_DOWN_MASK

	public static final int DOUBLE_CLICK_MASK   = 1 << 20;
	public static final int SCROLL_MASK         = 1 << 21;
	public static final int WIN_DOWN_MASK       = 1 << 22;

	public static final int IGNORE_MASK = -1;

	private static final TIntSet emptySet = new TIntHashSet();

	public static final InputTrigger NOT_MAPPED = new InputTrigger( IGNORE_MASK, emptySet, null, false, IGNORE_MASK, emptySet );

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

	/**
	 * String used to specify a special {@code NOT_MAPPED} trigger that blocks
	 * all triggers for an action.
	 */
	private static final String NOT_MAPPED_TEXT = "not mapped";

	private static final String IGNORE_ALL_TEXT = "all";

	private final int mask;

	private final TIntSet pressedKeys;

	private final KeyStroke keyStroke;

	/**
	 * No modifiers or keys will be ignored. Only exact combination of
	 * {@link #mask} and {@link #pressedKeys} matches.
	 */
	private final boolean ignoreNone;

	/**
	 * Ignore all additional mask bits and pressed keys when matching this
	 * trigger.
	 */
	private final boolean ignoreAll;

	/**
	 * Additional mask bits that are ignored when matching this trigger. Only
	 * considered if {@code ignoreAll == false} and {@code ignoreNone == false}.
	 */
	private final int ignoreMask;

	/**
	 * Additional keys that may be pressed when matching this trigger. Only
	 * considered if {@code ignoreAll == false} and {@code ignoreNone == false}.
	 */
	private final TIntSet ignoreKeys;

	/**
	 * {@code effectiveIgnoreKeys == ignoreKeys \ pressedKeys}
	 */
	private final TIntSet effectiveIgnoreKeys;

	private final int hashcode;

	public static InputTrigger getFromString( final String s ) throws IllegalArgumentException
	{
		if ( s == null || s.length() == 0 )
			return null;

		if ( s.equals( NOT_MAPPED_TEXT ) )
			return NOT_MAPPED;

		try
		{
			final String[] split = splitAndTrim( s );
			final String triggerdef = split[ 0 ];
			final String ignoredef = split.length > 1 ? split[ 1 ] : null;

			MaskAndKeys mak = getMaskAndKeysFromString( triggerdef );
			final int mask = mak.mask;
			final TIntSet pressedKeys = mak.pressedKeys;

			/*
			 * TODO: KeyStroke only if no ignore keys are given????
			 */
			KeyStroke keyStroke = null;
			if ( ( mask & ( DOUBLE_CLICK_MASK | BUTTON1_DOWN_MASK | BUTTON2_DOWN_MASK | BUTTON3_DOWN_MASK ) ) == 0 )
			{
				// no mouse keys, no double-click -- pure keystroke
				// This might still fail if for example "win" modifier is present.
				// In that case KeyStroke.getKeyStroke( s ) == null.
				keyStroke = KeyStroke.getKeyStroke( triggerdef );
			}

			// is there a definition of keys to ignore?
			int ignoreMask = 0;
			TIntSet ignoreKeys = emptySet;
			boolean ignoreAll = false;
			if ( ignoredef != null )
			{
				if ( ignoredef.equals( IGNORE_ALL_TEXT ) )
				{
					ignoreAll = true;
				}
				else
				{
					mak = getMaskAndKeysFromString( ignoredef );
					ignoreMask = mak.mask;
					ignoreKeys = mak.pressedKeys;
				}
			}

			// don't keep identical (wrt. equals()) InputTrigger instance around.
			return getCached( new InputTrigger( mask, pressedKeys, keyStroke, ignoreAll, ignoreMask, ignoreKeys ) );
		}
		catch ( final IllegalArgumentException e )
		{
			throw new IllegalArgumentException( "InputTrigger String \"" + s + "\" is formatted incorrectly" );
		}
	}

	private static String[] splitAndTrim( final String s ) throws IllegalArgumentException
	{
		final String[] split = s.split( "\\|" );
		if ( split.length < 1 || split.length > 2 )
			throw new IllegalArgumentException();
		for ( int i = 0; i < split.length; ++i )
		{
			split[ i ] = split[ i ].trim();
			if ( split[ i ].length() == 0 )
				throw new IllegalArgumentException();
		}
		return split;
	}

	private static class MaskAndKeys
	{
		int mask;

		TIntSet pressedKeys;

		public MaskAndKeys( final int mask, final TIntSet pressedKeys )
		{
			this.mask = mask;
			this.pressedKeys = pressedKeys;
		}
	}

	private static MaskAndKeys getMaskAndKeysFromString( final String s ) throws IllegalArgumentException
	{
		if ( s == null || s.length() == 0 )
			return null;

		final StringTokenizer st = new StringTokenizer( s, " " );
		final Map< String, Integer > modifierKeywords = getModifierKeywords();

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
						throw new IllegalArgumentException();
					pressedKeys.add( ks.getKeyCode() );
				}
			}
		}

		return new MaskAndKeys( mask, pressedKeys );
	}

	private InputTrigger(
			final int mask,
			final TIntSet pressedKeys,
			final KeyStroke keyStroke,
			final boolean ignoreAll,
			final int ignoreMask,
			final TIntSet ignoreKeys )
	{
		this.mask = mask;
		this.pressedKeys = pressedKeys;
		this.keyStroke = keyStroke;
		this.ignoreNone = !ignoreAll && ignoreMask == 0 && ignoreKeys.isEmpty();
		this.ignoreAll = ignoreAll;
		this.ignoreMask = ignoreMask;
		this.ignoreKeys = ignoreKeys;

		// TODO add flag that says: nothing is ignored and then only do the default check in matches()

		if ( ignoreAll )
		{
			effectiveIgnoreKeys = null;
		}
		else
		{
			effectiveIgnoreKeys = new TIntHashSet( ignoreKeys );
			effectiveIgnoreKeys.removeAll( pressedKeys );
		}

		int value = 17;
		value = 31 * value + mask;
		value = 31 * value + pressedKeys.hashCode();
		value = 31 * value + ignoreMask;
		value = 31 * value + ignoreKeys.hashCode();
		value += ignoreAll ? 1 : 0;
		hashcode = value;
	}

//	TODO: remove
//	private InputTrigger( final int mask, final TIntSet pressedKeys, final KeyStroke keyStroke )
//	{
//		this.mask = mask;
//		this.pressedKeys = pressedKeys;
//		this.keyStroke = keyStroke;
//
//		int value = 17;
//		value = 31 * value + mask;
//		value = 31 * value + pressedKeys.hashCode();
//		hashcode = value;
//	}

	/**
	 * Get the modifier mask for this trigger. The mask can have the following
	 * bits:
	 * <ul>
	 * <li>{@link InputEvent#SHIFT_DOWN_MASK}</li>
	 * <li>{@link InputEvent#CTRL_DOWN_MASK}</li>
	 * <li>{@link InputEvent#CTRL_DOWN_MASK}</li>
	 * <li>{@link InputEvent#META_DOWN_MASK}</li>
	 * <li>{@link InputEvent#ALT_DOWN_MASK}</li>
	 * <li>{@link InputEvent#ALT_GRAPH_DOWN_MASK}</li>
	 * <li>{@link InputEvent#BUTTON1_DOWN_MASK}</li>
	 * <li>{@link InputEvent#BUTTON2_DOWN_MASK}</li>
	 * <li>{@link InputEvent#BUTTON3_DOWN_MASK}</li>
	 * <li>{@link #DOUBLE_CLICK_MASK}</li>
	 * <li>{@link #SCROLL_MASK}</li>
	 * <li>{@link #WIN_DOWN_MASK}</li>
	 * </ul>
	 *
	 * @return get the modifier mask for this trigger.
	 */
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
		return ( mask & ( BUTTON1_DOWN_MASK |  BUTTON2_DOWN_MASK | BUTTON3_DOWN_MASK | SCROLL_MASK ) ) == 0;
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
		return matches( mask, keys, new TIntHashSet() );
	}

	public boolean matches( final int mask, final TIntSet keys, final TIntSet tmp )
	{
		// C = currently pressed keys
		// T = trigger keys (expected to be pressed)
		// I = ignored keys (not expected necessarily, but maybe present)
		//
		// T == C\(I\T) = C & ~(I & ~T)
		// expected keys == currently pressed keys with those removed that are
		// not expected (but may be present)
		//
		// C = mask, I = this.ignoreMask, T = this.mask
		// C = keys, I = this.ignoreKeys, T = this.pressedKeys

		if ( ignoreNone )
		{
			if ( this.mask != mask )
				return false;
			else
				return keys.equals( pressedKeys );
		}
		if ( ignoreAll )
		{
			return matchesSubset( mask, keys );
		}
		else
		{
			if ( this.mask != ( mask & ( ~this.ignoreMask | this.mask ) ) )
				return false;
			else
			{
				final TIntSet effectiveKeys = tmp;
				effectiveKeys.clear();
				effectiveKeys.addAll( keys );
				effectiveKeys.removeAll( effectiveIgnoreKeys );
				return pressedKeys.equals( effectiveKeys );
			}
		}
	}

	public boolean matchesSubset( final int mask, final TIntSet keys )
	{
		return matchesSubset( mask, keys, new TIntHashSet() );
	}

	public boolean matchesSubset( final int mask, final TIntSet keys, final TIntSet tmp )
	{
		// C = currently pressed keys
		// T = trigger keys (expected to be pressed)
		//
		// T == T & C
		//
		// C = mask, T = this.mask
		// C = keys, T = this.pressedKeys

		if ( this.mask != ( mask & this.mask ) )
			return false;
		else
		{
			final TIntSet effectiveKeys = tmp;
			effectiveKeys.clear();
			effectiveKeys.addAll( keys );
			effectiveKeys.retainAll( pressedKeys );
			return pressedKeys.equals( effectiveKeys );
		}
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
		return mask == o.mask
				&& ignoreMask == o.ignoreMask
				&& pressedKeys.equals( o.pressedKeys )
				&& ignoreKeys.equals( o.ignoreKeys )
				&& ignoreAll == o.ignoreAll;
	}

	/*
	 * String representation. toString() is also used for writing to YAML and JSON files.
	 */

    @Override
	public String toString()
    {
		if ( NOT_MAPPED.equals( this ) )
			return NOT_MAPPED_TEXT;

		final StringBuilder buf = new StringBuilder();

		addModifierTexts( mask, buf );
		addKeys( pressedKeys, buf );
		if ( !ignoreNone )
		{
			buf.append( " |" );
			if ( ignoreAll )
			{
				buf.append( " " );
				buf.append( IGNORE_ALL_TEXT );
			}
			else
			{
				addModifierTexts( ignoreMask, buf );
				addKeys( ignoreKeys, buf );
			}
		}

		return buf.toString();
    }

    private void addKeys( final TIntSet keys, final StringBuilder buf )
    {
		final TIntIterator iter = keys.iterator();
		while ( iter.hasNext() )
		{
			final int key = iter.next();
			if ( buf.length() > 0 )
				buf.append( " " );
			final String vkName = KeyStroke.getKeyStroke( key, 0 ).toString();
			buf.append( vkName.replace( "pressed ", "" ) );
		}
    }

    private void addModifierTexts( final int mask, final StringBuilder buf )
    {
		addModifierText( mask, SHIFT_DOWN_MASK, "shift", buf );
		addModifierText( mask, CTRL_DOWN_MASK, "ctrl", buf );
		addModifierText( mask, META_DOWN_MASK, "meta", buf );
		addModifierText( mask, ALT_DOWN_MASK, "alt", buf );
		addModifierText( mask, ALT_GRAPH_DOWN_MASK, "altGraph", buf );
		addModifierText( mask, BUTTON1_DOWN_MASK, "button1", buf );
		addModifierText( mask, BUTTON2_DOWN_MASK, "button2", buf );
		addModifierText( mask, BUTTON3_DOWN_MASK, "button3", buf );
		addModifierText( mask, DOUBLE_CLICK_MASK, DOUBLE_CLICK_TEXT, buf );
		addModifierText( mask, SCROLL_MASK, SCROLL_TEXT, buf );
		addModifierText( mask, WIN_DOWN_MASK, WINDOWS_TEXT, buf );
    }

    private void addModifierText( final int mask, final int flag, final String name, final StringBuilder buf )
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
					SHIFT_DOWN_MASK );
			uninitializedMap.put( "control",
					CTRL_DOWN_MASK );
			uninitializedMap.put( "ctrl",
					CTRL_DOWN_MASK );
			uninitializedMap.put( "meta",
					META_DOWN_MASK );
			uninitializedMap.put( "alt",
					ALT_DOWN_MASK );
			uninitializedMap.put( "altGraph",
					ALT_GRAPH_DOWN_MASK );
			uninitializedMap.put( "button1",
					BUTTON1_DOWN_MASK );
			uninitializedMap.put( "button2",
					BUTTON2_DOWN_MASK );
			uninitializedMap.put( "button3",
					BUTTON3_DOWN_MASK );
			uninitializedMap.put( DOUBLE_CLICK_TEXT,
					DOUBLE_CLICK_MASK );
			uninitializedMap.put( SCROLL_TEXT,
					SCROLL_MASK );
			uninitializedMap.put( WINDOWS_TEXT,
					WIN_DOWN_MASK );
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
