package bdv.behaviour;

import java.awt.Toolkit;
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

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;


public class MouseAndKeyHandler
	implements KeyListener, MouseListener, MouseWheelListener, MouseMotionListener
{
	private static final int DOUBLE_CLICK_INTERVAL = getDoubleClickInterval();

	private static final int OSX_META_LEFT_CLICK = InputEvent.BUTTON1_MASK | InputEvent.BUTTON3_MASK | InputEvent.META_MASK;

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
	 * Build internal lists buttonDrag, keyDrags, etc from BehaviourMap(?) and
	 * InputMap(?). The internal lists only contain entries for Behaviours that
	 * can be actually triggered with the current InputMap, grouped by Behaviour
	 * type, such that hopefully lookup from the event handlers is fast.
	 */
	private void updateInternalMaps()
	{
		buttonDrags.clear();
		keyDrags.clear();
		buttonClicks.clear();
		keyClicks.clear();

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
					if ( buttons.isKeyStroke() )
						keyDrags.add( dragEntry );
					else
						buttonDrags.add( dragEntry );
				}
				else if ( behaviour instanceof ClickBehaviour )
				{
					final BehaviourEntry< ClickBehaviour > clickEntry = new BehaviourEntry<>( buttons, ( ClickBehaviour ) behaviour );
					if ( buttons.isKeyStroke() )
						keyClicks.add( clickEntry );
					else
						buttonClicks.add( clickEntry );
				}
				else if ( behaviour instanceof ScrollBehaviour )
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

	/**
	 * Stores when the last non-double-clicked keystroke happened.
	 */
	private long timeKeyDown;

	private int getMask( final InputEvent e )
	{
		final int modifiers = e.getModifiers();
		final int modifiersEx = e.getModifiersEx();
		int mask = modifiersEx;

		/*
		 * For scrolling AWT uses the SHIFT_DOWN_MASK to indicate horizontal scrolling.
		 * We keep track of whether the SHIFT key was actually pressed for disambiguation.
		 */
		if ( shiftPressed )
			mask |= InputEvent.SHIFT_DOWN_MASK;
		else
			mask &= ~InputEvent.SHIFT_DOWN_MASK;

		/*
		 * On OS X AWT sets the META_DOWN_MASK to for right clicks. We keep
		 * track of whether the META key was actually pressed for
		 * disambiguation.
		 */
		if ( metaPressed )
			mask |= InputEvent.META_DOWN_MASK;
		else
			mask &= ~InputEvent.META_DOWN_MASK;

		/*
		 * We add the button modifiers to modifiersEx such that the
		 * XXX_DOWN_MASK can be used as the canonical flag. E.g. we adapt
		 * modifiersEx such that BUTTON1_DOWN_MASK is also present in
		 * mouseClicked() when BUTTON1 was clicked (although the button is no
		 * longer down at this point).
		 */
		if ( ( modifiers & InputEvent.BUTTON1_MASK ) != 0 )
			mask |= InputEvent.BUTTON1_DOWN_MASK;
		if ( ( modifiers & InputEvent.BUTTON2_MASK ) != 0 )
			mask |= InputEvent.BUTTON2_DOWN_MASK;
		if ( ( modifiers & InputEvent.BUTTON3_MASK ) != 0 )
			mask |= InputEvent.BUTTON3_DOWN_MASK;

		/*
		 * On OS X AWT sets the BUTTON3_DOWN_MASK for meta+left clicks. Fix
		 * that.
		 */
		System.out.println( modifiers + " -- " + OSX_META_LEFT_CLICK );
		if ( modifiers == OSX_META_LEFT_CLICK )
			mask &= ~InputEvent.BUTTON3_DOWN_MASK;

		/*
		 * Deal with double-clicks.
		 */

		if ( e instanceof MouseEvent && ( ( MouseEvent ) e ).getClickCount() > 1 )
			mask |= InputTrigger.DOUBLE_CLICK_MASK; // mouse
		else if ( e instanceof KeyEvent )
		{
			// double-click on keys.
			if ( ( e.getWhen() - timeKeyDown ) < DOUBLE_CLICK_INTERVAL )
				mask |= InputTrigger.DOUBLE_CLICK_MASK;
			else
				timeKeyDown = e.getWhen();
		}

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
		System.out.println( "MouseAndKeyHandler.mouseDragged()" );
		update();

		final int x = e.getX();
		final int y = e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : activeButtonDrags )
			drag.behaviour.drag( x, y );
	}

	@Override
	public void mouseMoved( final MouseEvent e )
	{
		System.out.println( "MouseAndKeyHandler.mouseMoved()" );
		update();

		mouseX = e.getX();
		mouseY = e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : activeKeyDrags )
			drag.behaviour.drag( mouseX, mouseY );
	}

	@Override
	public void mouseWheelMoved( final MouseWheelEvent e )
	{
		System.out.println( "MouseAndKeyHandler.mouseWheelMoved()" );
		update();

		System.out.println( MouseEvent.getModifiersExText( e.getModifiersEx() ) );

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
		final boolean isHorizontal = !shiftPressed && exShiftMask;

		for ( final BehaviourEntry< ScrollBehaviour > scroll : scrolls )
		{
			if ( scroll.buttons.matches( mask, pressedKeys ) )
			{
				scroll.behaviour.scroll( wheelRotation, isHorizontal, x, y );
			}
		}
	}

	@Override
	public void mouseClicked( final MouseEvent e )
	{
		System.out.println( "MouseAndKeyHandler.mouseClicked()" );
		System.out.println( e );
		update();

		final int mask = getMask( e );
		final int x = e.getX();
		final int y = e.getY();

		for ( final BehaviourEntry< ClickBehaviour > click : buttonClicks )
		{
			if ( click.buttons.matches( mask, pressedKeys ) )
			{
				click.behaviour.click( x, y );
			}
		}
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{
		System.out.println( "MouseAndKeyHandler.mousePressed()" );
		System.out.println( e );
		update();

		final int mask = getMask( e );
		final int x = e.getX();
		final int y = e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : buttonDrags )
		{
			if ( drag.buttons.matches( mask, pressedKeys ) )
			{
				drag.behaviour.init( x, y );
				activeButtonDrags.add( drag );
			}
		}
	}

	@Override
	public void mouseReleased( final MouseEvent e )
	{
		System.out.println( "MouseAndKeyHandler.mouseReleased()" );
		System.out.println( e );
		update();

		final int x = e.getX();
		final int y = e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : activeButtonDrags )
			drag.behaviour.end( x, y );
		activeButtonDrags.clear();
	}

	@Override
	public void mouseEntered( final MouseEvent e )
	{
		System.out.println( "MouseAndKeyHandler.mouseEntered()" );
		update();
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited( final MouseEvent e )
	{
		System.out.println( "MouseAndKeyHandler.mouseExited()" );
		update();
		// TODO Auto-generated method stub

	}

	@Override
	public void keyPressed( final KeyEvent e )
	{
		System.out.println( "MouseAndKeyHandler.keyPressed()" );
		update();

		if ( e.getKeyCode() == KeyEvent.VK_SHIFT )
		{
			shiftPressed = true;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_META )
		{
			metaPressed = true;
		}
		else if (
				e.getKeyCode() != KeyEvent.VK_ALT &&
				e.getKeyCode() != KeyEvent.VK_CONTROL &&
				e.getKeyCode() != KeyEvent.VK_ALT_GRAPH )
		{
			pressedKeys.add( e.getKeyCode() );

			final int mask = getMask( e );

			for ( final BehaviourEntry< DragBehaviour > drag : keyDrags )
			{
				if ( !activeKeyDrags.contains( drag ) && drag.buttons.matches( mask, pressedKeys ) )
				{
					drag.behaviour.init( mouseX, mouseY );
					activeKeyDrags.add( drag );
				}
			}

			for ( final BehaviourEntry< ClickBehaviour > click : keyClicks )
			{
				if ( click.buttons.matches( mask, pressedKeys ) )
				{
					click.behaviour.click( mouseX, mouseY );
				}
			}
		}
	}

	@Override
	public void keyReleased( final KeyEvent e )
	{
		System.out.println( "MouseAndKeyHandler.keyReleased()" );
		update();

		if ( e.getKeyCode() == KeyEvent.VK_SHIFT )
		{
			shiftPressed = false;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_META )
		{
			metaPressed = false;
		}
		else if (
				e.getKeyCode() != KeyEvent.VK_ALT &&
				e.getKeyCode() != KeyEvent.VK_CONTROL &&
				e.getKeyCode() != KeyEvent.VK_ALT_GRAPH )
		{
			pressedKeys.remove( e.getKeyCode() );

			for ( final BehaviourEntry< DragBehaviour > drag : activeKeyDrags )
				drag.behaviour.end( mouseX, mouseY );
			activeKeyDrags.clear();
		}
	}

	@Override
	public void keyTyped( final KeyEvent e )
	{
		System.out.println( "MouseAndKeyHandler.keyTyped()" );
		update();
		// TODO Auto-generated method stub

	}
}
