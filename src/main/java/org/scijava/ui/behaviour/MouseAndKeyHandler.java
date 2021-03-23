/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2021 Max Planck Institute of Molecular Cell Biology
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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

import org.scijava.ui.behaviour.KeyPressedManager.KeyPressedReceiver;

import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class MouseAndKeyHandler extends AbstractMouseAndKeyHandler
		implements KeyListener, MouseListener, MouseWheelListener, MouseMotionListener, FocusListener
{
	private static final int OSX_META_LEFT_CLICK = InputEvent.BUTTON1_MASK | InputEvent.BUTTON3_MASK | InputEvent.META_MASK;

	private static final int OSX_ALT_LEFT_CLICK = InputEvent.BUTTON1_MASK | InputEvent.BUTTON2_MASK | InputEvent.ALT_MASK;

	private static final int OSX_ALT_RIGHT_CLICK = InputEvent.BUTTON3_MASK | InputEvent.BUTTON2_MASK | InputEvent.ALT_MASK | InputEvent.META_MASK;

	/*
	 * Event handling. Forwards to registered behaviours.
	 */

	private final GlobalKeyEventDispatcher globalKeys = GlobalKeyEventDispatcher.getInstance();

	/**
	 * Which keys are currently pressed. This does not include modifier keys
	 * Control, Shift, Alt, AltGr, Meta, Win.
	 */
	private final TIntSet pressedKeys = new TIntHashSet( 5, 0.5f, -1 );

	/**
	 * When keys where pressed
	 */
	private final TIntLongHashMap keyPressTimes = new TIntLongHashMap( 100, 0.5f, -1, -1 );

	/**
	 * The current mouse coordinates, updated through {@link #mouseMoved(MouseEvent)}.
	 */
	private int mouseX;

	/**
	 * The current mouse coordinates, updated through {@link #mouseMoved(MouseEvent)}.
	 */
	private int mouseY;

	/**
	 * Active {@link DragBehaviour}s initiated by mouse button press.
	 */
	private final ArrayList< BehaviourEntry< DragBehaviour > > activeButtonDrags = new ArrayList<>();

	/**
	 * Active {@link DragBehaviour}s initiated by key press.
	 */
	private final ArrayList< BehaviourEntry< DragBehaviour > > activeKeyDrags = new ArrayList<>();

	private int getMask( final InputEvent e )
	{
		final int modifiers = e.getModifiers();
		final int modifiersEx = e.getModifiersEx();
		int mask = modifiersEx;

		/*
		 * For scrolling AWT uses the SHIFT_DOWN_MASK to indicate horizontal scrolling.
		 * We keep track of whether the SHIFT key was actually pressed for disambiguation.
		 */
		if ( globalKeys.shiftPressed() )
			mask |= InputTrigger.SHIFT_DOWN_MASK;
		else
			mask &= ~InputTrigger.SHIFT_DOWN_MASK;

		/*
		 * On OS X AWT sets the META_DOWN_MASK to for right clicks. We keep
		 * track of whether the META key was actually pressed for
		 * disambiguation.
		 */
		if ( globalKeys.metaPressed() )
			mask |= InputTrigger.META_DOWN_MASK;
		else
			mask &= ~InputTrigger.META_DOWN_MASK;

		if ( globalKeys.winPressed() )
			mask |= InputTrigger.WIN_DOWN_MASK;

		/*
		 * We add the button modifiers to modifiersEx such that the
		 * XXX_DOWN_MASK can be used as the canonical flag. E.g. we adapt
		 * modifiersEx such that BUTTON1_DOWN_MASK is also present in
		 * mouseClicked() when BUTTON1 was clicked (although the button is no
		 * longer down at this point).
		 *
		 * ...but only if its not a MOUSE_WHEEL because OS X sets button
		 * modifiers if ALT or META modifiers are pressed.
		 *
		 * ...and also only if its not a MOUSE_RELEASED. Otherwise we will not
		 * be able to detect drag-end because the mask would still match the
		 * drag trigger.
		 */
		if ( e.getID() != MouseEvent.MOUSE_WHEEL && e.getID() != MouseEvent.MOUSE_RELEASED )
		{
			if ( ( modifiers & InputEvent.BUTTON1_MASK ) != 0 )
				mask |= InputTrigger.BUTTON1_DOWN_MASK;
			if ( ( modifiers & InputEvent.BUTTON2_MASK ) != 0 )
				mask |= InputTrigger.BUTTON2_DOWN_MASK;
			if ( ( modifiers & InputEvent.BUTTON3_MASK ) != 0 )
				mask |= InputTrigger.BUTTON3_DOWN_MASK;
		}

		/*
		 * On OS X AWT sets the BUTTON3_DOWN_MASK for meta+left clicks. Fix
		 * that.
		 */
		if ( modifiers == OSX_META_LEFT_CLICK )
			mask &= ~InputTrigger.BUTTON3_DOWN_MASK;

		/*
		 * On OS X AWT sets the BUTTON2_DOWN_MASK for alt+left clicks. Fix
		 * that.
		 */
		if ( modifiers == OSX_ALT_LEFT_CLICK )
			mask &= ~InputTrigger.BUTTON2_DOWN_MASK;

		/*
		 * On OS X AWT sets the BUTTON2_DOWN_MASK for alt+right clicks. Fix
		 * that.
		 */
		if ( modifiers == OSX_ALT_RIGHT_CLICK )
			mask &= ~InputTrigger.BUTTON2_DOWN_MASK;

		/*
		 * Deal with mouse double-clicks.
		 */

		if ( e instanceof MouseEvent && ( ( MouseEvent ) e ).getClickCount() > 1 )
			mask |= InputTrigger.DOUBLE_CLICK_MASK; // mouse

		if ( e instanceof MouseWheelEvent )
			mask |= InputTrigger.SCROLL_MASK;

		return mask;
	}



	/*
	 * KeyListener, MouseListener, MouseWheelListener, MouseMotionListener.
	 */


	@Override
	public void mouseDragged( final MouseEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.mouseDragged()" );
//		System.out.println( e );
		update();

		mouseX = e.getX();
		mouseY = e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : activeButtonDrags )
			drag.behaviour().drag( mouseX, mouseY );
	}

	@Override
	public void mouseMoved( final MouseEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.mouseMoved()" );
		update();

		mouseX = e.getX();
		mouseY = e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : activeKeyDrags )
			drag.behaviour().drag( mouseX, mouseY );
	}

	@Override
	public void mouseWheelMoved( final MouseWheelEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.mouseWheelMoved()" );
//		System.out.println( e );
		update();

		final int mask = getMask( e );
		final int x = e.getX();
		final int y = e.getY();
		final double wheelRotation = e.getPreciseWheelRotation();

		/*
		 * AWT uses the SHIFT_DOWN_MASK to indicate horizontal scrolling. We
		 * keep track of whether the SHIFT key was actually pressed for
		 * disambiguation. However, we can only detect horizontal scrolling if
		 * the SHIFT key is not pressed. With SHIFT pressed, everything is
		 * treated as vertical scrolling.
		 */
		final boolean exShiftMask = ( e.getModifiersEx() & InputTrigger.SHIFT_DOWN_MASK ) != 0;
		final boolean isHorizontal = !globalKeys.shiftPressed() && exShiftMask;

		for ( final BehaviourEntry< ScrollBehaviour > scroll : scrolls )
		{
			if ( scroll.buttons().matches( mask, globalKeys.pressedKeys() ) )
			{
				scroll.behaviour().scroll( wheelRotation, isHorizontal, x, y );
			}
		}
	}

	@Override
	public void mouseClicked( final MouseEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.mouseClicked()" );
//		System.out.println( e );
		update();

		final int mask = getMask( e );
		final int x = e.getX();
		final int y = e.getY();

		final int clickMask = mask & ~InputTrigger.DOUBLE_CLICK_MASK;
		for ( final BehaviourEntry< ClickBehaviour > click : buttonClicks )
		{
			if ( click.buttons().matches( mask, globalKeys.pressedKeys() ) ||
					( clickMask != mask && click.buttons().matches( clickMask, globalKeys.pressedKeys() ) ) )
			{
				click.behaviour().click( x, y );
			}
		}
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.mousePressed()" );
//		System.out.println( e );
		update();

		final int mask = getMask( e );
		final int x = e.getX();
		final int y = e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : buttonDrags )
		{
			if ( drag.buttons().matches( mask, globalKeys.pressedKeys() ) )
			{
				drag.behaviour().init( x, y );
				activeButtonDrags.add( drag );
			}
		}
	}

	@Override
	public void mouseReleased( final MouseEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.mouseReleased()" );
//		System.out.println( e );
		update();

		final int x = e.getX();
		final int y = e.getY();
		final int mask = getMask( e );

		final ArrayList< BehaviourEntry< ? > > ended = new ArrayList<>();
		for ( final BehaviourEntry< DragBehaviour > drag : activeButtonDrags )
			if ( !drag.buttons().matchesSubset( mask, globalKeys.pressedKeys() ) )
			{
				drag.behaviour().end( x, y );
				ended.add( drag );
			}
		activeButtonDrags.removeAll( ended );

	}

	@Override
	public void mouseEntered( final MouseEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.mouseEntered()" );
		update();
		if ( keypressManager != null )
			keypressManager.activate( receiver );
	}

	@Override
	public void mouseExited( final MouseEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.mouseExited()" );
		update();
		if ( keypressManager != null )
			keypressManager.deactivate( receiver );
	}

	@Override
	public void keyPressed( final KeyEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.keyPressed()" );
//		System.out.println( e );
		update();

		if (	e.getKeyCode() != 0 &&
				e.getKeyCode() != KeyEvent.VK_SHIFT &&
				e.getKeyCode() != KeyEvent.VK_META &&
				e.getKeyCode() != KeyEvent.VK_WINDOWS &&
				e.getKeyCode() != KeyEvent.VK_ALT &&
				e.getKeyCode() != KeyEvent.VK_CONTROL &&
				e.getKeyCode() != KeyEvent.VK_ALT_GRAPH )
		{
			final boolean inserted = pressedKeys.add( e.getKeyCode() );

			/*
			 * Create mask and deal with double-click on keys.
			 */

			final int mask = getMask( e );
			boolean doubleClick = false;
			if ( inserted )
			{
				// double-click on keys.
				final long lastPressTime = keyPressTimes.get( e.getKeyCode() );
				if ( lastPressTime != -1 && ( e.getWhen() - lastPressTime ) < DOUBLE_CLICK_INTERVAL )
					doubleClick = true;

				keyPressTimes.put( e.getKeyCode(), e.getWhen() );
			}

			if ( keypressManager != null )
				keypressManager.handleKeyPressed( receiver, mask, doubleClick, globalKeys.pressedKeys() );
			else
				handleKeyPressed( mask, doubleClick, globalKeys.pressedKeys(), false );
		}
	}

	/**
	 * If non-null, {@link #keyPressed(KeyEvent)} events are forwarded to the
	 * {@link KeyPressedManager} which in turn forwards to the
	 * {@link KeyPressedReceiver} of the component currently under the mouse.
	 * (This requires that the other component is also registered with the
	 * {@link KeyPressedManager}.
	 */
	private KeyPressedManager keypressManager = null;

	/**
	 * Represents this {@link MouseAndKeyHandler} to the {@link #keypressManager}.
	 */
	private KeyPressedReceiver receiver = null;

	/**
	 * @param keypressManager
	 * @param focus
	 *            function that ensures that the component associated to this
	 *            {@link MouseAndKeyHandler} is focused.
	 */
	public void setKeypressManager(
			final KeyPressedManager keypressManager,
			final Runnable focus )
	{
		this.keypressManager = keypressManager;
		this.receiver = new KeyPressedReceiver()
		{
			@Override
			public void handleKeyPressed( final KeyPressedReceiver origin, final int mask, final boolean doubleClick, final TIntSet pressedKeys )
			{
				if ( MouseAndKeyHandler.this.handleKeyPressed( mask, doubleClick, pressedKeys, true ) )
					focus.run();
				MouseAndKeyHandler.this.handleKeyPressed( mask, doubleClick, pressedKeys, false );
			}
		};
	}

	/**
	 * @param keypressManager
	 * @param focusableOwner
	 *            container of this {@link MouseAndKeyHandler}. If key presses
	 *            are forwarded from the {@link KeyPressedManager} while the
	 *            component does not have focus, then
	 *            {@link Component#requestFocus()}.
	 */
	public void setKeypressManager(
			final KeyPressedManager keypressManager,
			final Component focusableOwner )
	{
		setKeypressManager( keypressManager, () -> {
			if ( !focusableOwner.isFocusOwner() )
			{
//				focusableOwner.requestFocusInWindow();
				focusableOwner.requestFocus();
			}
		} );
	}

	private boolean handleKeyPressed( final int mask, final boolean doubleClick, final TIntSet pressedKeys, final boolean dryRun )
	{
		update();

		final int doubleClickMask = mask | InputTrigger.DOUBLE_CLICK_MASK;

		boolean triggered = false;

		for ( final BehaviourEntry< DragBehaviour > drag : keyDrags )
		{
			if ( !activeKeyDrags.contains( drag ) &&
					( drag.buttons().matches( mask, pressedKeys ) ||
							( doubleClick && drag.buttons().matches( doubleClickMask, pressedKeys ) ) ) )
			{
				if ( dryRun )
					return true;
				triggered = true;
				drag.behaviour().init( mouseX, mouseY );
				activeKeyDrags.add( drag );
			}
		}

		for ( final BehaviourEntry< ClickBehaviour > click : keyClicks )
		{
			if ( click.buttons().matches( mask, pressedKeys ) ||
					( doubleClick && click.buttons().matches( doubleClickMask, pressedKeys ) ) )
			{
				if ( dryRun )
					return true;
				triggered = true;
				click.behaviour().click( mouseX, mouseY );
			}
		}

		return triggered;
	}

	@Override
	public void keyReleased( final KeyEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.keyReleased()" );
//		System.out.println( e );
		update();

		if (	e.getKeyCode() != 0 &&
				e.getKeyCode() != KeyEvent.VK_SHIFT &&
				e.getKeyCode() != KeyEvent.VK_META &&
				e.getKeyCode() != KeyEvent.VK_WINDOWS &&
				e.getKeyCode() != KeyEvent.VK_ALT &&
				e.getKeyCode() != KeyEvent.VK_CONTROL &&
				e.getKeyCode() != KeyEvent.VK_ALT_GRAPH )
		{
			pressedKeys.remove( e.getKeyCode() );
			final int mask = getMask( e );

			final ArrayList< BehaviourEntry< ? > > ended = new ArrayList<>();
			for ( final BehaviourEntry< DragBehaviour > drag : activeKeyDrags )
				if ( !drag.buttons().matchesSubset( mask, globalKeys.pressedKeys() ) )
				{
					drag.behaviour().end( mouseX, mouseY );
					ended.add( drag );
				}
			activeKeyDrags.removeAll( ended );
		}
	}

	@Override
	public void keyTyped( final KeyEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.keyTyped()" );
//		System.out.println( e );
	}

	@Override
	public void focusGained( final FocusEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.focusGained()" );
		pressedKeys.clear();
		pressedKeys.addAll( globalKeys.pressedKeys() );
	}

	@Override
	public void focusLost( final FocusEvent e )
	{
//		System.out.println( "MouseAndKeyHandler.focusLost()" );
		pressedKeys.clear();
	}
}
