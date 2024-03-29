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
package org.scijava.ui.behaviour.io.gui;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import org.scijava.listeners.Listeners;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerDescription;
import org.scijava.ui.behaviour.io.InputTriggerDescriptionsBuilder;

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
	public interface ConfigChangeListener
	{
		/**
		 * Called when settings are changed in the visual editor.
		 */
		void configChanged();
	}

	private JTextField textFieldFilter;

	private MyTableModel tableModel;

	private TableRowSorter< MyTableModel > tableRowSorter;

	private boolean blockRemoveNotMapped = false;

	private final InputTriggerPanelEditor keybindingEditor;

	private final TagPanelEditor contextsEditor;

	private final JLabel labelCommandName;

	private final JTable tableBindings;

	private final InputTriggerConfig config;

	private final Set< Command > commands;

	private final Map< String, Set< String > > commandNameToAcceptableContexts;

	private final Map< Command, String > actionDescriptions;

	private final JLabel lblConflict;

	private final JTextArea textAreaDescription;

	private final JPanel panelEditor;

	private final JPanel panelButtons;

	/**
	 * Set of listeners that are triggered whenever any single item change in the GUI.
	 * This, however, does not mean that the underlying {@link InputTriggerConfig} was
	 * changed at all as the GUI is buffering changes until the "Apply" button is pressed.
	 */
	private final Listeners.List< ConfigChangeListener > modelChangedListeners;

	/**
	 * Set of listeners that are triggered only when the "Apply" button is pressed,
	 * which is precisely the moment when the current state/content of GUI is committed
	 * to the underlying {@link InputTriggerConfig} via the ModelToConfig().
	 */
	private final Listeners.List< ConfigChangeListener > configCommittedListeners;

	private final JButton btnApply;

	private final JButton btnRestore;

	/**
	 * Creates a visual editor for an {@link InputTriggerConfig}. The config
	 * object is directly modified when the user clicks the 'Apply' button.
	 *
	 * @param config
	 *            the {@link InputTriggerConfig} object to modify.
	 */
	public VisualEditorPanel( final InputTriggerConfig config )
	{
		this( config, extractEmptyCommandDescriptions( config ) );
	}

	/**
	 * Creates a visual editor for an {@link InputTriggerConfig}. The config
	 * object is directly modified when the user clicks the 'Apply' button.
	 *
	 * @param config
	 *            the {@link InputTriggerConfig} object to modify.
	 * @param commandDescriptions
	 *            The commands available. They are specified as a map from
	 *            command to description. Use <code>null</code> as value to not
	 *            specify a description.
	 * @see CommandDescriptionsBuilder
	 */
	public VisualEditorPanel( final InputTriggerConfig config, final Map< Command, String > commandDescriptions )
	{

		this.config = config;
		this.actionDescriptions = commandDescriptions;
		this.commands = commandDescriptions.keySet();
		commandNameToAcceptableContexts = new HashMap<>();
		for ( final Command command : commands )
			commandNameToAcceptableContexts.computeIfAbsent( command.getName(), k -> new HashSet<>() ).add( command.getContext() );
		this.modelChangedListeners = new Listeners.SynchronizedList<>();
		this.configCommittedListeners = new Listeners.SynchronizedList<>();

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

		this.contextsEditor = new TagPanelEditor( Collections.emptyList() );
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

		this.btnRestore = new JButton( "Restore" );
		btnRestore.setToolTipText( "Re-read the key bindings from the config." );
		panelButtons.add( btnRestore );

		this.btnApply = new JButton( "Apply" );
		btnApply.setToolTipText( "Write these key bindings in the config." );
		panelButtons.add( btnApply );

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		scrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		add( scrollPane, BorderLayout.CENTER );

		tableBindings = new JTable() {
			@Override
			public void updateUI()
			{
				super.updateUI();
				setRowHeight( ( int ) ( getFontMetrics( getFont() ).getHeight() * 1.5 ) );
			}
		};
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
				updateEditors();
			}
		} );
		tableBindings.getSelectionModel().addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( final ListSelectionEvent e )
			{
				if ( e.getValueIsAdjusting() )
					return;

				if ( blockRemoveNotMapped )
				{
					blockRemoveNotMapped = false;
					return;
				}

				final int selIndex = tableBindings.getSelectionModel().getMinSelectionIndex();
				if ( selIndex < 0 )
					return;
				final MyTableRow selectedRowToRestore = tableModel.rows.get( selIndex );
				if ( !tableModel.removeSuperfluousNotMapped() )
					return;

				final int bs = Collections.binarySearch( tableModel.rows, selectedRowToRestore, MyTableRowComparator );
				if ( bs < 0 )
					return;
				final int vbs = tableBindings.convertRowIndexToView( bs );
				tableBindings.getSelectionModel().setSelectionInterval( vbs, vbs );
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

		// Buttons re-enabling when model and config are out of sync.
		modelChangedListeners.add( () -> {
			btnApply.setEnabled( true );
			btnRestore.setEnabled( true );
		} );

		configToModel();
		tableBindings.getRowSorter().toggleSortOrder( 0 );
		if ( tableBindings.getRowCount() > 0 )
			tableBindings.getSelectionModel().setSelectionInterval( 0, 0);

		scrollPane.setViewportView( tableBindings );
	}

	private void lookForConflicts()
	{
		lblConflict.setText( "" );

		final int viewRow = tableBindings.getSelectedRow();
		if ( viewRow < 0 )
			return;
		final int modelRow = tableBindings.convertRowIndexToModel( viewRow );

		lblConflict.setText( "" );
		final InputTrigger inputTrigger = tableModel.rows.get( modelRow ).getTrigger();
		if ( inputTrigger == InputTrigger.NOT_MAPPED )
			return;
		final List< String > contexts = tableModel.rows.get( modelRow ).getContexts();

		final ArrayList< String > conflicts = new ArrayList<>();
		for ( int i = 0; i < tableModel.getRowCount(); i++ )
		{
			if ( i == modelRow )
				continue;

			if ( tableModel.rows.get( i ).getTrigger().equals( inputTrigger ) )
			{
				// Same trigger. Check if contexts overlap.
				final List< String > overlappingContexts = new ArrayList<>( tableModel.rows.get( i ).getContexts() );
				overlappingContexts.retainAll( contexts );
				if ( !overlappingContexts.isEmpty() )
				{
					final StringBuilder str = new StringBuilder();
					str.append( tableModel.rows.get( i ).getName() );
					str.append( " in " ).append( overlappingContexts.get( 0 ) );
					for ( int j = 1; j < overlappingContexts.size(); j++ )
						str.append( ", " ).append( overlappingContexts.get( j ) );

					conflicts.add( str.toString() );
				}
			}
		}

		if ( !conflicts.isEmpty() )
		{
			final StringBuilder str = new StringBuilder( conflicts.get( 0 ) );
			for ( int i = 1; i < conflicts.size(); i++ )
				str.append( "; " ).append( conflicts.get( i ) );
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
		for ( final MyTableRow row : tableModel.rows )
		{
			final InputTrigger inputTrigger = row.getTrigger();
			if ( inputTrigger == InputTrigger.NOT_MAPPED )
				continue;

			final String action = row.getName();
			config.add( inputTrigger, action, row.getContexts() );
		}

		// fill in InputTrigger.NOT_MAPPED for any action that doesn't have any input
		for ( final Command command : commands )
		{
			final String action = command.getName();
			if ( config.getInputs( action, command.getContext() ).isEmpty() )
				config.add( InputTrigger.NOT_MAPPED, action, command.getContext() );
		}

		btnApply.setEnabled( false );
		btnRestore.setEnabled( false );

		configCommittedListeners.list.forEach( ConfigChangeListener::configChanged );
	}

	public void configToModel()
	{
		tableModel = new MyTableModel( commands, config );
		tableBindings.setModel( tableModel );

		tableRowSorter = new TableRowSorter<>( tableModel );
		tableRowSorter.setComparator( 1, InputTriggerComparator );
		tableBindings.setRowSorter( tableRowSorter );
		filterRows();

		// Renderers.
		tableBindings.getColumnModel().getColumn( 1 ).setCellRenderer( new MyBindingsRenderer() );
		tableBindings.getColumnModel().getColumn( 2 ).setCellRenderer( new MyContextsRenderer( Collections.emptyList() ) );

		// Notify listeners.
		notifyListeners();

		btnApply.setEnabled( false );
		btnRestore.setEnabled( false );
	}

	private void filterRows()
	{
		final String regex =  textFieldFilter.getText();
		final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE );
		final Matcher matcher = pattern.matcher( "" );
		final RowFilter< MyTableModel, Integer > rf = new RowFilter< MyTableModel, Integer >()
		{

			@Override
			public boolean include( final Entry< ? extends MyTableModel, ? extends Integer > entry )
			{
				int count = entry.getValueCount();
				while ( --count >= 0 )
				{
					matcher.reset( entry.getStringValue( count ) );
					if ( matcher.find() ) { return true; }
				}
				return false;
			}
		};
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

		for ( int i = 0; i < tableModel.getRowCount(); i++ )
		{
			sb.append( tableModel.rows.get( i ).getName() );
			sb.append( CSV_SEPARATOR + '\t' );
			sb.append( tableModel.rows.get( i ).getTrigger().toString() );
			sb.append( CSV_SEPARATOR + '\t' );
			final List< String > contexts = tableModel.rows.get( i ).getContexts();
			if ( !contexts.isEmpty() )
			{
				sb.append( contexts.get( 0 ) );
				for ( int j = 1; j < contexts.size(); j++ )
					sb.append( " - " ).append( contexts.get( j ) );
			}
			sb.append( '\n' );
		}

		try ( final PrintWriter pw = new PrintWriter( file ) )
		{
			pw.write( sb.toString() );
		}
		catch ( final FileNotFoundException e )
		{
			JOptionPane.showMessageDialog( fileChooser, "Error writing file:\n" + e.getMessage(), "Error writing file.", JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
		}
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
		final MyTableRow row = tableModel.rows.get( modelRow );
		final String action = row.getName();
		final InputTrigger trigger = row.getTrigger();
		final List< String > contexts = row.getContexts();

		final String description;
		final Set< String > acceptableContexts = commandNameToAcceptableContexts.get( action );
		if ( acceptableContexts.isEmpty() )
		{
			description = "";
		}
		else
		{
			final StringBuilder str = new StringBuilder();
			for ( final String context : acceptableContexts )
			{
				final String d = actionDescriptions.get( new Command( action, context ) );
				if ( d != null )
					str.append( "\n\nIn " ).append( context ).append( ":\n" ).append( d );
				else
					str.append( "\n\nIn " ).append( context ).append( " - no description." );
			}
			str.delete( 0, 2 );
			description = str.toString();
		}

		labelCommandName.setText( action );
		keybindingEditor.setInputTrigger( trigger );
		contextsEditor.setAcceptableTags( acceptableContexts );
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
		tableModel.rows.remove( modelRow );
		if ( !tableModel.addMissingRows() )
			tableModel.fireTableRowsDeleted( modelRow, modelRow );

		// Notify listeners.
		notifyListeners();
	}

	private void unbindAllCommand()
	{
		final int viewRow = tableBindings.getSelectedRow();
		if ( viewRow < 0 )
			return;
		final int modelRow = tableBindings.convertRowIndexToModel( viewRow );
		final String removeName = tableModel.rows.get( modelRow ).getName();
		tableModel.rows.removeIf( row -> row.getName().equals( removeName ) );
		if ( !tableModel.addMissingRows() )
			tableModel.fireTableDataChanged();

		// Notify listeners.
		notifyListeners();
	}

	private void copyCommand()
	{
		final int viewRow = tableBindings.getSelectedRow();
		if ( viewRow < 0 )
			return;
		final int modelRow = tableBindings.convertRowIndexToModel( viewRow );
		final MyTableRow row = tableModel.rows.get( modelRow );

		final MyTableRow copiedRow = new MyTableRow( row.getName(), InputTrigger.NOT_MAPPED, row.getContexts() );
		tableModel.rows.add( modelRow + 1, copiedRow  );
		blockRemoveNotMapped = true;
		if ( !tableModel.mergeRows() )
			tableModel.fireTableRowsInserted( modelRow + 1, modelRow + 1 );

		blockRemoveNotMapped = true;
		// Find the row we just added if any.
		final int modelRowToSelect = Collections.binarySearch( tableModel.rows, copiedRow, MyTableRowComparator );
		final int rowToSelect;
		if ( modelRowToSelect < 0 )
			rowToSelect = tableBindings.convertRowIndexToView( modelRow );
		else
			rowToSelect = tableBindings.convertRowIndexToView( modelRowToSelect );
		tableBindings.getSelectionModel().setSelectionInterval( rowToSelect, rowToSelect );

		keybindingEditor.requestFocusInWindow();
	}

	private void keybindingsChanged( final InputTrigger inputTrigger )
	{
		final int viewRow = tableBindings.getSelectedRow();
		if ( viewRow < 0 )
			return;
		final int modelRow = tableBindings.convertRowIndexToModel( viewRow );
		final MyTableRow row = tableModel.rows.get( modelRow );

		final MyTableRow updatedRow = new MyTableRow( row.getName(), inputTrigger, row.getContexts() );
		tableModel.rows.set( modelRow, updatedRow );
		if ( !tableModel.mergeRows() )
			tableModel.fireTableRowsUpdated( modelRow, modelRow );
		lookForConflicts();

		final int modelRowToSelect = Collections.binarySearch( tableModel.rows, updatedRow, MyTableRowComparator );
		final int rowToSelect;
		if ( modelRowToSelect < 0 )
			rowToSelect = tableBindings.convertRowIndexToView( modelRow );
		else
			rowToSelect = tableBindings.convertRowIndexToView( modelRowToSelect );
		blockRemoveNotMapped = true;
		tableBindings.getSelectionModel().setSelectionInterval( rowToSelect, rowToSelect );

		// Notify listeners.
		notifyListeners();
	}

	private void contextsChanged( final List< String > selectedContexts )
	{
		final int viewRow = tableBindings.getSelectedRow();
		if ( viewRow < 0 )
			return;
		final int modelRow = tableBindings.convertRowIndexToModel( viewRow );
		final MyTableRow row = tableModel.rows.get( modelRow );

		final List< String > newContexts = new ArrayList<>( selectedContexts );
		newContexts.sort( null );
		tableModel.rows.set( modelRow, new MyTableRow( row.getName(), row.getTrigger(), newContexts ) );
		if ( !tableModel.addMissingRows() )
			tableModel.fireTableRowsUpdated( modelRow, modelRow );

		// Notify listeners.
		notifyListeners();

		// Select proper row again (might have sorted differently now).
		final int viewRowToSelect = tableBindings.convertRowIndexToView( modelRow );
		if ( viewRowToSelect < 0 )
			return;
		tableBindings.getSelectionModel().setSelectionInterval( viewRowToSelect, viewRowToSelect );
	}

	private void notifyListeners()
	{
		modelChangedListeners.list.forEach( ConfigChangeListener::configChanged );
	}

	private static Map< Command, String > extractEmptyCommandDescriptions( final InputTriggerConfig keyconf )
	{
		final List< InputTriggerDescription > descriptions = new InputTriggerDescriptionsBuilder( keyconf ).getDescriptions();
		final Set< Command > commands = new LinkedHashSet<>();
		for ( final InputTriggerDescription desc : descriptions )
			for( final String context : desc.getContexts() )
				commands.add( new Command( desc.getAction(), context ) );
		final Map< Command, String > commandDescriptions = new HashMap<>();
		commands.forEach( command -> commandDescriptions.put( command, null ) );
		return commandDescriptions;
	}

	/**
	 * @deprecated Use {@code modelChangedListeners()} instead.
	 */
	@Deprecated
	public Listeners< ConfigChangeListener > configChangeListeners()
	{
		return modelChangedListeners();
	}

	/**
	 * @deprecated Use {@code modelChangedListeners().add(listener)} instead.
	 */
	@Deprecated
	public void addConfigChangeListener( final ConfigChangeListener listener )
	{
		modelChangedListeners().add( listener );
	}

	/**
	 * @deprecated Use {@code modelChangedListeners().remove(listener)} instead.
	 */
	@Deprecated
	public void removeConfigChangeListener( final ConfigChangeListener listener )
	{
		modelChangedListeners().remove( listener );
	}

	/**
	 * Please, see the documentation of {@link VisualEditorPanel#modelChangedListeners}
	 * and {@link VisualEditorPanel#configCommittedListeners} to understand when these
	 * listeners are triggered. In short, listeners here are triggered anytime a GUI item is changed.
	 */
	public Listeners< ConfigChangeListener > modelChangedListeners()
	{
		return modelChangedListeners;
	}

	/**
	 * Please, see the documentation of {@link VisualEditorPanel#modelChangedListeners}
	 * and {@link VisualEditorPanel#configCommittedListeners} to understand when these
	 * listeners are triggered. In short, listeners here are triggered only when "Apply" button is pressed.
	 */
	public Listeners< ConfigChangeListener > configCommittedListeners()
	{
		return configCommittedListeners;
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
		}

		@Override
		public void updateUI()
		{
			super.updateUI();
			setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		}

		@Override
		public Component getTableCellRendererComponent( final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			setForeground( isSelected ? table.getSelectionForeground() : table.getForeground() );
			setBackground( isSelected ? table.getSelectionBackground() : table.getBackground() );
			final int modelRow = tableBindings.convertRowIndexToModel( row );
			final String name = tableModel.rows.get( modelRow ).getName();
			setAcceptableTags( commandNameToAcceptableContexts.get( name ) );

			@SuppressWarnings( "unchecked" )
			final List< String > contexts = value != null
					? ( List< String > ) value
					: Collections.emptyList();
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
		}

		@Override
		public void updateUI()
		{
			super.updateUI();
			setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
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

	private static class MyTableRow
	{
		private final String name;

		private final InputTrigger trigger;

		private final List< String > contexts;

		public MyTableRow( final String name, final InputTrigger trigger, final String context )
		{
			this( name, trigger, Collections.singletonList( context ) );
		}

		public MyTableRow( final String name, final InputTrigger trigger, final Collection< String > contexts )
		{
			this.name = name;
			this.trigger = trigger;
			this.contexts = new ArrayList<>( contexts );
		}

		public String getName()
		{
			return name;
		}

		public InputTrigger getTrigger()
		{
			return trigger;
		}

		public List< String > getContexts()
		{
			return contexts;
		}

		@Override
		public boolean equals( final Object o )
		{
			if ( this == o )
				return true;
			if ( o == null || getClass() != o.getClass() )
				return false;

			final MyTableRow that = ( MyTableRow ) o;

			if ( !name.equals( that.name ) )
				return false;
			if ( !trigger.equals( that.trigger ) )
				return false;
			return contexts.equals( that.contexts );
		}

		@Override
		public int hashCode()
		{
			int result = name.hashCode();
			result = 31 * result + trigger.hashCode();
			result = 31 * result + contexts.hashCode();
			return result;
		}

		@Override
		public String toString()
		{
			return "MyTableRow{" +
					"name='" + name + '\'' +
					", trigger=" + trigger +
					", contexts=" + contexts +
					'}';
		}
	}

	private static class MyTableModel extends AbstractTableModel
	{

		private static final long serialVersionUID = 1L;

		private static final String[] TABLE_HEADERS = new String[] { "Command", "Binding", "Contexts" };

		private final List< MyTableRow > rows;

		private final Set< Command > allCommands;

		public MyTableModel( final Set< Command > commands, final InputTriggerConfig config )
		{
			rows = new ArrayList<>();
			allCommands = commands;
			for ( final Command command : commands )
			{
				final Set< InputTrigger > inputs = config.getInputs( command.getName(), command.getContext() );
				for ( final InputTrigger input : inputs )
					rows.add( new MyTableRow( command.getName(), input, command.getContext() ) );
			}
			addMissingRows();
		}

		/**
		 * Find and remove rows with trigger NOT_MAPPED.
		 */
		public void removeAllNotMapped( final List< MyTableRow > rows )
		{
			rows.removeIf( row -> row.getTrigger().equals( InputTrigger.NOT_MAPPED ) );
		}

		/**
		 * Find and remove table rows with trigger {@code NOT_MAPPED}, whose
		 * name and contexts are covered by other rows (that map to other
		 * triggers).
		 *
		 * If any changes are made, {@code fireTableDataChanged} is fired.
		 *
		 * @return true, if changes were made.
		 */
		public boolean removeSuperfluousNotMapped()
		{
			final ArrayList< MyTableRow > copy = new ArrayList<>( rows );
			removeAllNotMapped( rows );
			addMissingRows( rows );
			if ( !copy.equals( rows ) )
			{
				this.fireTableDataChanged();
				return true;
			}
			return false;
		}

		/**
		 * Find and merge table rows with the same action name and trigger, but
		 * different contexts.
		 *
		 * If any changes are made, {@code fireTableDataChanged} is fired.
		 *
		 * @return true, if changes were made.
		 */
		private boolean mergeRows()
		{
			final ArrayList< MyTableRow > copy = new ArrayList<>( rows );
			mergeRows( rows );
			if ( !copy.equals( rows ) )
			{
				this.fireTableDataChanged();
				return true;
			}
			return false;
		}

		/**
		 * In the given list of {@code rows}, find and merge rows with the same
		 * action name and trigger, but different contexts.
		 *
		 * @param rows list of rows to modify.
		 */
		private void mergeRows( final List< MyTableRow > rows )
		{
			final List< MyTableRow > rowsUnmerged = new ArrayList<>( rows );
			rows.clear();

			rowsUnmerged.sort( MyTableRowComparator );

			for ( int i = 0; i < rowsUnmerged.size(); )
			{
				final MyTableRow rowA = rowsUnmerged.get( i );
				int j = i + 1;
				while ( j < rowsUnmerged.size() && MyTableRowComparator.compare( rowsUnmerged.get( j ), rowA ) == 0 )
					++j;

				final Set< String > contexts = new HashSet<>();
				for ( int k = i; k < j; ++k )
					contexts.addAll( rowsUnmerged.get( k ).getContexts() );

				rows.add( new MyTableRow( rowA.getName(), rowA.getTrigger(), contexts ) );

				i = j;
			}
		}

		/**
		 * Add {@code NOT_MAPPED} rows for (name, context) pairs in
		 * {@link #allCommands} that are not otherwise covered. Then
		 * {@link #mergeRows()}.
		 *
		 * If any changes are made, {@code fireTableDataChanged} is fired.
		 *
		 * @return true, if changes were made.
		 */
		private boolean addMissingRows()
		{
			final ArrayList< MyTableRow > copy = new ArrayList<>( rows );
			addMissingRows( rows );
			if ( !copy.equals( rows ) )
			{
				this.fireTableDataChanged();
				return true;
			}
			return false;
		}

		/**
		 * In the given list of {@code rows}, add {@code NOT_MAPPED} rows for
		 * (name, context) pairs in {@link #allCommands} that are not otherwise
		 * covered. Then {@link #mergeRows(List)}.
		 *
		 * @param rows
		 *            list of rows to modify.
		 */
		private void addMissingRows( final List< MyTableRow > rows )
		{
			final ArrayList< Command > missingCommands = new ArrayList<>();
			for ( final Command command : allCommands )
			{
				boolean found = false;
				for ( final MyTableRow row : rows )
				{
					if ( row.getName().equals( command.getName() ) && row.getContexts().contains( command.getContext() ) )
					{
						found = true;
						break;
					}
				}
				if ( !found )
					missingCommands.add( command );
			}

			for ( final Command command : missingCommands )
				rows.add( new MyTableRow( command.getName(), InputTrigger.NOT_MAPPED, command.getContext() ) );

			mergeRows( rows );
		}

		@Override
		public int getRowCount()
		{
			return rows.size();
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
				return rows.get( rowIndex ).getName();
			case 1:
				return rows.get( rowIndex ).getTrigger();
			case 2:
				return rows.get( rowIndex ).getContexts();
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

	private static final Comparator< MyTableRow > MyTableRowComparator = new Comparator< MyTableRow >()
	{
		@Override
		public int compare( final MyTableRow o1, final MyTableRow o2 )
		{
			final int cn = o1.name.compareTo( o2.name );
			if ( cn != 0 )
				return cn;

			return compare( o1.trigger, o2.trigger );
		}

		private int compare( final InputTrigger o1, final InputTrigger o2 )
		{
			if ( o1 == InputTrigger.NOT_MAPPED )
				return o2 == InputTrigger.NOT_MAPPED ? 0 : 1;
			if ( o2 == InputTrigger.NOT_MAPPED )
				return -1;
			return o1.toString().compareTo( o2.toString() );
		}
	};

	private static final Comparator< InputTrigger > InputTriggerComparator = new Comparator< InputTrigger >()
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
	};
}
