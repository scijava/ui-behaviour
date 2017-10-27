package org.scijava.ui.behaviour.io.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

public class TagPanelEditor extends JPanel
{

	@FunctionalInterface
	public static interface TagSelectionChangeListener
	{
		public void tagSelectionChanged();
	}

	private static final long serialVersionUID = 1L;

	private static final String COMMIT_ACTION = "commit";

	protected final List< String > selectedTags;

	protected final List< TagPanel > tagPanels;

	protected final List< String > tags;

	private final JTextField textField;

	private final boolean editable;

	private final HashSet< TagSelectionChangeListener > listeners;

	private final Map< String, String > printables;

	public TagPanelEditor( final Collection< String > tags )
	{
		this( tags, true );
	}

	public TagPanelEditor( final Collection< String > tags, final boolean editable )
	{
		this( tags, editable, Collections.emptyMap() );
	}

	public TagPanelEditor( final Collection< String > tags, final boolean editable, final Map< String, String > printables )
	{
		this.editable = editable;
		this.printables = printables;
		this.tags = new ArrayList<>( tags );
		this.tags.sort( null );
		this.selectedTags = new ArrayList<>();
		this.tagPanels = new ArrayList<>();
		this.listeners = new HashSet<>();

		setPreferredSize( new Dimension( 400, 26 ) );
		setMinimumSize( new Dimension( 26, 26 ) );
		setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
		setBackground( Color.white );
		setBorder( new JTextField().getBorder() );

		this.textField = new JTextField();
		textField.setColumns( 10 );
		textField.setBorder( null );
		textField.setOpaque( false );
		textField.setEditable( editable );

		if ( editable )
		{
			final Autocomplete autoComplete = new Autocomplete();
			textField.getDocument().addDocumentListener( autoComplete );
			textField.getInputMap().put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 ), COMMIT_ACTION );
			textField.getActionMap().put( COMMIT_ACTION, autoComplete.new CommitAction() );
			textField.addKeyListener( new KeyAdapter()
			{
				@Override
				public void keyPressed( final KeyEvent e )
				{
					/*
					 * We have to use a key listener to deal separately with
					 * character removal and tag removal.
					 */
					if ( e.getKeyCode() == KeyEvent.VK_BACK_SPACE && textField.getText().isEmpty() && !selectedTags.isEmpty() )
					{
						removeTag( selectedTags.get( selectedTags.size() - 1 ) );
						e.consume();
					}
				}
			} );
		}

		add( textField );
		add( Box.createHorizontalGlue() );
	}

	public List< String > getSelectedTags()
	{
		return Collections.unmodifiableList( selectedTags );
	}

	public void setTags( final Collection< String > tags )
	{
		for ( final TagPanel tagPanel : tagPanels )
			remove( tagPanel );
		selectedTags.clear();
		tagPanels.clear();

		for ( final String tag : tags )
			addTag( tag );
		revalidate();
		repaint();
	}

	protected void addTag( final String tag )
	{
		final TagPanel tagp = new TagPanel( tag, this.tags.contains( tag ) );
		selectedTags.add( tag );
		tagPanels.add( tagp );
		add( tagp, getComponentCount() - 2 );
	}

	private void removeTag( final String tag )
	{
		final int index = selectedTags.indexOf( tag );
		if ( index < 0 )
			return;

		selectedTags.remove( index );
		final TagPanel tagPanel = tagPanels.remove( index );
		notifyListeners();
		remove( tagPanel );
		revalidate();
		repaint();
	}

	/*
	 * INNER CLASSES
	 */

	/**
	 * Adapted from
	 * http://stackabuse.com/example-adding-autocomplete-to-jtextfield/
	 */
	private class Autocomplete implements DocumentListener
	{

		@Override
		public void changedUpdate( final DocumentEvent ev )
		{}

		@Override
		public void removeUpdate( final DocumentEvent ev )
		{}

		@Override
		public void insertUpdate( final DocumentEvent ev )
		{
			if ( ev.getLength() != 1 )
				return;

			final int pos = ev.getOffset();
			String content = null;
			try
			{
				content = textField.getText( 0, pos + 1 );
			}
			catch ( final BadLocationException e )
			{
				e.printStackTrace();
			}

			// Find where the word starts
			int w;
			for ( w = pos; w >= 0; w-- )
			{
				if ( !Character.isLetter( content.charAt( w ) ) )
				{
					break;
				}
			}

			// Too few chars
			if ( pos - w < 2 )
				return;

			final String prefix = content.substring( w + 1 ).toLowerCase();
			final int n = Collections.binarySearch( tags, prefix );
			if ( n < 0 && -n <= tags.size() )
			{
				final String match = tags.get( -n - 1 );
				if ( match.startsWith( prefix ) )
				{
					// A completion is found
					final String completion = match.substring( pos - w );
					// We cannot modify Document from within notification,
					// so we submit a task that does the change later
					SwingUtilities.invokeLater( new CompletionTask( completion, pos + 1 ) );
				}
			}
		}

		public class CommitAction extends AbstractAction
		{
			private static final long serialVersionUID = 5794543109646743416L;

			@Override
			public void actionPerformed( final ActionEvent ev )
			{
				final String tag = textField.getText();
				if ( selectedTags.contains( tag ) )
				{
					// Do not allow more than 1 tag instance.
					textField.setText( "" );
					return;
				}

				addTag( tag );
				textField.setText( "" );
				notifyListeners();
				revalidate();
				repaint();
			}
		}

		private class CompletionTask implements Runnable
		{
			private String completion;

			private int position;

			CompletionTask( final String completion, final int position )
			{
				this.completion = completion;
				this.position = position;
			}

			@Override
			public void run()
			{
				final StringBuffer sb = new StringBuffer( textField.getText() );
				sb.insert( position, completion );
				textField.setText( sb.toString() );
				textField.setCaretPosition( position + completion.length() );
				textField.moveCaretPosition( position );
			}
		}

	}

	final class TagPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		public TagPanel( final String tag, final boolean valid )
		{
			final Font font = TagPanelEditor.this.getFont().deriveFont( TagPanelEditor.this.getFont().getSize2D() - 2f );
			final String str = printables.containsKey( tag ) ? printables.get( tag ) : tag;
			final JLabel txt = new JLabel( str );
			txt.setFont( font );
			txt.setOpaque( true );
			if ( !valid )
				txt.setBackground( Color.PINK );
			txt.setBorder( new RoundBorder( getBackground().darker(), TagPanelEditor.this, 3 ) );

			final JLabel close = new JLabel( "\u00D7" );
			close.setOpaque( true );
			close.setBackground( getBackground().darker().darker() );
			close.setFont( font );
			close.addMouseListener( new java.awt.event.MouseAdapter()
			{
				@Override
				public void mousePressed( final java.awt.event.MouseEvent evt )
				{
					removeTag( tag );
				}
			} );

			setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
			if ( editable )
				add( close );
			add( Box.createHorizontalStrut( 1 ) );
			add( txt );
			add( Box.createHorizontalStrut( 4 ) );
			setOpaque( false );
		}
	}

	private void notifyListeners()
	{
		for ( final TagSelectionChangeListener listener : listeners )
			listener.tagSelectionChanged();
	}

	public void addTagSelectionChangeListener( final TagSelectionChangeListener listener )
	{
		listeners.add( listener );
	}

	public void removeTagSelectionChangeListener( final TagSelectionChangeListener listener )
	{
		listeners.remove( listener );
	}

}