package bdv.behaviour;

import java.awt.event.MouseListener;

public interface DragBehaviour extends Behaviour
{
	/**
	 * Possibly start the behaviour. This can mean for example that
	 * {@link MouseListener#mousePressed(java.awt.event.MouseEvent)} event was
	 * reveiced. Whether this is actually a drag, can only be determined if the
	 * {@link #drag(int, int)} is called subsequently.
	 *
	 * @param x
	 *            mouse x.
	 * @param y
	 *            mouse y.
	 */
	public void init( int x, int y );
	/*
	 * TODO consider changing return type to boolean to indicate whether this
	 * behaviour "accepts" the drag after checking some preconditions. If it
	 * doesn't, that means it wont we notified about drag()s.
	 */

	/**
	 * Mouse was dragged. Only called between {@link #init(int, int)} and
	 * {@link #end(int, int)}.
	 *
	 * @param x
	 *            mouse x.
	 * @param y
	 *            mouse y.
	 */
	public void drag( int x, int y );

	/**
	 * Possibly end the behaviour. This can mean for example that
	 * {@link MouseListener#mouseReleased(java.awt.event.MouseEvent)} event was
	 * reveiced. Whether this was actually a drag, can only be determined if the
	 * {@link #drag(int, int)} was called since the last {@link #init(int, int)}
	 * .
	 *
	 * @param x
	 *            mouse x.
	 * @param y
	 *            mouse y.
	 */
	public void end( int x, int y );
}
