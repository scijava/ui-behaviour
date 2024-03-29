/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2023 Max Planck Institute of Molecular Cell Biology
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
