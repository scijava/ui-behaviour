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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.InputMap;



/**
 * Similar to {@link InputMap}, this provides bindings from {@link InputTrigger}
 * to {@link Behaviour} keys, and is chainable (see
 * {@link #setParent(InputTriggerMap)}).
 * <p>
 * In contrast to {@code InputMap}, one {@link InputTrigger} can map to multiple
 * {@link Behaviour} keys.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class InputTriggerMap
{
	/**
	 * Maps {@link InputTrigger} to a set of {@link Behaviour} keys.
	 */
	private final Map< InputTrigger, Set< String > > triggerToKeys;

	/**
	 * Parent that handles any bindings we don't contain.
	 */
	private InputTriggerMap parent;

	private int expectedParentModCount;

	private int modCount;

	/**
	 * Creates an {@link InputTriggerMap} with no parent and no mappings.
	 */
	public InputTriggerMap()
	{
		triggerToKeys = new HashMap<>();
		parent = null;
		expectedParentModCount = 0;
		modCount = 0;
	}

	/**
	 * Sets this {@link InputTriggerMap}'s parent.
	 *
	 * @param map
	 *            the map that is the parent of this one
	 */
	public void setParent( final InputTriggerMap map )
	{
		this.parent = map;
		if ( map != null )
			expectedParentModCount = parent.modCount();
		++modCount;
	}

	/**
	 * Gets this {@link InputTriggerMap}'s parent.
	 *
	 * @return map the map that is the parent of this one, or {@code null} if
	 *         this map has no parent
	 */
	public InputTriggerMap getParent()
	{
		return parent;
	}

    /**
     * Adds a binding for {@code inputTrigger} to {@code behaviourKey}.
     *
	 * @param inputTrigger
	 * @param behaviourKey
	 */
	public synchronized void put( final InputTrigger inputTrigger, final String behaviourKey )
	{
		Set< String > behaviourKeys = triggerToKeys.get( inputTrigger );
		if ( behaviourKeys == null )
		{
			behaviourKeys = new HashSet<>();
			triggerToKeys.put( inputTrigger, behaviourKeys );
		}

		behaviourKeys.add( behaviourKey );
		++modCount;
	}

	/**
	 * Get the set of all bindings for {@code inputTrigger} defined in this map
	 * and its parents.
	 *
	 * @param inputTrigger
	 * @return bindings for {@code inputTrigger}.
	 */
	public synchronized Set< String > get( final InputTrigger inputTrigger )
	{
		Set< String > keys = null;
		if ( parent != null )
			keys = parent.get( inputTrigger );
		else
			keys = new HashSet<>();
		keys.addAll( triggerToKeys.get( inputTrigger ) );
		return keys;
	}

	/**
	 * Remove the specific binding from {@code inputTrigger} to {@code behaviourKey} from this map.
	 *
	 * @param inputTrigger
	 * @param behaviourKey
	 */
	public synchronized void remove( final InputTrigger inputTrigger, final String behaviourKey )
	{
		final Set< String > behaviourKeys = triggerToKeys.get( inputTrigger );
		if ( behaviourKeys == null )
			return;

		behaviourKeys.remove( behaviourKey );

		if ( behaviourKeys.isEmpty() )
			triggerToKeys.remove( behaviourKeys );
		++modCount;
	}

	/**
	 * Remove all bindings for {@code inputTrigger} from this map.
	 * @param inputTrigger
	 */
	public synchronized void removeAll( final InputTrigger inputTrigger )
	{
		triggerToKeys.remove( inputTrigger );
		++modCount;
	}

	/**
	 * Remove all bindings from this map.
	 */
	public synchronized void clear()
	{
		triggerToKeys.clear();
		++modCount;
	}

	/**
	 * Get all bindings defined in this map and its parents. Note that the
	 * returned map is <em>not</em> backed by the {@link InputTriggerMap}, i.e.,
	 * it will not reflect changes to the {@link InputTriggerMap}.
	 * <p>
	 * This differs from {@code getBindings()} in that this method includes the
	 * bindings defined in the parent.
	 *
	 * @return all bindings (trigger to set of behaviour keys) defined in this
	 *         map and its parents.
	 */
	public synchronized Map< InputTrigger, Set< String > > getAllBindings()
	{
		final Map< InputTrigger, Set< String > > allBindings;
		if ( parent != null )
			allBindings = parent.getAllBindings();
		else
			allBindings = new HashMap<>();

		addBindings( allBindings );

		return allBindings;
	}

	/**
	 * Get all bindings defined in this map. Note that the returned map is
	 * <em>not</em> backed by the {@link InputTriggerMap}, i.e., it will not
	 * reflect changes to the {@link InputTriggerMap}.
	 *
	 * @return all bindings (trigger to set of behaviour keys) defined in this
	 *         map.
	 */
	public synchronized Map< InputTrigger, Set< String > > getBindings()
	{
		final Map< InputTrigger, Set< String > > bindings = new HashMap<>();
		addBindings( bindings );
		return bindings;
	}

	private void addBindings( final Map< InputTrigger, Set< String > > bindings )
	{
		for ( final Entry< InputTrigger, Set< String > > entry : triggerToKeys.entrySet() )
		{
			final InputTrigger inputTrigger = entry.getKey();
			if ( entry.getValue() == null || entry.getValue().isEmpty() )
				continue;

			Set< String > behaviourKeys = bindings.get( inputTrigger );
			if ( behaviourKeys == null )
			{
				behaviourKeys = new HashSet<>();
				bindings.put( inputTrigger, behaviourKeys );
			}
			behaviourKeys.addAll( entry.getValue() );
		}
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
