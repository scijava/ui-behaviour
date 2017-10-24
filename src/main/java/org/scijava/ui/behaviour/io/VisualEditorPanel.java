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
import java.util.Collections;
import java.util.Comparator;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.InputTriggerConfig.Input;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

public class VisualEditorPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private JTextField textFieldFilter;

	private JTextField textFieldBinding;

	private InputTriggerConfig config;

	private Set< String > actions;

	private Set< String > contexts;

	private final MyTableModel tableModel;

	/**
	 * Create the panel.
	 */
	public VisualEditorPanel( final InputTriggerConfig config, final Set< String > actions, final Set< String > contexts )
	{
		this.config = config;
		this.actions = actions;
		this.contexts = contexts;

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

		final JButton btnCopyCommand = new JButton( "Copy binding" );
		panelCommandButtons.add( btnCopyCommand );

		final JButton btnUnbindAction = new JButton( "Unbind action" );
		panelCommandButtons.add( btnUnbindAction );

		final JButton btnDeleteAction = new JButton( "Delete binding" );
		panelCommandButtons.add( btnDeleteAction );

		final Component horizontalGlue = Box.createHorizontalGlue();
		panelCommandButtons.add( horizontalGlue );

		final JButton btnExportCsv = new JButton( "Export CSV" );
		panelCommandButtons.add( btnExportCsv );

		final JPanel panelCommandEditor = new JPanel();
		panelEditor.add( panelCommandEditor, BorderLayout.CENTER );
		final GridBagLayout gbl_panelCommandEditor = new GridBagLayout();
		gbl_panelCommandEditor.columnWidths = new int[] { 30, 100, 30, 0, 0 };
		gbl_panelCommandEditor.rowHeights = new int[] { 20, 20, 20 };
		gbl_panelCommandEditor.columnWeights = new double[] { 0.0, 1.0, 0.0, 1.0, 0.0 };
		gbl_panelCommandEditor.rowWeights = new double[] { 0.0, 0.0, 0.0 };
		panelCommandEditor.setLayout( gbl_panelCommandEditor );

		final JLabel lblName = new JLabel( "Name:" );
		final GridBagConstraints gbc_lblName = new GridBagConstraints();
		gbc_lblName.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblName.anchor = GridBagConstraints.WEST;
		gbc_lblName.gridx = 0;
		gbc_lblName.gridy = 0;
		panelCommandEditor.add( lblName, gbc_lblName );

		final JLabel labelActionName = new JLabel( "<>" );
		final GridBagConstraints gbc_labelActionName = new GridBagConstraints();
		gbc_labelActionName.insets = new Insets( 5, 5, 5, 5 );
		gbc_labelActionName.gridx = 1;
		gbc_labelActionName.gridy = 0;
		panelCommandEditor.add( labelActionName, gbc_labelActionName );

		final JLabel lblBinding = new JLabel( "Binding:" );
		final GridBagConstraints gbc_lblBinding = new GridBagConstraints();
		gbc_lblBinding.anchor = GridBagConstraints.WEST;
		gbc_lblBinding.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblBinding.gridx = 2;
		gbc_lblBinding.gridy = 0;
		panelCommandEditor.add( lblBinding, gbc_lblBinding );

		textFieldBinding = new JTextField();
		final GridBagConstraints gbc_textFieldBinding = new GridBagConstraints();
		gbc_textFieldBinding.insets = new Insets( 5, 5, 5, 5 );
		gbc_textFieldBinding.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldBinding.gridx = 3;
		gbc_textFieldBinding.gridy = 0;
		panelCommandEditor.add( textFieldBinding, gbc_textFieldBinding );
		textFieldBinding.setColumns( 10 );

		final JButton buttonSpecialChar = new JButton( "<" );
		final GridBagConstraints gbc_buttonSpecialChar = new GridBagConstraints();
		gbc_buttonSpecialChar.insets = new Insets( 5, 5, 5, 5 );
		gbc_buttonSpecialChar.gridx = 4;
		gbc_buttonSpecialChar.gridy = 0;
		panelCommandEditor.add( buttonSpecialChar, gbc_buttonSpecialChar );

		final JLabel lblDescription = new JLabel( "Description:" );
		final GridBagConstraints gbc_lblDescription = new GridBagConstraints();
		gbc_lblDescription.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblDescription.anchor = GridBagConstraints.WEST;
		gbc_lblDescription.gridx = 0;
		gbc_lblDescription.gridy = 1;
		panelCommandEditor.add( lblDescription, gbc_lblDescription );

		final JLabel labelActionDescription = new JLabel( "<>" );
		final GridBagConstraints gbc_labelActionDescription = new GridBagConstraints();
		gbc_labelActionDescription.gridheight = 2;
		gbc_labelActionDescription.insets = new Insets( 5, 5, 5, 5 );
		gbc_labelActionDescription.gridx = 1;
		gbc_labelActionDescription.gridy = 1;
		panelCommandEditor.add( labelActionDescription, gbc_labelActionDescription );

		final JLabel lblContext = new JLabel( "Contexts:" );
		final GridBagConstraints gbc_lblContext = new GridBagConstraints();
		gbc_lblContext.anchor = GridBagConstraints.WEST;
		gbc_lblContext.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblContext.gridx = 2;
		gbc_lblContext.gridy = 1;
		panelCommandEditor.add( lblContext, gbc_lblContext );

		final TagPanelEditor panelContextEditor = new TagPanelEditor( contexts );
		final GridBagConstraints gbc_comboBoxContext = new GridBagConstraints();
		gbc_comboBoxContext.gridwidth = 2;
		gbc_comboBoxContext.insets = new Insets( 5, 5, 5, 5 );
		gbc_comboBoxContext.fill = GridBagConstraints.BOTH;
		gbc_comboBoxContext.gridx = 3;
		gbc_comboBoxContext.gridy = 1;
		panelCommandEditor.add( panelContextEditor, gbc_comboBoxContext );

		final JLabel lblConflicts = new JLabel( "Conflicts:" );
		final GridBagConstraints gbc_lblConflicts = new GridBagConstraints();
		gbc_lblConflicts.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblConflicts.anchor = GridBagConstraints.WEST;
		gbc_lblConflicts.gridx = 2;
		gbc_lblConflicts.gridy = 2;
		panelCommandEditor.add( lblConflicts, gbc_lblConflicts );

		final JLabel lblConflict = new JLabel( "TODO" );
		final GridBagConstraints gbc_lblConflict = new GridBagConstraints();
		gbc_lblConflict.gridwidth = 2;
		gbc_lblConflict.gridx = 3;
		gbc_lblConflict.gridy = 2;
		panelCommandEditor.add( lblConflict, gbc_lblConflict );

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
		final JTable tableBindings = new JTable( tableModel );
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

				final int row = tableBindings.getSelectedRow();
				if ( row < 0 )
					return;

				final String action = tableModel.actions.get( row );
				final InputTrigger trigger = tableModel.bindings.get( row );
				final List< String > contexts = new ArrayList<>( tableModel.contexts.get( row ) );
				contexts.sort( null );

				labelActionName.setText( action );
				labelActionDescription.setText( "TODO" );
				textFieldBinding.setText( trigger == null ? "" : prettyPrintTrigger( trigger ) );
				panelContextEditor.setTags( contexts );
			}
		} );

		// Renderers.
		tableBindings.getColumnModel().getColumn( 1 ).setCellRenderer( new MyBindingsRenderer() );
		tableBindings.getColumnModel().getColumn( 2 ).setCellRenderer( new MyContextsRenderer() );
		scrollPane.setViewportView( tableBindings );
	}

	/*
	 * INNER CLASSES
	 */

	private static final class MyContextsRenderer extends DefaultTableCellRenderer implements TableCellRenderer
	{

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent( final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			@SuppressWarnings( "unchecked" )
			final List< String > contexts = ( List< String > ) value;
			setText( prettyPrintContexts( contexts ) );
			setToolTipText( contexts.toString() );
			return this;
		}
	}

	private static final String prettyPrintContexts( final List< String > contexts )
	{
		if ( null == contexts || contexts.isEmpty() )
		{
			return "";
		}
		else
		{
			final StringBuilder str = new StringBuilder();
			str.append( contexts.get( 0 ) );
			for ( int i = 1; i < contexts.size(); i++ )
				str.append( ", " + contexts.get( i ) );
			return str.toString();
		}
	}

	private static final class MyBindingsRenderer extends DefaultTableCellRenderer implements TableCellRenderer
	{

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent( final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			final InputTrigger input = ( InputTrigger ) value;
			setText( null == input ? "" : prettyPrintTrigger( input ) );
			setToolTipText( null == input ? "" : input.toString() );
			return this;
		}
	}

	private static final String prettyPrintTrigger( final InputTrigger input )
	{
		final String str = input.toString();
		final String str2 = str
				.replaceAll( "shift", "\u21E7" )
				.replaceAll( "win", "\u229E" )
				.replaceAll( "ctrl", "\u2303" )
				.replaceAll( "escape", "\u238B" )
				.replaceAll( "tab", "\u21E5" )
				.replaceAll( "caps_lock", "\u21EA" )
				.replaceAll( "option", "\u2325" )
				.replaceAll( "Apple", "\uF8FF" )
				.replaceAll( "command", "\u2318" )
				.replaceAll( "space", "\u2423" )
				.replaceAll( "return", "\u23CE" )
				.replaceAll( "back_space", "\u232B" )
				.replaceAll( "delete", "\u2326" )
				.replaceAll( "home", "\u21F1" )
				.replaceAll( "end", "\u21F2" )
				.replaceAll( "page_up", "\u21DE" )
				.replaceAll( "page_down", "\u21DF" )
				.replaceAll( "up", "\u2191" )
				.replaceAll( "down", "\u2193" )
				.replaceAll( "left", "\u2190" )
				.replaceAll( "right", "\u2192" )
				.replaceAll( "clear", "\u2327" )
				.replaceAll( "num lock", "\u21ED" )
				.replaceAll( "enter", "\u2324" )
				.replaceAll( "eject", "\u23CF" )
				.replaceAll( "power", "\u233D" )
				.replaceAll( "button1", "left mouse button" )
				.replaceAll( "button2", "middle mouse button" )
				.replaceAll( "button3", "right mouse button" )
				.replaceAll( "scroll", "\u21c5" ); // double arrow. Not ideal.
		return str2;
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
					bindings.add( null );
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
		actions.add( "awake the dragons" );
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
