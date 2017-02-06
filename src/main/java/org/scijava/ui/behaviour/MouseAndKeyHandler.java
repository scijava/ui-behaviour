/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2017 Max Planck Institute of Molecular Cell Biology
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
import java.awt.Toolkit;
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
import java.util.Map.Entry;
import java.util.Set;

import org.scijava.ui.behaviour.KeyPressedManager.KeyPressedReceiver;

import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class MouseAndKeyHandler
		implements KeyListener, MouseListener, MouseWheelListener, MouseMotionListener, FocusListener
{
	private static final int DOUBLE_CLICK_INTERVAL = getDoubleClickInterval();

	private static final int OSX_META_LEFT_CLICK = InputEvent.BUTTON1_MASK | InputEvent.BUTTON3_MASK | InputEvent.META_MASK;

	private static final int OSX_ALT_LEFT_CLICK = InputEvent.BUTTON1_MASK | InputEvent.BUTTON2_MASK | InputEvent.ALT_MASK;

	private static final int OSX_ALT_RIGHT_CLICK = InputEvent.BUTTON3_MASK | InputEvent.BUTTON2_MASK | InputEvent.ALT_MASK | InputEvent.META_MASK;

	private static int getDoubleClickInterval()
	{
		final Object prop = Toolkit.getDefaultToolkit().getDesktopProperty( "awt.multiClickInterval" );
		return prop == null ? 200 : ( Integer ) prop;
	}

	private InputTriggerMap inputMap;

	private BehaviourMap behaviourMap;

	private int inputMapExpectedModCount;

	private int behaviourMapExpectedModCount;

	public void setInputMap( final InputTriggerMap inputMap )
	{
		this.inputMap = inputMap;
		inputMapExpectedModCount = inputMap.modCount() - 1;
	}

	public void setBehaviourMap( final BehaviourMap behaviourMap )
	{
		this.behaviourMap = behaviourMap;
		behaviourMapExpectedModCount = behaviourMap.modCount() - 1;
	}

	/*
	 * Managing internal behaviour lists.
	 *
	 * The internal lists only contain entries for Behaviours that can be
	 * actually triggered with the current InputMap, grouped by Behaviour type,
	 * such that hopefully lookup from the event handlers is fast,
	 */

	static class BehaviourEntry< T extends Behaviour >
	{
		final InputTrigger buttons;

		final T behaviour;

		BehaviourEntry(
				final InputTrigger buttons,
				final T behaviour )
		{
			this.buttons = buttons;
			this.behaviour = behaviour;
		}
	}

	private final ArrayList< BehaviourEntry< DragBehaviour > > buttonDrags = new ArrayList<>();

	private final ArrayList< BehaviourEntry< DragBehaviour > > keyDrags = new ArrayList<>();

	private final ArrayList< BehaviourEntry< ClickBehaviour > > buttonClicks = new ArrayList<>();

	private final ArrayList< BehaviourEntry< ClickBehaviour > > keyClicks = new ArrayList<>();

	private final ArrayList< BehaviourEntry< ScrollBehaviour > > scrolls = new ArrayList<>();

	/**
	 * Make sure that the internal behaviour lists are up to date. For this, we
	 * keep track the modification count of {@link #inputMap} and
	 * {@link #behaviourMap}. If expected mod counts are not matched, call
	 * {@link #updateInternalMaps()} to rebuild the internal behaviour lists.
	 */
	private synchronized void update()
	{
		final int imc = inputMap.modCount();
		final int bmc = behaviourMap.modCount();
		if ( imc != inputMapExpectedModCount || bmc != behaviourMapExpectedModCount )
		{
			inputMapExpectedModCount = imc;
			behaviourMapExpectedModCount = bmc;
			updateInternalMaps();
		}
	}

	/**
	 * Build internal lists buttonDrag, keyDrags, etc from of {@link #inputMap}
	 * and {@link #behaviourMap}. The internal lists only contain entries for
	 * behaviours that can be actually triggered with the current InputMap,
	 * grouped by behaviour type, such that hopefully lookup from the event
	 * handlers is fast.
	 */
	private void updateInternalMaps()
	{
		buttonDrags.clear();
		keyDrags.clear();
		buttonClicks.clear();
		keyClicks.clear();
		scrolls.clear();

		for ( final Entry< InputTrigger, Set< String > > entry : inputMap.getAllBindings().entrySet() )
		{
			final InputTrigger buttons = entry.getKey();
			final Set< String > behaviourKeys = entry.getValue();
			if ( behaviourKeys == null )
				continue;

			for ( final String behaviourKey : behaviourKeys )
			{
				final Behaviour behaviour = behaviourMap.get( behaviourKey );
				if ( behaviour == null )
					continue;

				if ( behaviour instanceof DragBehaviour )
				{
					final BehaviourEntry< DragBehaviour > dragEntry = new BehaviourEntry<>( buttons, ( DragBehaviour ) behaviour );
					if ( buttons.isKeyTriggered() )
						keyDrags.add( dragEntry );
					else
						buttonDrags.add( dragEntry );
				}
				if ( behaviour instanceof ClickBehaviour )
				{
					final BehaviourEntry< ClickBehaviour > clickEntry = new BehaviourEntry<>( buttons, ( ClickBehaviour ) behaviour );
					if ( buttons.isKeyTriggered() )
						keyClicks.add( clickEntry );
					else
						buttonClicks.add( clickEntry );
				}
				if ( behaviour instanceof ScrollBehaviour )
				{
					final BehaviourEntry< ScrollBehaviour > scrollEntry = new BehaviourEntry<>( buttons, ( ScrollBehaviour ) behaviour );
					scrolls.add( scrollEntry );
				}
			}
		}
	}



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
			mask |= InputEvent.SHIFT_DOWN_MASK;
		else
			mask &= ~InputEvent.SHIFT_DOWN_MASK;

		/*
		 * On OS X AWT sets the META_DOWN_MASK to for right clicks. We keep
		 * track of whether the META key was actually pressed for
		 * disambiguation.
		 */
		if ( globalKeys.metaPressed() )
			mask |= InputEvent.META_DOWN_MASK;
		else
			mask &= ~InputEvent.META_DOWN_MASK;

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
				mask |= InputEvent.BUTTON1_DOWN_MASK;
			if ( ( modifiers & InputEvent.BUTTON2_MASK ) != 0 )
				mask |= InputEvent.BUTTON2_DOWN_MASK;
			if ( ( modifiers & InputEvent.BUTTON3_MASK ) != 0 )
				mask |= InputEvent.BUTTON3_DOWN_MASK;
		}

		/*
		 * On OS X AWT sets the BUTTON3_DOWN_MASK for meta+left clicks. Fix
		 * that.
		 */
		if ( modifiers == OSX_META_LEFT_CLICK )
			mask &= ~InputEvent.BUTTON3_DOWN_MASK;

		/*
		 * On OS X AWT sets the BUTTON2_DOWN_MASK for alt+left clicks. Fix
		 * that.
		 */
		if ( modifiers == OSX_ALT_LEFT_CLICK )
			mask &= ~InputEvent.BUTTON2_DOWN_MASK;

		/*
		 * On OS X AWT sets the BUTTON2_DOWN_MASK for alt+right clicks. Fix
		 * that.
		 */
		if ( modifiers == OSX_ALT_RIGHT_CLICK )
			mask &= ~InputEvent.BUTTON2_DOWN_MASK;

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
		println( "MouseAndKeyHandler.mouseDragged()" );
