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
package org.scijava.ui.behaviour.util;

import javax.swing.Action;
import javax.swing.ActionMap;

/**
 * A view of the specified {@link ActionMap} that can have its own parent. The
 * wrapped {@code ActionMap} should not have a parent!?
 */
public class WrappedActionMap extends ActionMap
{
	private static final long serialVersionUID = 1L;

	private final ActionMap actionMap;

	private ActionMap parent;

	public WrappedActionMap( final ActionMap inputMap )
	{
		this.actionMap = inputMap;
	}

	@Override
	public void setParent( final ActionMap map )
	{
		parent = map;
	}

	@Override
	public ActionMap getParent()
	{
		return parent;
	}

	@Override
	public void put( final Object key, final Action action )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public Action get( final Object key )
	{
		final Action value = actionMap.get( key );
		if ( value == null )
		{
			final ActionMap parent = getParent();
			if ( parent != null )
				return parent.get( key );
		}
		return value;
	}

	@Override
	public void remove( final Object key )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public void clear()
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public Object[] keys()
	{
		return actionMap.keys();
	}

	@Override
	public int size()
	{
		return actionMap.size();
	}
}
