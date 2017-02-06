package org.scijava.ui.behaviour;

import java.awt.event.KeyListener;

import gnu.trove.set.TIntSet;

/**
 * Distributes {@link KeyListener#keyPressed(java.awt.event.KeyEvent)} events
 * between windows that share the same {@link KeyPressedManager}. The goal is to
 * make keyboard click/drag behaviours work like mouse click/drag: When a
 * behaviour is initiated with a key press, the window under the mouse receives
 * focus and the behaviour is handled there.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class KeyPressedManager
{
	public interface KeyPressedReceiver
	{
		public void handleKeyPressed( final int mask, final boolean doubleClick, final TIntSet pressedKeys );
	}

	private KeyPressedReceiver active = null;

	public void handleKeyPressed(
			final KeyPressedReceiver origin,
			final int mask,
			final boolean doubleClick,
			final TIntSet pressedKeys )
	{
		if ( active != null )
			active.handleKeyPressed( mask, doubleClick, pressedKeys );
		else
			origin.handleKeyPressed( mask, doubleClick, pressedKeys );
	}

	public void activate( final KeyPressedReceiver handler)
	{
		active = handler;
	}

	public void deactivate( final KeyPressedReceiver handler )
	{
		if ( active == handler )
			active = null;
	}
}
