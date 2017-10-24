package org.scijava.ui.behaviour.io;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class TagPanelEditor extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final List< String > selectedTags;

	private final Collection< String > tags;

	public TagPanelEditor( final Collection< String > tags )
	{
		this.tags = tags;
		this.selectedTags = new ArrayList<>();

		setPreferredSize( new Dimension( 400, 26 ) );
		setMinimumSize( new Dimension( 26, 26 ) );
		setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
		setBackground( Color.white );
		setBorder( new JTextField().getBorder() );

		final JTextField txtfieldContext = new JTextField();
		txtfieldContext.setColumns( 10 );
		txtfieldContext.setBorder( null );
		txtfieldContext.setOpaque( false );
		add( txtfieldContext );
		add( Box.createHorizontalGlue() );

		txtfieldContext.addKeyListener( new KeyAdapter()
		{
			@Override
			public void keyReleased( final java.awt.event.KeyEvent evt )
			{
				tagcheck( evt );
			}

			private void tagcheck( final KeyEvent evt )
			{
				final String s = txtfieldContext.getText();
				if ( s.length() > 0 )
				{
					for ( final String tag : tags )
					{
						if ( s.equals( tag ) && !selectedTags.contains( tag) )
						{
							selectedTags.add( tag );
							final TagPanel tagp = new TagPanel( tag );
							add( tagp, getComponentCount() - 2 );
							txtfieldContext.setText( "" );
							revalidate();
						}
					}
				}
			}
		} );
	}

	public List< String > getSelectedTags()
	{
		return Collections.unmodifiableList( selectedTags );
	}

	public void setTags( final Collection< String > tags )
	{
		for ( final Component child : getComponents() )
			if ( child instanceof TagPanel )
				remove( child );

		selectedTags.clear();
		for ( final String tag : tags )
		{
			selectedTags.add( tag );
			final TagPanel tagp = new TagPanel( tag, this.tags.contains( tag ) );
			add( tagp, getComponentCount() - 2 );
		}
		revalidate();
	}

	private final class TagPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		public TagPanel( final String tag )
		{
			this( tag, true );
		}

		public TagPanel( final String tag, final boolean valid )
		{
			final Font font = TagPanelEditor.this.getFont().deriveFont( TagPanelEditor.this.getFont().getSize2D() - 2f );
			final JLabel txt = new JLabel( tag );
			txt.setFont( font );
			txt.setOpaque( true );
			if ( !valid )
				txt.setBackground( Color.PINK );
			txt.setBorder( new RoundBorder( getBackground().darker(), Color.WHITE, 3 ) );

			final JLabel close = new JLabel( "\u00D7" );
			close.setOpaque( true );
			close.setBackground( getBackground().darker().darker() );
			close.setFont( font );

			setLayout( new FlowLayout( FlowLayout.CENTER, 2, 0 ) );
			close.addMouseListener( new java.awt.event.MouseAdapter()
			{
				@Override
				public void mousePressed( final java.awt.event.MouseEvent evt )
				{
					TagPanelEditor.this.remove( TagPanel.this );
					TagPanelEditor.this.revalidate();
				}
			} );
			add( close );
			add( txt );
			setOpaque( false );
		}

	}

}
