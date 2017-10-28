package org.scijava.ui.behaviour.io;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.InputTriggerConfig.Input;
import org.scijava.ui.behaviour.io.gui.InputTriggerPanelEditor;
import org.scijava.ui.behaviour.io.gui.TagPanelEditor;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

public class VisualEditorPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private JTextField textFieldFilter;

	private final MyTableModel tableModel;

	private final InputTriggerPanelEditor keybindingEditor;

	private final TagPanelEditor contextsEditor;

	private final JLabel labelActionName;

	private final JTable tableBindings;

	/**
	 * Create the panel.
	 */
	public VisualEditorPanel( final InputTriggerConfig config, final Set< String > actions, final Set< String > contexts )
	{
		/*
		 * GUI
		 */

		setLayout( new BorderLayout( 0, 0 ) );

		textFieldFilter = new JTextField();
		add( textFieldFilter, BorderLayout.NORTH );
		textFieldFilter.setColumns( 10 );

		final JPanel panelEditor = new JPanel();
		add( panelEditor, BorderLayout.SOUTH );
		panelEditor.setLayout( new BorderLayout( 0, 0 ) );

		final JPanel panelCommandButtons = new JPanel();
		panelEditor.add( panelCommandButtons, BorderLayout.NORTH );
		panelCommandButtons.setLayout( new BoxLayout( panelCommandButtons, BoxLayout.X_AXIS ) );

		final JButton btnCopyCommand = new JButton( "Copy" );
		btnCopyCommand.setToolTipText( "Duplicate action binding \nto a new, blank binding." );
		panelCommandButtons.add( btnCopyCommand );

		final JButton btnUnbindAction = new JButton( "Unbind" );
		btnUnbindAction.setToolTipText( "Remove current binding\nfor current action." );
		panelCommandButtons.add( btnUnbindAction );

		final JButton btnDeleteAction = new JButton( "Unbind all" );
		btnDeleteAction.setToolTipText( "Remove all bindings\nto current action." );
		panelCommandButtons.add( btnDeleteAction );

		final Component horizontalGlue = Box.createHorizontalGlue();
		panelCommandButtons.add( horizontalGlue );

		final JButton btnExportCsv = new JButton( "Export CSV" );
		btnExportCsv.setToolTipText( "Export all action bindings \nto a CSV file." );
		panelCommandButtons.add( btnExportCsv );

		final JPanel panelCommandEditor = new JPanel();
		panelEditor.add( panelCommandEditor, BorderLayout.CENTER );
		final GridBagLayout gbl_panelCommandEditor = new GridBagLayout();
		gbl_panelCommandEditor.columnWidths = new int[] { 30, 100 };
		gbl_panelCommandEditor.columnWeights = new double[] { 0.0, 1.0 };
		gbl_panelCommandEditor.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0 };
		panelCommandEditor.setLayout( gbl_panelCommandEditor );

		final JLabel lblName = new JLabel( "Name:" );
		final GridBagConstraints gbc_lblName = new GridBagConstraints();
		gbc_lblName.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblName.anchor = GridBagConstraints.WEST;
		gbc_lblName.gridx = 0;
		gbc_lblName.gridy = 0;
		panelCommandEditor.add( lblName, gbc_lblName );

		this.labelActionName = new JLabel();
		final GridBagConstraints gbc_labelActionName = new GridBagConstraints();
		gbc_labelActionName.anchor = GridBagConstraints.WEST;
		gbc_labelActionName.insets = new Insets( 5, 5, 5, 5 );
		gbc_labelActionName.gridx = 1;
		gbc_labelActionName.gridy = 0;
		panelCommandEditor.add( labelActionName, gbc_labelActionName );

		final JLabel lblBinding = new JLabel( "Binding:" );
		final GridBagConstraints gbc_lblBinding = new GridBagConstraints();
		gbc_lblBinding.anchor = GridBagConstraints.WEST;
		gbc_lblBinding.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblBinding.gridx = 0;
		gbc_lblBinding.gridy = 1;
		panelCommandEditor.add( lblBinding, gbc_lblBinding );

		this.keybindingEditor = new InputTriggerPanelEditor( true );
		final GridBagConstraints gbc_textFieldBinding = new GridBagConstraints();
		gbc_textFieldBinding.insets = new Insets( 5, 5, 5, 5 );
		gbc_textFieldBinding.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldBinding.gridx = 1;
		gbc_textFieldBinding.gridy = 1;
		panelCommandEditor.add( keybindingEditor, gbc_textFieldBinding );

		final JLabel lblContext = new JLabel( "Contexts:" );
		final GridBagConstraints gbc_lblContext = new GridBagConstraints();
		gbc_lblContext.anchor = GridBagConstraints.WEST;
		gbc_lblContext.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblContext.gridx = 0;
		gbc_lblContext.gridy = 2;
		panelCommandEditor.add( lblContext, gbc_lblContext );

		this.contextsEditor = new TagPanelEditor( contexts );
		final GridBagConstraints gbc_comboBoxContext = new GridBagConstraints();
		gbc_comboBoxContext.insets = new Insets( 5, 5, 5, 5 );
		gbc_comboBoxContext.fill = GridBagConstraints.BOTH;
		gbc_comboBoxContext.gridx = 1;
		gbc_comboBoxContext.gridy = 2;
		panelCommandEditor.add( contextsEditor, gbc_comboBoxContext );

		final JLabel lblConflicts = new JLabel( "Conflicts:" );
		final GridBagConstraints gbc_lblConflicts = new GridBagConstraints();
		gbc_lblConflicts.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblConflicts.anchor = GridBagConstraints.WEST;
		gbc_lblConflicts.gridx = 0;
		gbc_lblConflicts.gridy = 3;
		panelCommandEditor.add( lblConflicts, gbc_lblConflicts );

		final JLabel lblConflict = new JLabel( "" );
		final GridBagConstraints gbc_lblConflict = new GridBagConstraints();
		gbc_lblConflict.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblConflict.gridx = 1;
		gbc_lblConflict.gridy = 3;
		panelCommandEditor.add( lblConflict, gbc_lblConflict );

		final JLabel lblDescription = new JLabel( "Description:" );
		final GridBagConstraints gbc_lblDescription = new GridBagConstraints();
		gbc_lblDescription.fill = GridBagConstraints.VERTICAL;
		gbc_lblDescription.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblDescription.anchor = GridBagConstraints.WEST;
		gbc_lblDescription.gridx = 0;
		gbc_lblDescription.gridy = 4;
		panelCommandEditor.add( lblDescription, gbc_lblDescription );

		final JLabel labelActionDescription = new JLabel();
		final GridBagConstraints gbc_labelActionDescription = new GridBagConstraints();
		gbc_labelActionDescription.fill = GridBagConstraints.VERTICAL;
		gbc_labelActionDescription.insets = new Insets( 5, 5, 5, 5 );
		gbc_labelActionDescription.gridx = 1;
		gbc_labelActionDescription.gridy = 4;
		panelCommandEditor.add( labelActionDescription, gbc_labelActionDescription );

		final JPanel panelButtons = new JPanel();
		panelEditor.add( panelButtons, BorderLayout.SOUTH );
		final FlowLayout flowLayout = ( FlowLayout ) panelButtons.getLayout();
		flowLayout.setAlignment( FlowLayout.TRAILING );

		final JButton btnRestore = new JButton( "Restore" );
		panelButtons.add( btnRestore );

		final JButton btnApply = new JButton( "Apply" );
		panelButtons.add( btnApply );

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		scrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		add( scrollPane, BorderLayout.CENTER );

		tableModel = new MyTableModel( actions, config.actionToInputsMap );
		tableBindings = new JTable( tableModel );
		tableBindings.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		tableBindings.setFillsViewportHeight( true );
		tableBindings.setAutoResizeMode( JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS );
		tableBindings.getSelectionModel().addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( final ListSelectionEvent e )
			{
				if ( e.getValueIsAdjusting() )
					return;
				removeDuplicates();
				updateEditors();

			}
		} );

		// Listen to changes in the
		keybindingEditor.addInputTriggerChangeListener( () -> keybindingsChanged( tableBindings.getSelectedRow(), keybindingEditor.getInputTrigger() ) );

		// Listen to changes in context editor and forward to table model.
		contextsEditor.addTagSelectionChangeListener( () -> contextsChanged( tableBindings.getSelectedRow(), contextsEditor.getSelectedTags() ) );

		// Button presses.
		btnCopyCommand.addActionListener( ( e ) -> copyCommand( tableBindings.getSelectedRow() ) );
		btnUnbindAction.addActionListener( ( e ) -> unbindCommand( tableBindings.getSelectedRow() ) );

		// Renderers.
		tableBindings.getColumnModel().getColumn( 1 ).setCellRenderer( new MyBindingsRenderer() );
		tableBindings.getColumnModel().getColumn( 2 ).setCellRenderer( new MyContextsRenderer( contexts ) );
		tableBindings.getSelectionModel().setSelectionInterval( 0, 0 );
		tableBindings.setRowHeight( 30 );
		scrollPane.setViewportView( tableBindings );
	}

	private void updateEditors()
	{
		final int row = tableBindings.getSelectedRow();
		if ( row < 0 )
		{
			labelActionName.setText( "" );
			keybindingEditor.setInputTrigger( InputTrigger.NOT_MAPPED );
			contextsEditor.setTags( Collections.emptyList() );
			return;
		}

		final String action = tableModel.actions.get( row );
		final InputTrigger trigger = tableModel.bindings.get( row );
		final List< String > contexts = new ArrayList<>( tableModel.contexts.get( row ) );

		labelActionName.setText( action );
		keybindingEditor.setInputTrigger( trigger );
		contextsEditor.setTags( contexts );
	}

	private void unbindCommand( final int row )
	{
		if ( row < 0 )
			return;

		final InputTrigger inputTrigger = tableModel.bindings.get( row );
		if ( inputTrigger == InputTrigger.NOT_MAPPED )
			return;

		// Update model &
		keybindingsChanged( row, InputTrigger.NOT_MAPPED );
		contextsChanged( row, Collections.emptyList() );

		// Find whether we have two lines with the same action, unbound.
		removeDuplicates();
	}

	private void removeDuplicates()
	{
		final Map< String, Set< InputTrigger > > bindings = new HashMap<>();
		final List< Integer > toRemove = new ArrayList<>();
		for ( int row = 0; row < tableModel.getRowCount(); row++ )
		{
			final String action = tableModel.actions.get( row );
			final InputTrigger trigger = tableModel.bindings.get( row );

			if ( bindings.get( action ) == null )
			{
				final Set< InputTrigger > triggers = new HashSet<>();
				triggers.add( trigger );
				bindings.put( action, triggers );
			}
			else
			{
				final Set< InputTrigger > triggers = bindings.get( action );
				final boolean notAlreadyPresent = triggers.add( trigger );
				if ( !notAlreadyPresent )
					toRemove.add( Integer.valueOf( row ) );
			}
		}

		toRemove.sort( Comparator.reverseOrder() );
		for ( final Integer rowToRemove : toRemove )
		{
			final int row = rowToRemove.intValue();
			tableModel.actions.remove( row );
			tableModel.bindings.remove( row );
			tableModel.contexts.remove( row );
			tableModel.fireTableRowsDeleted( row, row );
		}

		updateEditors();
	}

	private void copyCommand( final int row )
	{
		if ( row < 0 )
			return;

		final String action = tableModel.actions.get( row );
		// Check whether there is already a line in the table without a binding.
		for ( int i = 0; i < tableModel.actions.size(); i++ )
		{
			// Brute force.
			if ( tableModel.actions.get( i ).equals( action ) && tableModel.bindings.get( i ) == InputTrigger.NOT_MAPPED )
				return;
		}

		// Create one then.
		tableModel.actions.add( row + 1, action );
		tableModel.bindings.add( row + 1, InputTrigger.NOT_MAPPED );
		tableModel.contexts.add( row + 1, Collections.emptyList() );
		tableModel.fireTableRowsInserted( row, row );
	}

	private void keybindingsChanged( final int row, final InputTrigger inputTrigger )
	{
		if ( row < 0 )
			return;

		tableModel.bindings.set( row, inputTrigger );
		tableModel.fireTableCellUpdated( row, 1 );
	}

	private void contextsChanged( final int row, final List< String > selectedContexts )
	{
		if ( row < 0 )
			return;

		tableModel.contexts.set( row, new ArrayList<>( selectedContexts ) );
		tableModel.fireTableCellUpdated( row, 2 );
	}

	/*
	 * INNER CLASSES
	 */

	private static final class MyContextsRenderer extends TagPanelEditor implements TableCellRenderer
	{

		private static final long serialVersionUID = 1L;

		public MyContextsRenderer( final Collection< String > tags )
		{
			super( tags, false );
			setBorder( null );
		}

		@Override
		public Component getTableCellRendererComponent( final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			setForeground( isSelected ? table.getSelectionForeground() : table.getForeground() );
			setBackground( isSelected ? table.getSelectionBackground() : table.getBackground() );

			@SuppressWarnings( "unchecked" )
			final List< String > contexts = ( List< String > ) value;
			setTags( contexts );
			setToolTipText( contexts.toString() );
			return this;
		}
	}

	private static final class MyBindingsRenderer extends InputTriggerPanelEditor implements TableCellRenderer
	{

		private static final long serialVersionUID = 1L;

		public MyBindingsRenderer()
		{
			super( false );
			setBorder( null );
		}

		@Override
		public Component getTableCellRendererComponent( final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			setForeground( isSelected ? table.getSelectionForeground() : table.getForeground() );
			setBackground( isSelected ? table.getSelectionBackground() : table.getBackground() );

			final InputTrigger input = ( InputTrigger ) value;
			if ( null != input )
			{
				setInputTrigger( input );
				final String val = input.toString();
				setToolTipText( val );
			}
			else
			{
				setInputTrigger( InputTrigger.NOT_MAPPED );
				setToolTipText( "No binding" );
			}
			return this;
		}
	}

	private static class MyTableModel extends AbstractTableModel
	{

		private static final long serialVersionUID = 1L;

		private static final String[] TABLE_HEADERS = new String[] { "Action", "Binding", "Contexts" };

		private final List< String > actions;

		private final List< InputTrigger > bindings;

		private final List< List< String > > contexts;

		public MyTableModel( final Set< String > baseActions, final Map< String, Set< Input > > actionToInputsMap )
		{
			this.actions = new ArrayList<>();
			this.bindings = new ArrayList<>();
			this.contexts = new ArrayList<>();

			final Set< String > allActions = new HashSet<>();
			allActions.addAll( baseActions );
			allActions.addAll( actionToInputsMap.keySet() );
			final List< String > sortedActions = new ArrayList<>( allActions );
			sortedActions.sort( null );
			final InputComparator inputComparator = new InputComparator();
			for ( final String action : sortedActions )
			{
				final Set< Input > inputs = actionToInputsMap.get( action );
				if ( null == inputs )
				{
					actions.add( action );
					bindings.add( InputTrigger.NOT_MAPPED );
					contexts.add( Collections.emptyList() );
				}
				else
				{
					final List< Input > sortedInputs = new ArrayList<>( inputs );
					sortedInputs.sort( inputComparator );
					for ( final Input input : sortedInputs )
					{
						actions.add( action );
						bindings.add( input.trigger );
						final List< String > cs = new ArrayList<>( input.contexts );
						cs.sort( null );
						contexts.add( cs );
					}
				}
			}
		}

		@Override
		public int getRowCount()
		{
			return actions.size();
		}

		@Override
		public int getColumnCount()
		{
			return 3;
		}

		@Override
		public Object getValueAt( final int rowIndex, final int columnIndex )
		{
			switch ( columnIndex )
			{
			case 0:
				return actions.get( rowIndex );
			case 1:
				return bindings.get( rowIndex );
			case 2:
				return contexts.get( rowIndex );
			default:
				throw new NoSuchElementException( "Cannot access column " + columnIndex + " in this model." );
			}
		}

		@Override
		public String getColumnName( final int column )
		{
			return TABLE_HEADERS[ column ];
		}
	}

	private static final class InputComparator implements Comparator< Input >
	{

		@Override
		public int compare( final Input o1, final Input o2 )
		{
			if ( o1.trigger == InputTrigger.NOT_MAPPED )
				return 1;
			if ( o2.trigger == InputTrigger.NOT_MAPPED )
				return -1;
			return o1.trigger.toString().compareTo( o2.trigger.toString() );
		}

	}

	/*
	 * DEMO METHODS.
	 */

	private static InputTriggerConfig getDemoConfig()
	{
		final StringReader reader = new StringReader( "---\n" +
				"- !mapping" + "\n" +
				"  action: fluke" + "\n" +
				"  contexts: [all]" + "\n" +
				"  triggers: [F]" + "\n" +
				"- !mapping" + "\n" +
				"  action: drag1" + "\n" +
				"  contexts: [all]" + "\n" +
				"  triggers: [button1, win G]" + "\n" +
				"- !mapping" + "\n" +
				"  action: scroll1" + "\n" +
				"  contexts: [all]" + "\n" +
				"  triggers: [scroll]" + "\n" +
				"- !mapping" + "\n" +
				"  action: scroll1" + "\n" +
				"  contexts: [trackscheme, mamut]" + "\n" +
				"  triggers: [shift D]" + "\n" +
				"- !mapping" + "\n" +
				"  action: destroy the world" + "\n" +
				"  contexts: [unknown context, mamut]" + "\n" +
				"  triggers: [control A]" + "\n" +
				"" );
		final List< InputTriggerDescription > triggers = YamlConfigIO.read( reader );
		final InputTriggerConfig config = new InputTriggerConfig( triggers );
		return config;
	}

	private static Set< String > getDemoActions()
	{
		final Set< String > actions = new HashSet<>();
		actions.add( "drag1" );
		actions.add( "scroll1" );
		actions.add( "destroy the world" );
		actions.add( "ride the dragon" );
		actions.add( "make some coffee" );
		return actions;
	}

	private static Set< String > getDemoContexts()
	{
		final Set< String > contexts = new HashSet<>();
		contexts.add( "all" );
		contexts.add( "mamut" );
		contexts.add( "trackscheme" );
		return contexts;
	}

	/**
	 * Launch the application.
	 *
	 * @throws UnsupportedLookAndFeelException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		EventQueue.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					final JFrame frame = new JFrame( "Behaviour Key bindings editor" );
					final VisualEditorPanel editorPanel = new VisualEditorPanel( getDemoConfig(), getDemoActions(), getDemoContexts() );
					frame.getContentPane().add( editorPanel );
					frame.pack();
					frame.setVisible( true );
				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		} );
	}

}
