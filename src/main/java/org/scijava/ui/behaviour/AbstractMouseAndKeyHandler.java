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

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public abstract class AbstractMouseAndKeyHandler
{
	protected static final int DOUBLE_CLICK_INTERVAL = getDoubleClickInterval();

	private static int getDoubleClickInterval()
	{
		final Object prop = Toolkit.getDefaultToolkit().getDesktopProperty( "awt.multiClickInterval" );
		return prop == null ? 200 : ( Integer ) prop;
	}

	private InputTriggerMap inputMap;

	private BehaviourMap behaviourMap;

	private int inputMapExpectedModCount;

	private int behaviourMapExpectedModCount;

	public void setInputMap( final InputTriggerMap inputMap )
	{
		this.inputMap = inputMap;
		inputMapExpectedModCount = inputMap.modCount() - 1;
	}

	public void setBehaviourMap( final BehaviourMap behaviourMap )
	{
		this.behaviourMap = behaviourMap;
		behaviourMapExpectedModCount = behaviourMap.modCount() - 1;
	}

	/*
	 * Managing internal behaviour lists.
	 *
	 * The internal lists only contain entries for Behaviours that can be
	 * actually triggered with the current InputMap, grouped by Behaviour type,
	 * such that hopefully lookup from the event handlers is fast,
	 */

	protected static class BehaviourEntry< T extends Behaviour >
	{
		private final InputTrigger buttons;

		private final T behaviour;

		public BehaviourEntry(
				final InputTrigger buttons,
				final T behaviour )
		{
			this.buttons = buttons;
			this.behaviour = behaviour;
		}

		public InputTrigger buttons()
		{
			return buttons;
		}

		public T behaviour()
		{
			return behaviour;
		}
	}

	protected final ArrayList< BehaviourEntry< DragBehaviour > > buttonDrags = new ArrayList<>();

	protected final ArrayList< BehaviourEntry< DragBehaviour > > keyDrags = new ArrayList<>();

	protected final ArrayList< BehaviourEntry< ClickBehaviour > > buttonClicks = new ArrayList<>();

	protected final ArrayList< BehaviourEntry< ClickBehaviour > > keyClicks = new ArrayList<>();

	protected final ArrayList< BehaviourEntry< ScrollBehaviour > > scrolls = new ArrayList<>();

	/**
	 * Make sure that the internal behaviour lists are up to date. For this, we
	 * keep track the modification count of {@link #inputMap} and
	 * {@link #behaviourMap}. If expected mod counts are not matched, call
	 * {@link #updateInternalMaps()} to rebuild the internal behaviour lists.
	 */
	protected synchronized void update()
	{
		final int imc = inputMap.modCount();
		final int bmc = behaviourMap.modCount();
		if ( imc != inputMapExpectedModCount || bmc != behaviourMapExpectedModCount )
		{
			inputMapExpectedModCount = imc;
			behaviourMapExpectedModCount = bmc;
			updateInternalMaps();
		}
	}

	/**
	 * Build internal lists buttonDrag, keyDrags, etc from of {@link #inputMap}
	 * and {@link #behaviourMap}. The internal lists only contain entries for
	 * behaviours that can be actually triggered with the current InputMap,
	 * grouped by behaviour type, such that hopefully lookup from the event
	 * handlers is fast.
	 */
	private void updateInternalMaps()
	{
		buttonDrags.clear();
		keyDrags.clear();
		buttonClicks.clear();
		keyClicks.clear();
		scrolls.clear();

		for ( final Map.Entry< InputTrigger, Set< String > > entry : inputMap.getAllBindings().entrySet() )
		{
			final InputTrigger buttons = entry.getKey();
			final Set< String > behaviourKeys = entry.getValue();
			if ( behaviourKeys == null )
				continue;

			for ( final String behaviourKey : behaviourKeys )
			{
				final Behaviour behaviour = behaviourMap.get( behaviourKey );
				if ( behaviour == null )
					continue;

				if ( behaviour instanceof DragBehaviour )
				{
					final BehaviourEntry< DragBehaviour > dragEntry = new BehaviourEntry<>( buttons, ( DragBehaviour ) behaviour );
					if ( buttons.isKeyTriggered() )
						keyDrags.add( dragEntry );
					else
						buttonDrags.add( dragEntry );
				}
				if ( behaviour instanceof ClickBehaviour )
				{
					final BehaviourEntry< ClickBehaviour > clickEntry = new BehaviourEntry<>( buttons, ( ClickBehaviour ) behaviour );
					if ( buttons.isKeyTriggered() )
						keyClicks.add( clickEntry );
					else
						buttonClicks.add( clickEntry );
				}
				if ( behaviour instanceof ScrollBehaviour )
				{
					final BehaviourEntry< ScrollBehaviour > scrollEntry = new BehaviourEntry<>( buttons, ( ScrollBehaviour ) behaviour );
					scrolls.add( scrollEntry );
				}
			}
		}
	}
}
