package org.scijava.ui.behaviour.io;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.InputTriggerConfig.Input;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionBuilder;
import org.scijava.ui.behaviour.io.gui.InputTriggerPanelEditor;
import org.scijava.ui.behaviour.io.gui.TagPanelEditor;

public class VisualEditorPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	static JFileChooser fileChooser = new JFileChooser();

	static
	{
		fileChooser.setFileFilter( new FileFilter()
		{

			@Override
			public String getDescription()
			{
				return "CSV files";
			}

			@Override
			public boolean accept( final File f )
			{
				return f.isFile() && f.getName().toLowerCase().endsWith( ".csv" );
			}
		} );
	}

	/**
	 * Interface for listeners notified when settings are changed in the visual
	 * editor.
	 */
	@FunctionalInterface
	public static interface ConfigChangeListener
	{
		/**
		 * Called when settings are changed in the visual editor.
		 */
		public void configChanged();
	}

	private JTextField textFieldFilter;

	private MyTableModel tableModel;

	private final InputTriggerPanelEditor keybindingEditor;

	private final TagPanelEditor contextsEditor;

	private final JLabel labelCommandName;

	private final JTable tableBindings;

	private final InputTriggerConfig config;

	private final Map< String, Map< String, String > > actionDescriptions;

	private final Set< String > contexts;

	private final JLabel lblConflict;

	private JTextArea textAreaDescription;

	private final JPanel panelEditor;

	private final JPanel panelButtons;

	private final HashSet< ConfigChangeListener > listeners;

	/**
	 * Creates a visual editor for an {@link InputTriggerConfig}. The config
	 * object is directly modified when the user clicks the 'Apply' button.
	 *
	 * @param config
	 *            the {@link InputTriggerConfig} object to modify.
	 * @param commandDescriptions
	 *            The commands available. They are specified as a map from map
	 *            of commands -> map of contexts -> description of what the
	 *            command do in a context. Use <code>null</code> as value to not
	 *            specify a description.
	 * @see CommandDescriptionBuilder
	 */
	public VisualEditorPanel( final InputTriggerConfig config, final Map< String, Map< String, String > > commandDescriptions )
	{

		this.config = config;
		this.actionDescriptions = commandDescriptions;
		this.contexts = new HashSet<>();
		for ( final String command : commandDescriptions.keySet() )
		{
			final Map< String, String > contextMap = commandDescriptions.get( command );
			contexts.addAll( contextMap.keySet() );
		}
		this.listeners = new HashSet<>();

		/*
		 * GUI
		 */

		setLayout( new BorderLayout( 0, 0 ) );

		final JPanel panelFilter = new JPanel();
		add( panelFilter, BorderLayout.NORTH );
		panelFilter.setLayout( new BoxLayout( panelFilter, BoxLayout.X_AXIS ) );

		final Component horizontalStrut = Box.createHorizontalStrut( 5 );
		panelFilter.add( horizontalStrut );

		final JLabel lblFilter = new JLabel( "Filter:" );
		lblFilter.setToolTipText( "Filter on command names. Accept regular expressions." );
		lblFilter.setAlignmentX( Component.CENTER_ALIGNMENT );
		panelFilter.add( lblFilter );

		final Component horizontalStrut_1 = Box.createHorizontalStrut( 5 );
		panelFilter.add( horizontalStrut_1 );

		textFieldFilter = new JTextField();
		panelFilter.add( textFieldFilter );
		textFieldFilter.setColumns( 10 );
		textFieldFilter.getDocument().addDocumentListener( new DocumentListener()
		{

			@Override
			public void removeUpdate( final DocumentEvent e )
			{
				filterRows();
			}

			@Override
			public void insertUpdate( final DocumentEvent e )
			{
				filterRows();
			}

			@Override
			public void changedUpdate( final DocumentEvent e )
			{
				filterRows();
			}
		} );

		panelEditor = new JPanel();
		add( panelEditor, BorderLayout.SOUTH );
		panelEditor.setLayout( new BorderLayout( 0, 0 ) );

		final JPanel panelCommandButtons = new JPanel();
		panelEditor.add( panelCommandButtons, BorderLayout.NORTH );
		panelCommandButtons.setLayout( new BoxLayout( panelCommandButtons, BoxLayout.X_AXIS ) );

		final JButton btnCopyCommand = new JButton( "Copy" );
		btnCopyCommand.setToolTipText( "Duplicate command binding to a new, blank binding." );
		panelCommandButtons.add( btnCopyCommand );

		final JButton btnUnbindAction = new JButton( "Unbind" );
		btnUnbindAction.setToolTipText( "Remove current binding for selected command." );
		panelCommandButtons.add( btnUnbindAction );

		final JButton btnDeleteAction = new JButton( "Unbind all" );
		btnDeleteAction.setToolTipText( "Remove all bindings to selected command." );
		panelCommandButtons.add( btnDeleteAction );

		final Component horizontalGlue = Box.createHorizontalGlue();
		panelCommandButtons.add( horizontalGlue );

		final JButton btnExportCsv = new JButton( "Export CSV" );
		btnExportCsv.setToolTipText( "Export all command bindings to a CSV file." );
		panelCommandButtons.add( btnExportCsv );

		final JPanel panelCommandEditor = new JPanel();
		panelEditor.add( panelCommandEditor, BorderLayout.CENTER );
		final GridBagLayout gbl_panelCommandEditor = new GridBagLayout();
		gbl_panelCommandEditor.rowHeights = new int[] { 0, 0, 0, 0, 60 };
		gbl_panelCommandEditor.columnWidths = new int[] { 30, 100 };
		gbl_panelCommandEditor.columnWeights = new double[] { 0.0, 1.0 };
		gbl_panelCommandEditor.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0 };
		panelCommandEditor.setLayout( gbl_panelCommandEditor );

		final JLabel lblName = new JLabel( "Name:" );
		final GridBagConstraints gbc_lblName = new GridBagConstraints();
		gbc_lblName.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblName.anchor = GridBagConstraints.WEST;
		gbc_lblName.gridx = 0;
		gbc_lblName.gridy = 0;
		panelCommandEditor.add( lblName, gbc_lblName );

		this.labelCommandName = new JLabel();
		final GridBagConstraints gbc_labelActionName = new GridBagConstraints();
		gbc_labelActionName.anchor = GridBagConstraints.WEST;
		gbc_labelActionName.insets = new Insets( 5, 5, 5, 0 );
		gbc_labelActionName.gridx = 1;
		gbc_labelActionName.gridy = 0;
		panelCommandEditor.add( labelCommandName, gbc_labelActionName );

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

		lblConflict = new JLabel( "" );
		lblConflict.setToolTipText( "Conflicts with other commands." );
		lblConflict.setForeground( Color.PINK.darker() );
		lblConflict.setFont( getFont().deriveFont( Font.BOLD ) );
		final GridBagConstraints gbc_lblConflict = new GridBagConstraints();
		gbc_lblConflict.insets = new Insets( 5, 5, 5, 0 );
		gbc_lblConflict.anchor = GridBagConstraints.WEST;
		gbc_lblConflict.gridx = 1;
		gbc_lblConflict.gridy = 3;
		panelCommandEditor.add( lblConflict, gbc_lblConflict );

		final JLabel lblDescription = new JLabel( "Description:" );
		final GridBagConstraints gbc_lblDescription = new GridBagConstraints();
		gbc_lblDescription.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblDescription.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblDescription.gridx = 0;
		gbc_lblDescription.gridy = 4;
		panelCommandEditor.add( lblDescription, gbc_lblDescription );

		final JScrollPane scrollPaneDescription = new JScrollPane();
		scrollPaneDescription.setOpaque( false );
		scrollPaneDescription.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		final GridBagConstraints gbc_scrollPaneDescription = new GridBagConstraints();
		gbc_scrollPaneDescription.insets = new Insets( 5, 5, 5, 5 );
		gbc_scrollPaneDescription.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneDescription.gridx = 1;
		gbc_scrollPaneDescription.gridy = 4;
		panelCommandEditor.add( scrollPaneDescription, gbc_scrollPaneDescription );

		textAreaDescription = new JTextArea();
		textAreaDescription.setRows( 3 );
		textAreaDescription.setFont( getFont().deriveFont( getFont().getSize2D() - 1f ) );
		textAreaDescription.setOpaque( false );
		textAreaDescription.setWrapStyleWord( true );
		textAreaDescription.setEditable( false );
		textAreaDescription.setLineWrap( true );
		textAreaDescription.setFocusable( false );
		scrollPaneDescription.setViewportView( textAreaDescription );

		panelButtons = new JPanel();
		panelEditor.add( panelButtons, BorderLayout.SOUTH );
		final FlowLayout flowLayout = ( FlowLayout ) panelButtons.getLayout();
		flowLayout.setAlignment( FlowLayout.TRAILING );

		final JButton btnRestore = new JButton( "Restore" );
		btnRestore.setToolTipText( "Re-read the key bindings from the config." );
		panelButtons.add( btnRestore );

		final JButton btnApply = new JButton( "Apply" );
		btnApply.setToolTipText( "Write these key bindings in the config." );
		panelButtons.add( btnApply );

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		scrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		add( scrollPane, BorderLayout.CENTER );

		tableBindings = new JTable();
		tableBindings.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		tableBindings.setFillsViewportHeight( true );
		tableBindings.setAutoResizeMode( JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS );
		tableBindings.setRowHeight( 30 );
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
		tableBindings.setFocusTraversalKeys( KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null );
		tableBindings.setFocusTraversalKeys( KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null );

		// Listen to changes in the keybinding editor and forward to table
		// model.
		keybindingEditor.addInputTriggerChangeListener( () -> keybindingsChanged(
				keybindingEditor.getInputTrigger() == null
						? keybindingEditor.getLastValidInputTrigger()
						: keybindingEditor.getInputTrigger() ) );

		// Listen to changes in context editor and forward to table model.
		contextsEditor.addTagSelectionChangeListener( () -> contextsChanged( contextsEditor.getSelectedTags() ) );

		// Button presses.
		btnCopyCommand.addActionListener( ( e ) -> copyCommand() );
		btnUnbindAction.addActionListener( ( e ) -> unbindCommand() );
		btnDeleteAction.addActionListener( ( e ) -> unbindAllCommand() );
		btnExportCsv.addActionListener( ( e ) -> exportToCsv() );
		btnRestore.addActionListener( ( e ) -> configToModel() );
		btnApply.addActionListener( ( e ) -> modelToConfig() );

		configToModel();
		scrollPane.setViewportView( tableBindings );
	}

	private void lookForConflicts()
	{
		final int viewRow = tableBindings.getSelectedRow();
		if ( viewRow < 0 )
			return;
		final int modelRow = tableBindings.convertRowIndexToModel( viewRow );

		lblConflict.setText( "" );
		final InputTrigger inputTrigger = tableModel.bindings.get( modelRow );
		if ( inputTrigger == InputTrigger.NOT_MAPPED )
			return;

		final ArrayList< String > conflicts = new ArrayList<>();
		for ( int i = 0; i < tableModel.commands.size(); i++ )
		{
			if ( i == modelRow )
				continue;

			if ( tableModel.bindings.get( i ).equals( inputTrigger ) )
				conflicts.add( tableModel.commands.get( i ) );
		}

		if ( !conflicts.isEmpty() )
		{
			final StringBuilder str = new StringBuilder( conflicts.get( 0 ) );
			for ( int i = 1; i < conflicts.size(); i++ )
				str.append( ", " + conflicts.get( i ) );
			lblConflict.setText( str.toString() );
		}
	}

	public void setButtonPanelVisible( final boolean visible )
	{
		panelEditor.remove( panelButtons );
		if ( visible )
			panelEditor.add( panelButtons, BorderLayout.SOUTH );
	}

	// TODO Change method name to 'apply()'. API breaking change.
	/**
	 * Copies the settings in this editor to the {@link InputTriggerConfig}
	 * specified at construction. The {@link InputTriggerConfig} is cleared
	 * before copying.
	 */
	public void modelToConfig()
	{
		config.clear();
		for ( int i = 0; i < tableModel.commands.size(); i++ )
		{
			final InputTrigger inputTrigger = tableModel.bindings.get( i );
			if ( inputTrigger == InputTrigger.NOT_MAPPED )
				continue;

			final String action = tableModel.commands.get( i );
			final Set< String > cs = new HashSet<>( tableModel.contexts.get( i ) );
			config.add( inputTrigger, action, cs );
		}

		// fill in InputTrigger.NOT_MAPPED for any action that doesn't have any
		// input
		for ( final String context : contexts )
		{
			final Set< String > cs = Collections.singleton( context );
			for ( final String action : actionDescriptions.keySet() )
				if ( config.getInputs( action, cs ).isEmpty() )
					config.add( InputTrigger.NOT_MAPPED, action, cs );
		}
	}

	public void configToModel()
	{
		tableModel = new MyTableModel( actionDescriptions, config.actionToInputsMap );
		tableBindings.setModel( tableModel );
		// Renderers.
		tableBindings.getColumnModel().getColumn( 1 ).setCellRenderer( new MyBindingsRenderer() );
		tableBindings.getColumnModel().getColumn( 2 ).setCellRenderer( new MyContextsRenderer( contexts ) );
		tableBindings.getSelectionModel().setSelectionInterval( 0, 0 );

		// Notify listeners.
		notifyListeners();
	}

	private void filterRows()
	{
		final TableRowSorter< MyTableModel > tableRowSorter = new TableRowSorter<>( tableModel );
		tableRowSorter.setComparator( 1, new InputTriggerComparator() );
		tableBindings.setRowSorter( tableRowSorter );
		RowFilter< MyTableModel, Integer > rf = null;
		try
		{
			final int[] indices = new int[ tableModel.commands.size() ];
			for ( int i = 0; i < indices.length; i++ )
				indices[ i ] = i;
			rf = RowFilter.regexFilter( textFieldFilter.getText(), 0 );
		}
		catch ( final java.util.regex.PatternSyntaxException pse )
		{
			return;
		}
		tableRowSorter.setRowFilter( rf );
	}

	private final static String CSV_SEPARATOR = ",";

	private void exportToCsv()
	{
		final int userSignal = fileChooser.showSaveDialog( this );
		if ( userSignal != JFileChooser.APPROVE_OPTION )
			return;

		final File file = fileChooser.getSelectedFile();
		if ( file.exists() )
		{
			if ( !file.canWrite() )
			{
				JOptionPane.showMessageDialog( fileChooser, "Cannot write on existing file " + file.getAbsolutePath(), "File error", JOptionPane.ERROR_MESSAGE );
				return;
			}
			final int doOverwrite = JOptionPane.showConfirmDialog( fileChooser, "The file already exists. Do you want to overwrite it?", "Overwrite?", JOptionPane.YES_NO_OPTION );
			if ( doOverwrite != JOptionPane.YES_OPTION )
				return;
		}

		final StringBuilder sb = new StringBuilder();
		sb.append( MyTableModel.TABLE_HEADERS[ 0 ] );
		sb.append( CSV_SEPARATOR + '\t' );
		sb.append( MyTableModel.TABLE_HEADERS[ 1 ] );
		sb.append( CSV_SEPARATOR + '\t' );
		sb.append( MyTableModel.TABLE_HEADERS[ 2 ] );
		sb.append( '\n' );

		for ( int i = 0; i < tableModel.commands.size(); i++ )
		{
			sb.append( tableModel.commands.get( i ) );
			sb.append( CSV_SEPARATOR + '\t' );
			sb.append( tableModel.bindings.get( i ).toString() );
			sb.append( CSV_SEPARATOR + '\t' );
			final List< String > contexts = tableModel.contexts.get( i );
			if ( !contexts.isEmpty() )
			{
				sb.append( contexts.get( 0 ) );
				for ( int j = 1; j < contexts.size(); j++ )
					sb.append( " - " + contexts.get( j ) );
			}
			sb.append( '\n' );
		}

		try (final PrintWriter pw = new PrintWriter( file ))
		{

			pw.write( sb.toString() );
			pw.close();
		}
		catch ( final FileNotFoundException e )
		{
			JOptionPane.showMessageDialog( fileChooser, "Error writing file:\n" + e.getMessage(), "Error writing file.", JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
		}

	}

	private void unbindAllCommand()
	{
		final int viewRow = tableBindings.getSelectedRow();
		if ( viewRow < 0 )
			return;
		final int modelRow = tableBindings.convertRowIndexToModel( viewRow );

		final String action = tableModel.commands.get( modelRow );
		for ( int i = 0; i < tableModel.commands.size(); i++ )
		{
			if ( tableModel.commands.get( i ).equals( action ) )
			{
				tableModel.bindings.set( i, InputTrigger.NOT_MAPPED );
				Map< String, String > contextMap = actionDescriptions.get( action );
				if ( null == contextMap )
					contextMap = Collections.emptyMap();
				final List< String > cs = new ArrayList<>( contextMap.keySet() );
				cs.sort( null );
				tableModel.contexts.set( i, cs );
				tableModel.fireTableRowsUpdated( i, i );
			}
		}
		removeDuplicates();
	}

	private void updateEditors()
	{
		final int viewRow = tableBindings.getSelectedRow();
		if ( viewRow < 0 )
		{
			labelCommandName.setText( "" );
			keybindingEditor.setInputTrigger( InputTrigger.NOT_MAPPED );
			contextsEditor.setTags( Collections.emptyList() );
			textAreaDescription.setText( "" );
			return;
		}

		final int modelRow = tableBindings.convertRowIndexToModel( viewRow );
		final String action = tableModel.commands.get( modelRow );
		final InputTrigger trigger = tableModel.bindings.get( modelRow );
		final List< String > contexts = new ArrayList<>( tableModel.contexts.get( modelRow ) );

		Map< String, String > contextMap = actionDescriptions.get( action );
		if ( null == contextMap )
			contextMap = Collections.emptyMap();
		final String description;
		if ( contextMap.isEmpty() )
			description = "";
		else
		{
			final StringBuilder str = new StringBuilder();
			final Iterator< String > cs = contextMap.keySet().iterator();
			while ( cs.hasNext() )
			{
				final String c = cs.next();
				final String d = contextMap.get( c );
				if ( d != null )
					str.append( "\n\nIn " + c + ":\n" + d );
				else
					str.append( "\n\nIn " + c + " - no description." );
			}
			str.delete( 0, 2 );
			description = str.toString();
		}

		labelCommandName.setText( action );
		keybindingEditor.setInputTrigger( trigger );
		contextsEditor.setAcceptableTags( contextMap.keySet() );
		contextsEditor.setTags( contexts );
		textAreaDescription.setText( description );
		textAreaDescription.setCaretPosition( 0 );

		lookForConflicts();
	}

	private void unbindCommand()
	{
		final int viewRow = tableBindings.getSelectedRow();
		if ( viewRow < 0 )
			return;
		final int modelRow = tableBindings.convertRowIndexToModel( viewRow );

		final InputTrigger inputTrigger = tableModel.bindings.get( modelRow );
		if ( inputTrigger == InputTrigger.NOT_MAPPED )
			return;

		// Update model.
		keybindingsChanged( InputTrigger.NOT_MAPPED );
		final String command = tableModel.commands.get( modelRow );
		Map< String, String > contextMap = actionDescriptions.get( command );
		if ( null == contextMap )
			contextMap = Collections.emptyMap();
		final List< String > cs = new ArrayList<>( contextMap.keySet() );
		cs.sort( null );
		contextsChanged( cs );

		// Find whether we have two lines with the same action, unbound.
		removeDuplicates();
	}

	private void removeDuplicates()
	{
		final Map< String, Set< InputTrigger > > bindings = new HashMap<>();
		final List< Integer > toRemove = new ArrayList<>();
		for ( int row = 0; row < tableModel.getRowCount(); row++ )
		{
			final String action = tableModel.commands.get( row );
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
			tableModel.commands.remove( row );
			tableModel.bindings.remove( row );
			tableModel.contexts.remove( row );
			tableModel.fireTableRowsDeleted( row, row );
		}

		// Notify listeners.
		if ( !toRemove.isEmpty() )
			notifyListeners();

		updateEditors();

	}

	private void copyCommand()
	{
		final int viewRow = tableBindings.getSelectedRow();
		if ( viewRow < 0 )
			return;
		final int modelRow = tableBindings.convertRowIndexToModel( viewRow );

		final String action = tableModel.commands.get( modelRow );
		final List< String > cs = tableModel.contexts.get( modelRow );
		// Check whether there is already a line in the table without a binding.
		for ( int i = 0; i < tableModel.commands.size(); i++ )
		{
			// Brute force.
			if ( tableModel.commands.get( i ).equals( action )
					&& new HashSet<>( cs ).equals( new HashSet<>( tableModel.contexts.get( i ) ) )
					&& tableModel.bindings.get( i ) == InputTrigger.NOT_MAPPED )
				return;
		}

		// Create one then.
		tableModel.commands.add( modelRow + 1, action );
		tableModel.bindings.add( modelRow + 1, InputTrigger.NOT_MAPPED );
		tableModel.contexts.add( modelRow + 1, cs );
		tableModel.fireTableRowsInserted( modelRow, modelRow );

		// Notify listeners.
		notifyListeners();
	}

	private void keybindingsChanged( final InputTrigger inputTrigger )
	{
		final int viewRow = tableBindings.getSelectedRow();
		if ( viewRow < 0 )
			return;
		final int modelRow = tableBindings.convertRowIndexToModel( viewRow );

		tableModel.bindings.set( modelRow, inputTrigger );
		tableModel.fireTableCellUpdated( modelRow, 1 );
		lookForConflicts();

		// Notify listeners.
		notifyListeners();
	}

	private void contextsChanged( final List< String > selectedContexts )
	{
		final int viewRow = tableBindings.getSelectedRow();
		if ( viewRow < 0 )
			return;
		final int modelRow = tableBindings.convertRowIndexToModel( viewRow );

		tableModel.contexts.set( modelRow, new ArrayList<>( selectedContexts ) );
		tableModel.fireTableCellUpdated( modelRow, 2 );

		// Check whether we have lost some contexts known for this command.
		final String command = tableModel.commands.get( modelRow );
		Map< String, String > contextMap = actionDescriptions.get( command );
		if ( null == contextMap )
			contextMap = Collections.emptyMap();
		final Set< String > missingContexts = new HashSet<>( contextMap.keySet() );
		// Brute force
		for ( int i = 0; i < tableModel.commands.size(); i++ )
		{
			if ( command.equals( tableModel.commands.get( i ) ) )
				missingContexts.removeAll( tableModel.contexts.get( i ) );
		}
		// Recreate missing contexts as unbound line.
		if ( !missingContexts.isEmpty() )
		{
			final List< String > cs = new ArrayList<>( missingContexts );
			cs.sort( null );
			tableModel.commands.add( modelRow + 1, command );
			tableModel.bindings.add( modelRow + 1, InputTrigger.NOT_MAPPED );
			tableModel.contexts.add( modelRow + 1, cs );
			tableModel.fireTableRowsInserted( modelRow + 1, modelRow + 1 );
		}

		// Notify listeners.
		notifyListeners();
	}

	private void notifyListeners()
	{
		for ( final ConfigChangeListener listener : listeners )
			listener.configChanged();
	}

	public void addConfigChangeListener( final ConfigChangeListener listener )
	{
		listeners.add( listener );
	}

	public void removeConfigChangeListener( final ConfigChangeListener listener )
	{
		listeners.remove( listener );
	}

	/*
	 * INNER CLASSES
	 */

	private final class MyContextsRenderer extends TagPanelEditor implements TableCellRenderer
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
			final int modelRow = tableBindings.convertRowIndexToModel( row );
			final String command = tableModel.commands.get( modelRow );
			Map< String, String > contextMap = actionDescriptions.get( command );
			if ( null == contextMap )
				contextMap = Collections.emptyMap();
			setAcceptableTags( contextMap.keySet() );

			@SuppressWarnings( "unchecked" )
			final List< String > contexts = ( List< String > ) value;
			if ( contexts.isEmpty() )
				setBackground( Color.PINK );
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

		private static final String[] TABLE_HEADERS = new String[] { "Command", "Binding", "Contexts" };

		private final List< String > commands;

		private final List< InputTrigger > bindings;

		private final List< List< String > > contexts;

		public MyTableModel( final Map< String, Map< String, String > > actionDescriptions, final Map< String, Set< Input > > actionToInputsMap )
		{
			this.commands = new ArrayList<>();
			this.bindings = new ArrayList<>();
			this.contexts = new ArrayList<>();

			final Set< String > allCommands = new HashSet<>();
			allCommands.addAll( actionDescriptions.keySet() );
			allCommands.addAll( actionToInputsMap.keySet() );
			final List< String > sortedCommands = new ArrayList<>( allCommands );
			sortedCommands.sort( null );
			final InputComparator inputComparator = new InputComparator();
			for ( final String command : sortedCommands )
			{
				Map< String, String > contextMap = actionDescriptions.get( command );
				if ( null == contextMap )
					contextMap = Collections.emptyMap();

				final Set< Input > inputs = actionToInputsMap.get( command );
				if ( null == inputs )
				{
					// Not found in the config. Add command with due contexts.
					commands.add( command );
					bindings.add( InputTrigger.NOT_MAPPED );
					final List< String > cs = new ArrayList<>( contextMap.keySet() );
					cs.sort( null );
					contexts.add( cs );
				}
				else
				{
					// Found in the config. 
					final List< Input > sortedInputs = new ArrayList<>( inputs );
					sortedInputs.sort( inputComparator );
					final Set< String > usedContexts = new HashSet<>();
					for ( final Input input : sortedInputs )
					{
						commands.add( command );
						bindings.add( input.trigger );
						final List< String > cs = new ArrayList<>( input.contexts );
						cs.sort( null );
						contexts.add( cs );
						usedContexts.addAll( cs );
					}
					/*
					 * Check that we have exhausted known contexts. If not, push
					 * missing ones.
					 */
					final Set< String > missingContexts = new HashSet<>( contextMap.keySet() );
					missingContexts.removeAll( usedContexts );
					if ( !missingContexts.isEmpty() )
					{
						final List< String > sortedMissingContexts = new ArrayList<>( missingContexts );
						sortedMissingContexts.sort( null );
						commands.add( command );
						bindings.add( InputTrigger.NOT_MAPPED );
						contexts.add( sortedMissingContexts );
					}
				}
			}
		}

		@Override
		public int getRowCount()
		{
			return commands.size();
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
				return commands.get( rowIndex );
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

	private static final class InputTriggerComparator implements Comparator< InputTrigger >
	{

		@Override
		public int compare( final InputTrigger o1, final InputTrigger o2 )
		{
			if ( o1 == InputTrigger.NOT_MAPPED )
				return 1;
			if ( o2 == InputTrigger.NOT_MAPPED )
				return -1;
			return o1.toString().compareTo( o2.toString() );
		}
	}
}
