package org.scijava.ui.behaviour;

public interface ScrollBehaviour extends Behaviour
{
	/**
	 * The mouse-wheel was scrolled by the specified amount horizontally or
	 * vertically, while the mouse pointer was at the specified location.
	 *
	 * @param wheelRotation
	 *            the number of "clicks" the mouse wheel was rotated, may be
	 *            fractional. negative for scrolling up/right and positive for
	 *            scrolling down/left.
	 * @param isHorizontal
	 *            {@code true} if scrolling horizontally, {@code false} if
	 *            scrolling vertically.
	 * @param x
	 *            mouse x.
	 * @param y
	 *            mouse y.
	 */
	public void scroll( double wheelRotation, boolean isHorizontal, int x, int y );
}
