/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2022 Max Planck Institute of Molecular Cell Biology
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

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Global {@link KeyEventDispatcher} hook that allows to share state of modifier
 * keys between {@link MouseAndKeyHandler} in different windows of the
 * application.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class GlobalKeyEventDispatcher implements KeyEventDispatcher
{
	public static GlobalKeyEventDispatcher getInstance()
	{
		install();
		return instance;
	}

	/**
	 * Which keys are currently pressed. This does not include modifier keys
	 * Control, Shift, Alt, AltGr, Meta.
	 */
	private final TIntSet pressedKeys = new TIntHashSet( 5, 0.5f, -1 );

	/**
	 * Whether the SHIFT key is currently pressed. We need this, because for
	 * mouse-wheel AWT uses the SHIFT_DOWN_MASK to indicate horizontal
	 * scrolling. We keep track of whether the SHIFT key was actually pressed
	 * for disambiguation.
	 */
	private boolean shiftPressed = false;

	/**
	 * Whether the META key is currently pressed. We need this, because on OS X
	 * AWT sets the META_DOWN_MASK to for right clicks. We keep track of whether
	 * the META key was actually pressed for disambiguation.
	 */
	private boolean metaPressed = false;

	/**
	 * Whether the WINDOWS key is currently pressed.
	 */
	private boolean winPressed = false;

	public TIntSet pressedKeys()
	{
		return pressedKeys;
	}

	public boolean shiftPressed()
	{
		return shiftPressed;
	}

	public boolean metaPressed()
	{
		return metaPressed;
	}

	public boolean winPressed()
	{
		return winPressed;
	}

	@Override
	public boolean dispatchKeyEvent( final KeyEvent e )
	{
		final int id = e.getID();
		if ( id == KeyEvent.KEY_PRESSED )
		{
			if ( e == probeEvent )
			{
				probeInstalled = true;
				return true;
			}
			else
				keyPressed( e );
		}
		else if ( id == KeyEvent.KEY_RELEASED )
			keyReleased( e );
		return false;
	}

	private void keyPressed( final KeyEvent e )
	{
		if ( e.getKeyCode() == KeyEvent.VK_SHIFT )
		{
			shiftPressed = true;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_META )
		{
			metaPressed = true;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_WINDOWS )
		{
			winPressed = true;
		}
		else if (
				e.getKeyCode() != 0 &&
				e.getKeyCode() != KeyEvent.VK_ALT &&
				e.getKeyCode() != KeyEvent.VK_CONTROL &&
				e.getKeyCode() != KeyEvent.VK_ALT_GRAPH )
		{
			pressedKeys.add( e.getKeyCode() );
		}
	}

	private void keyReleased( final KeyEvent e )
	{
		if ( e.getKeyCode() == KeyEvent.VK_SHIFT )
		{
			shiftPressed = false;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_META )
		{
			metaPressed = false;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_WINDOWS )
		{
			winPressed = false;
		}
		else if (
				e.getKeyCode() != 0 &&
				e.getKeyCode() != KeyEvent.VK_ALT &&
				e.getKeyCode() != KeyEvent.VK_CONTROL &&
				e.getKeyCode() != KeyEvent.VK_ALT_GRAPH )
		{
			pressedKeys.remove( e.getKeyCode() );
		}
	}

	private GlobalKeyEventDispatcher()
	{}

	private static final GlobalKeyEventDispatcher instance = new GlobalKeyEventDispatcher();

	/**
	 * This is an event we send to the
	 * {@link KeyboardFocusManager#getCurrentKeyboardFocusManager()} to see if
	 * we will receive it. Then we know that we're installed...
	 */
	private static KeyEvent probeEvent = new KeyEvent( new Component() {}, KeyEvent.KEY_PRESSED, 0, 0, 0, 'a' );

	private static boolean probeInstalled = false;

	private final static PropertyChangeListener clearOnApplicationFocusGained = new PropertyChangeListener()
	{
		private long focuslosttime;

		@Override
		public void propertyChange( final PropertyChangeEvent evt )
		{
			final long t = System.currentTimeMillis();
			if ( evt.getNewValue() == null )
				focuslosttime = t;
			else if ( evt.getOldValue() == null &&  t - focuslosttime > 3 )
			{
				instance.shiftPressed = false;
				instance.metaPressed = false;
				instance.winPressed = false;
				instance.pressedKeys.clear();
			}
		}
	};

	/**
	 * Install a {@link GlobalKeyEventDispatcher} into the current
	 * {@link KeyboardFocusManager}, if it is not installed yet.
	 */
	private static void install()
	{
		final KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		probeInstalled = false;
		focusManager.dispatchEvent( probeEvent );
		if ( !probeInstalled )
		{
			focusManager.addKeyEventDispatcher( instance );
			focusManager.addPropertyChangeListener( "activeWindow", clearOnApplicationFocusGained );
		}
	}
}
