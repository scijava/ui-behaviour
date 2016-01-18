package bdv.behaviour;

import java.awt.Dimension;
import java.io.StringReader;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import bdv.behaviour.io.InputTriggerConfig;
import bdv.behaviour.io.InputTriggerDescription;
import bdv.behaviour.io.yaml.YamlConfigIO;

public class UsageExample
{
	static class MyDragBehaviour implements DragBehaviour
	{
		private final String name;

		public MyDragBehaviour( final String name )
		{
			this.name = name;
		}

		@Override
		public void init( final int x, final int y )
		{
			System.out.println( name + ": init(" + x + ", " + y + ")" );
		}

		@Override
		public void drag( final int x, final int y )
		{
			System.out.println( name + ": drag(" + x + ", " + y + ")" );
		}

		@Override
		public void end( final int x, final int y )
		{
			System.out.println( name + ": end(" + x + ", " + y + ")" );
		}
	}

	static class MyClickBehaviour implements ClickBehaviour
	{
		private final String name;

		public MyClickBehaviour( final String name )
		{
			this.name = name;
		}

		@Override
		public void click( final int x, final int y )
		{
			System.out.println( name + ": click(" + x + ", " + y + ")" );
		}
	}

	static class MyScrollBehaviour implements ScrollBehaviour
	{
		private final String name;

		public MyScrollBehaviour( final String name )
		{
			this.name = name;
		}

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y )
		{
			System.out.println( name + ": scroll(" + wheelRotation + ", " + isHorizontal + ", " + x + ", " + y + ")" );
		}
	}

	public static void main( final String[] args )
	{
		/*
		 * Create InputTriggerMap and BehaviourMap. This is analogous to
		 * javax.swing InputMap and ActionMap.
		 */
		final InputTriggerMap inputMap = new InputTriggerMap();
		final BehaviourMap behaviourMap = new BehaviourMap();

		/*
		 * Create a MouseAndKeyHandler that dispatches to registered Behaviours.
		 */
		final MouseAndKeyHandler handler = new MouseAndKeyHandler();
		handler.setInputMap( inputMap );
		handler.setBehaviourMap( behaviourMap );

		/*
		 * Display a JPanel with the MouseAndKeyHandler registered.
		 */
		final JPanel panel = new JPanel();
		panel.setPreferredSize( new Dimension( 400, 400 ) );
		panel.addMouseListener( handler );
		panel.addMouseMotionListener( handler );
		panel.addMouseWheelListener( handler );
		panel.addKeyListener( handler );
		final JFrame frame = new JFrame( "UsageExample" );
		frame.add( panel );
		frame.pack();
		frame.setVisible( true );
		panel.requestFocusInWindow();

		/*
		 * Load YAML config "file".
		 */
		final StringReader reader = new StringReader( "---\n" +
				"- !mapping"               + "\n" +
				"  action: drag1"          + "\n" +
				"  contexts: [all]"        + "\n" +
				"  triggers: [button1, G]" + "\n" +
				"- !mapping"               + "\n" +
				"  action: scroll1"        + "\n" +
				"  contexts: [all]"        + "\n" +
				"  triggers: [scroll]"     + "\n" +
				"" );
		final List< InputTriggerDescription > triggers = YamlConfigIO.read( reader );
		final InputTriggerConfig config = new InputTriggerConfig( triggers );

		/*
		 * Create behaviours and input mappings.
		 */
		behaviourMap.put( "drag1", new MyDragBehaviour( "drag1" ) );
		behaviourMap.put( "drag2", new MyDragBehaviour( "drag2" ) );
		behaviourMap.put( "scroll1", new MyScrollBehaviour( "scroll1" ) );
		behaviourMap.put( "click1", new MyClickBehaviour( "click1" ) );

		final InputTriggerAdder adder = config.inputTriggerAdder( inputMap, "all" );
		adder.put( "drag1" ); // put input trigger as defined in config
		adder.put( "drag2", "button1", "shift A" ); // default triggers if not defined in config
		adder.put( "scroll1", "alt scroll" );
		adder.put( "click1", "button3", "B" );

		/*
		 * See bdv.viewer.TriggerBehaviourBindings for chaining InputMaps and BehaviourMaps.
		 * (might move here in the future)
		 */
	}
}
