package bdv.behaviour;

import javax.swing.InputMap;
import javax.swing.KeyStroke;

public interface KeyStrokeAdder
{
	public interface Factory
	{
		public KeyStrokeAdder keyStrokeAdder( InputMap map, final String ... contexts );
	}

	public void put( final String actionName, final KeyStroke... defaultKeyStrokes );

	public void put( final String actionName, final String... defaultKeyStrokes );

	public void put( final String actionName );
}
