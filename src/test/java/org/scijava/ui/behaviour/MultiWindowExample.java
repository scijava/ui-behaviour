package org.scijava.ui.behaviour;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.AbstractNamedBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

public class MultiWindowExample
{
	static class MyDragBehaviour extends AbstractNamedBehaviour implements DragBehaviour
	{
		public MyDragBehaviour( final String name )
		{
			super( name );
		}

		@Override
		public void init( final int x, final int y )
		{
			System.out.println( name() + ": init(" + x + ", " + y + ")" );
		}

		@Override
		public void drag( final int x, final int y )
		{
			System.out.println( name() + ": drag(" + x + ", " + y + ")" );
		}

		@Override
		public void end( final int x, final int y )
		{
			System.out.println( name() + ": end(" + x + ", " + y + ")" );
		}
	}

	static class MyClickBehaviour extends AbstractNamedBehaviour implements ClickBehaviour
	{
		public MyClickBehaviour( final String name )
		{
			super( name );
		}

		@Override
		public void click( final int x, final int y )
		{
			System.out.println( name() + ": click(" + x + ", " + y + ")" );
		}
	}

	static class BehaviourFrame
	{
		final JFrame frame;

		final JPanel panel;

		final String title;

		final Behaviours behaviours;

		final MouseAndKeyHandler handler;

		public BehaviourFrame( final String title, final String prefix )
		{
			this.title = title;

			/*
			 * Create InputTriggerMap and BehaviourMap. This is analogous to
			 * javax.swing InputMap and ActionMap.
			 */
			final InputTriggerMap inputMap = new InputTriggerMap();
			final BehaviourMap behaviourMap = new BehaviourMap();

			/*
			 * Create a MouseAndKeyHandler that dispatches to registered Behaviours.
			 */
			handler = new MouseAndKeyHandler();
			handler.setInputMap( inputMap );
			handler.setBehaviourMap( behaviourMap );

			behaviours = new Behaviours( inputMap, behaviourMap, new InputTriggerConfig() );
			behaviours.namedBehaviour( new MyDragBehaviour( prefix + "drag m" ), "button1" );
			behaviours.namedBehaviour( new MyDragBehaviour( prefix + "drag k" ), "K" );
			behaviours.namedBehaviour( new MyClickBehaviour( prefix + "click c " ), "C" );

			/*
			 * Display a JPanel with the MouseAndKeyHandler registered.
			 */
			panel = new JPanel();
			panel.setPreferredSize( new Dimension( 400, 400 ) );
			panel.addMouseListener( handler );
			panel.addMouseMotionListener( handler );
			panel.addMouseWheelListener( handler );
			panel.addKeyListener( handler );
			panel.addFocusListener( handler );
			frame = new JFrame( title );
			frame.add( panel );
			frame.pack();
			frame.setVisible( true );
			panel.requestFocusInWindow();
		}
	}

	public static void main( final String[] args )
	{
		final BehaviourFrame f1 = new BehaviourFrame( "frame1", "f1 : " );
		final BehaviourFrame f2 = new BehaviourFrame( "frame2", "f2 : " );
		f2.frame.setLocation( 300, 0 );

		final KeyPressedManager manager = new KeyPressedManager();
		f1.handler.setKeypressManager( manager, f1.panel );
		f2.handler.setKeypressManager( manager, f2.panel );
	}
}
