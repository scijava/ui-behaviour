package org.scijava.ui.behaviour;

public interface ClickBehaviour extends Behaviour
{
	/**
	 * A click occuered at the specified location, where click can mean a
	 * regular mouse click or a typed key.
	 *
	 * @param x
	 *            mouse x.
	 * @param y
	 *            mouse y.
	 */
	public void click( int x, int y );
}
