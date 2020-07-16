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
import java.util.Map;

import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;

/**
 * A view of the specified {@link BehaviourMap} that can have its own parent.
 * The wrapped {@code BehaviourMap} should not have a parent!?
 */
public class WrappedBehaviourMap extends BehaviourMap
{
	private final BehaviourMap behaviourMap;

	private BehaviourMap parent;

	private int expectedParentModCount;

	public WrappedBehaviourMap( final BehaviourMap behaviourMap )
	{
		this.behaviourMap = behaviourMap;
		parent = null;
		expectedParentModCount = 0;
	}

	@Override
	public void setParent( final BehaviourMap map )
	{
		parent = map;
		if ( map != null )
			expectedParentModCount = parent.modCount();
	}

	@Override
	public BehaviourMap getParent()
	{
		return parent;
	}

	@Override
	public synchronized void put( final String key, final Behaviour behaviour )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized Behaviour get( final String key )
	{
		final Behaviour behaviour = behaviourMap.get( key );
		if ( behaviour == null && parent != null )
			return parent.get( key );
		else
			return behaviour;
	}

	@Override
	public synchronized void remove( final String key )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized void clear()
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized Map< String, Behaviour > getAllBindings()
	{
		final Map< String, Behaviour > allBindings = ( parent == null ) ? new HashMap<>() : parent.getAllBindings();

		for ( final Map.Entry< String, Behaviour > entry : behaviourMap.getAllBindings().entrySet() )
			allBindings.put( entry.getKey(), entry.getValue() );

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
				behaviourMap.remove( null ); // hack to bump
												// behaviourMap.modCount
			}
		}
		return behaviourMap.modCount();
	}
}