//		println( e );
		update();

		final int x = e.getX();
		final int y = e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : activeButtonDrags )
			drag.behaviour.drag( x, y );
	}

	@Override
	public void mouseMoved( final MouseEvent e )
	{
		println( "MouseAndKeyHandler.mouseMoved()" );
//		println( e );
		update();

		mouseX = e.getX();
		mouseY = e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : activeKeyDrags )
			drag.behaviour.drag( mouseX, mouseY );
	}

	@Override
	public void mouseWheelMoved( final MouseWheelEvent e )
	{
		println( "MouseAndKeyHandler.mouseWheelMoved()" );
//		println( e );
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
		final boolean exShiftMask = ( e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK ) != 0;
		final boolean isHorizontal = !globalKeys.shiftPressed() && exShiftMask;

		for ( final BehaviourEntry< ScrollBehaviour > scroll : scrolls )
		{
			if ( scroll.buttons.matches( mask, globalKeys.pressedKeys() ) )
			{
				scroll.behaviour.scroll( wheelRotation, isHorizontal, x, y );
			}
		}
	}

	@Override
	public void mouseClicked( final MouseEvent e )
	{
		println( "MouseAndKeyHandler.mouseClicked()" );
//		println( e );
		update();

		final int mask = getMask( e );
		final int x = e.getX();
		final int y = e.getY();

		final int clickMask = mask & ~InputTrigger.DOUBLE_CLICK_MASK;
		for ( final BehaviourEntry< ClickBehaviour > click : buttonClicks )
		{
			if ( click.buttons.matches( mask, pressedKeys ) ||
					( clickMask != mask && click.buttons.matches( clickMask, pressedKeys ) ) )
			{
				click.behaviour.click( x, y );
			}
		}
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{
		println( "MouseAndKeyHandler.mousePressed()" );
//		println( e );
		update();

		final int mask = getMask( e );
		final int x = e.getX();
		final int y = e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : buttonDrags )
		{
			if ( drag.buttons.matches( mask, globalKeys.pressedKeys() ) )
			{
				drag.behaviour.init( x, y );
				activeButtonDrags.add( drag );
			}
		}
	}

	@Override
	public void mouseReleased( final MouseEvent e )
	{
		println( "MouseAndKeyHandler.mouseReleased()" );
//		println( e );
		update();

		final int x = e.getX();
		final int y = e.getY();
		final int mask = getMask( e );

		final ArrayList< BehaviourEntry< ? > > ended = new ArrayList<>();
		for ( final BehaviourEntry< DragBehaviour > drag : activeButtonDrags )
			if ( !drag.buttons.matchesSubset( mask, globalKeys.pressedKeys() ) )
			{
				drag.behaviour.end( x, y );
				ended.add( drag );
			}
		activeButtonDrags.removeAll( ended );

	}

	@Override
	public void mouseEntered( final MouseEvent e )
	{
		println( "MouseAndKeyHandler.mouseEntered()" );
		update();
		if ( keypressManager != null )
			keypressManager.activate( receiver );
	}

	@Override
	public void mouseExited( final MouseEvent e )
	{
		println( "MouseAndKeyHandler.mouseExited()" );
		update();
		if ( keypressManager != null )
			keypressManager.deactivate( receiver );
	}

	@Override
	public void keyPressed( final KeyEvent e )
	{
		println( "MouseAndKeyHandler.keyPressed()" );
//		println( e );

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
				keypressManager.handleKeyPressed( receiver, mask, doubleClick, pressedKeys );
			else
				handleKeyPressed( mask, doubleClick, pressedKeys, false );
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
			public void handleKeyPressed( final int mask, final boolean doubleClick, final TIntSet pressedKeys )
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
					( drag.buttons.matches( mask, pressedKeys ) ||
					( doubleClick && drag.buttons.matches( doubleClickMask, pressedKeys ) ) ) )
			{
				if ( dryRun )
					return true;
				triggered = true;
				drag.behaviour.init( mouseX, mouseY );
				activeKeyDrags.add( drag );
			}
		}

		for ( final BehaviourEntry< ClickBehaviour > click : keyClicks )
		{
			if ( click.buttons.matches( mask, pressedKeys ) ||
					( doubleClick && click.buttons.matches( doubleClickMask, pressedKeys ) ) )
			{
				if ( dryRun )
					return true;
				triggered = true;
				click.behaviour.click( mouseX, mouseY );
			}
		}

		return triggered;
	}

	@Override
	public void keyReleased( final KeyEvent e )
	{
		println( "MouseAndKeyHandler.keyReleased()" );
//		println( e );
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
				if ( !drag.buttons.matchesSubset( mask, pressedKeys ) )
				{
					drag.behaviour.end( mouseX, mouseY );
					ended.add( drag );
				}
			activeKeyDrags.removeAll( ended );
		}
	}

	@Override
	public void keyTyped( final KeyEvent e )
	{
		println( "MouseAndKeyHandler.keyTyped()" );
//		println( e );
	}

	@Override
	public void focusGained( final FocusEvent e )
	{
		println( "MouseAndKeyHandler.focusGained()" );
		pressedKeys.clear();
		pressedKeys.addAll( globalKeys.pressedKeys() );
	}

	@Override
	public void focusLost( final FocusEvent e )
	{
		println( "MouseAndKeyHandler.focusLost()" );
		pressedKeys.clear();
	}

	private void debugMask( final InputEvent e )
	{
		final int modifiers = e.getModifiers();
		final int modifiersEx = e.getModifiersEx();
		final int mask = getMask( e );
		println( "  modifiers:   " + binary( modifiers ) );
		println( "  modifiersEx: " + binary( modifiersEx ) );
		println( "  mask:        " + binary( mask ) );
	}

	private String binary( final int value )
	{
		return String.format("%32s", Integer.toBinaryString( value ) ).replace(' ', '0');
	}

	private boolean DEBUG = false;

	private String debugPrefix = "";

	private void println( final Object string )
	{
		if ( DEBUG )
		{
			System.out.println( debugPrefix + string );
		}
	}

	public void setDebug( final boolean b )
	{
		DEBUG = b;
	}

	public void setDebugPrefix( final String string )
	{
		debugPrefix = string;
	}
}
