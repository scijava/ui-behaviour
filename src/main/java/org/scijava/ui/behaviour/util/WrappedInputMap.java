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
package org.scijava.ui.behaviour.util;

import javax.swing.InputMap;
import javax.swing.KeyStroke;

/**
 * A view of the specified {@link InputMap} that can have its own parent. The
 * wrapped {@code InputMap} should not have a parent!?
 */
public class WrappedInputMap extends InputMap
{
	private static final long serialVersionUID = 1L;

	private final InputMap inputMap;

	private InputMap parent;

	public WrappedInputMap( final InputMap inputMap )
	{
		this.inputMap = inputMap;
	}

	@Override
	public void setParent( final InputMap map )
	{
		parent = map;
	}

	@Override
	public InputMap getParent()
	{
		return parent;
	}

	@Override
	public void put( final KeyStroke keyStroke, final Object actionMapKey )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public Object get( final KeyStroke keyStroke )
	{
		final Object value = inputMap.get( keyStroke );
		if ( value == null )
		{
			final InputMap parent = getParent();
			if ( parent != null )
				return parent.get( keyStroke );
		}
		return value;
	}

	@Override
	public void remove( final KeyStroke key )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public void clear()
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public KeyStroke[] keys()
	{
		return inputMap.keys();
	}

	@Override
	public int size()
	{
		return inputMap.size();
	}
}
