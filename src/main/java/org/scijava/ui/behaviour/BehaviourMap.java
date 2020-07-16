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
package org.scijava.ui.behaviour;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import java.util.Set;
import javax.swing.ActionMap;

/**
 * Maps {@link String} keys to {@link Behaviour}s. Equivalent to
 * {@link ActionMap}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class BehaviourMap
{
	/**
	 * Maps key to {@link Behaviour}.
	 * Similar to {@link ActionMap}.
	 */
	private final Map< String, Behaviour > behaviours;

	/**
	 * Parent that handles any bindings we don't contain.
	 */
	private BehaviourMap parent;

	private int expectedParentModCount;

	private int modCount;

    /**
     * Creates an {@link BehaviourMap} with no parent and no mappings.
     */
	public BehaviourMap()
	{
		behaviours = new HashMap<>();
		parent = null;
		expectedParentModCount = 0;
		modCount = 0;
	}

	/**
	 * Sets this {@link BehaviourMap}'s parent.
	 *
	 * @param map
	 *            the map that is the parent of this one
	 */
	public void setParent( final BehaviourMap map )
	{
		this.parent = map;
		if ( map != null )
			expectedParentModCount = parent.modCount();
		++modCount;
	}

	/**
	 * Gets this {@link BehaviourMap}'s parent.
	 *
	 * @return map the map that is the parent of this one, or {@code null} if
	 *         this map has no parent
	 */
	public BehaviourMap getParent()
	{
		return parent;
	}

	/**
	 * Adds a binding for {@code key} to {@code behaviour}. If {@code behaviour}
	 * is {@code null}, this removes the current binding for {@code key}.
	 *
	 * @param key
	 * @param behaviour
	 */
	public synchronized void put( final String key, final Behaviour behaviour )
	{
		behaviours.put( key, behaviour );
		++modCount;
	}

	/**
	 * Returns the binding for {@code key}, messaging the parent
	 * {@link BehaviourMap} if the binding is not locally defined.
	 */
	public synchronized Behaviour get( final String key )
	{
		final Behaviour behaviour = behaviours.get( key );
		if ( behaviour == null && parent != null )
			return parent.get( key );
		else
			return behaviour;
	}

    /**
     * Removes the binding for {@code key} from this map.
     */
	public synchronized void remove( final String key )
	{
		behaviours.remove( key );
		++modCount;
	}

    /**
     * Removes all bindings from this map..
     */
	public synchronized void clear()
	{
		behaviours.clear();
		++modCount;
	}

	/**
	 * Get all bindings defined in this map and its parents. Note the returned
	 * map <em>not</em> backed by the {@link BehaviourMap}, i.e., it will not
	 * reflect changes to the {@link BehaviourMap}.
	 *
	 * @return all bindings defined in this map and its parents.
	 */
	public synchronized Map< String, Behaviour > getAllBindings()
	{
		final Map< String, Behaviour > allBindings =
				( parent == null ) ? new HashMap<>() : parent.getAllBindings();

		for ( final Entry< String, Behaviour > entry : behaviours.entrySet() )
			allBindings.put( entry.getKey(), entry.getValue() );

		return allBindings;
	}

	public synchronized Set< String > keys()
	{
		return new HashSet<>( behaviours.keySet() );
	}

	public int modCount()
	{
		if ( parent != null )
		{
			final int m = parent.modCount();
			if ( m != expectedParentModCount )
			{
				expectedParentModCount = m;
				++modCount;
			}
		}
		return modCount;
	}
}
