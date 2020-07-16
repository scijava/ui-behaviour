/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2020 Max Planck Institute of Molecular Cell Biology
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.InputTriggerMap;

/**
 * A view of the specified {@link InputTriggerMap} that can have its own parent.
 * The wrapped {@code InputTriggerMap} should not have a parent!?
 */
public class WrappedInputTriggerMap extends InputTriggerMap
{
	private final InputTriggerMap inputTriggerMap;

	private InputTriggerMap parent;

	private int expectedParentModCount;

	public WrappedInputTriggerMap( final InputTriggerMap inputTriggerMap )
	{
		this.inputTriggerMap = inputTriggerMap;
		parent = null;
		expectedParentModCount = 0;
	}

	@Override
	public void setParent( final InputTriggerMap map )
	{
		parent = map;
		if ( map != null )
			expectedParentModCount = parent.modCount();
	}

	@Override
	public InputTriggerMap getParent()
	{
		return parent;
	}

	@Override
	public synchronized void put( final InputTrigger inputTrigger, final String behaviourKey )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized Set< String > get( final InputTrigger inputTrigger )
	{
		Set< String > keys = null;
		if ( parent != null )
			keys = parent.get( inputTrigger );
		else
			keys = new HashSet<>();
		keys.addAll( inputTriggerMap.get( inputTrigger ) );
		return keys;
	}

	@Override
	public synchronized void remove( final InputTrigger inputTrigger, final String behaviourKey )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized void removeAll( final InputTrigger inputTrigger )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized void clear()
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized Map< InputTrigger, Set< String > > getAllBindings()
	{
		final Map< InputTrigger, Set< String > > allBindings;
		if ( parent != null )
			allBindings = parent.getAllBindings();
		else
			allBindings = new HashMap<>();

		for ( final Map.Entry< InputTrigger, Set< String > > entry : inputTriggerMap.getAllBindings().entrySet() )
		{
			final InputTrigger inputTrigger = entry.getKey();
			if ( entry.getValue() == null || entry.getValue().isEmpty() )
				continue;

			final Set< String > behaviourKeys = allBindings.computeIfAbsent( inputTrigger, k -> new HashSet<>() );
			behaviourKeys.addAll( entry.getValue() );
		}

		return allBindings;
	}

	@Override
	public int modCount()
	{
		if ( parent != null )
		{
			final int m = parent.modCount();
			if ( m != expectedParentModCount )
			{
				expectedParentModCount = m;
				inputTriggerMap.remove( null, null ); // hack to bump
														// inputTriggerMap.modCount
			}
		}
		return inputTriggerMap.modCount();
	}
}
