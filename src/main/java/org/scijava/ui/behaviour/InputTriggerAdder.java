package org.scijava.ui.behaviour;

public interface InputTriggerAdder
{
	public interface Factory
	{
		public InputTriggerAdder inputTriggerAdder( InputTriggerMap map, final String ... contexts );
	}

	public void put( final String behaviourName, final InputTrigger... defaultTriggers );

	public void put( final String behaviourName, final String... defaultTriggers );

	public void put( final String behaviourName );
}
