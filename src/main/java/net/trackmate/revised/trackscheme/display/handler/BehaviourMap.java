package net.trackmate.revised.trackscheme.display.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ActionMap;

/**
 * Maps {@link String} keys to {@link Behaviour}s. Equivalent to
 * {@link BehaviourMap}.
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

	protected int modCount()
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
