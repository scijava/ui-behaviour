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

import gnu.trove.set.TIntSet;

/**
 * Distributes KEY_PRESSED events between windows that share the same
 * {@link KeyPressedManager}. The goal is to make keyboard click/drag behaviours
 * work like mouse click/drag: When a behaviour is initiated with a key press,
 * the window under the mouse receives focus and the behaviour is handled there.
 *
 * @author Tobias Pietzsch
 */
public class KeyPressedManager
{
	public interface KeyPressedReceiver
	{
		void handleKeyPressed( final KeyPressedReceiver origin, final int mask, final boolean doubleClick, final TIntSet pressedKeys );
	}

	private KeyPressedReceiver active = null;

	public void handleKeyPressed(
			final KeyPressedReceiver origin,
			final int mask,
			final boolean doubleClick,
			final TIntSet pressedKeys )
	{
		if ( active != null )
			active.handleKeyPressed( origin, mask, doubleClick, pressedKeys );
		else
			origin.handleKeyPressed( origin, mask, doubleClick, pressedKeys );
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
