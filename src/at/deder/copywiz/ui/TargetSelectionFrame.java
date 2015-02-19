package at.deder.copywiz.ui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;






import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;






import se.datadosen.component.RiverLayout;





import at.deder.copywiz.runner.ScanForComponentesRunner;

public class TargetSelectionFrame extends JFrame {
	// private JSplitPane panelSelector = new JSplitPane(JSplitPane);
	private JPanel panelGlobalButtons = new JPanel(new FlowLayout());
	private JPanel panelAddRemoveButtons = new JPanel(new RiverLayout());
	private BlinkingLabel lblStatus = new BlinkingLabel("Loading...");
	private JTree treeAvailable = new JTree(new DefaultMutableTreeNode(
			"targets"));
	private JTable tableSelected = new JTable(new DefaultTableModel(
			new Object[] { "Targets" }, 0));
	private JButton buttonAdd = new JButton(">>");
	private JButton buttonRemove = new JButton("<<");
	private JButton buttonNext = new JButton("Next");
	private JButton buttonExit = new JButton("Exit");
	private JSplitPane spSelector = null;
	private JSplitPane spSelected = null;

	public TargetSelectionFrame() {
		super("Select targets");
		setLayout(new RiverLayout());

		// configure UI elements
		lblStatus.setFont(new Font("Arial", Font.BOLD, 12));
		lblStatus.setBlinking(true);
		treeAvailable.setRootVisible(false);

		// button actions
		buttonAdd
				.addActionListener(e -> {
					// add selected items to the selection
					TreePath[] selectedItems = treeAvailable
							.getSelectionPaths();

					if (selectedItems.length < 1) {
						return;
					}

					for (TreePath tp : selectedItems) {
						DefaultMutableTreeNode last = (DefaultMutableTreeNode) tp
								.getPathComponent(tp.getPathCount() - 1);
						if (last.isLeaf()) {
							addNodeToUpdate(last);
						} else {
							recursivelyAddNode(last);
						}
					}

				});

		buttonRemove.addActionListener(e -> {
			int[] indices = tableSelected.getSelectedRows();

			if (indices.length < 1) {
				return;
			}

			DefaultTableModel dtm = (DefaultTableModel) tableSelected
					.getModel();

			for (int i = 0; i < indices.length; i++) {
				dtm.removeRow(indices[i] - i);
			}
		});

		buttonExit.addActionListener(e -> {
			// close window
				dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
			});
		
		buttonNext.addActionListener(e -> {
			if(tableSelected.getRowCount() < 1) {
				JOptionPane.showMessageDialog(this, "Please select at least on target.", "No target selected",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			MainFrame mFrame = new MainFrame();
			
			for(int i=0; i<tableSelected.getRowCount(); ++i) {
				String target = (String) tableSelected.getModel().getValueAt(i, 0);
				mFrame.addUpdatePanel(target);
			}
			
			mFrame.setVisible(true);
			setVisible(false);
		});

		// place elements
		add("p center", lblStatus);
		spSelected = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				panelAddRemoveButtons, new JScrollPane(tableSelected));
		spSelector = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				new JScrollPane(treeAvailable), spSelected);
		add("p left hfill", spSelector);
		add("p left hfill", panelGlobalButtons);

		panelAddRemoveButtons.add("p center", buttonAdd);
		panelAddRemoveButtons.add("br", buttonRemove);

		panelGlobalButtons.add("p left", buttonExit);
		panelGlobalButtons.add("right", buttonNext);

		// configure frame
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		setLocationRelativeTo(null);

		// trigger next actions
		scanForTargets();
	}

	private void addNodeToUpdate(DefaultMutableTreeNode last) {
		String path = ".";
		for (Object ob: last.getPath()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) ob;
			
			if(node.equals(treeAvailable.getModel().getRoot())) {
				continue; // ignore root node
			}
			
			path += File.separator
					+ node.getUserObject().toString();
		}
		DefaultTableModel dtm = (DefaultTableModel) tableSelected
				.getModel();
		
		// check if target exists
		File targetFile = new File(path);
		if(!targetFile.exists()) {
			JOptionPane.showMessageDialog(this, "Target '"+path+"' does not exist.", "Error!", JOptionPane.ERROR_MESSAGE);;
			return;
		}
		
		// prevent duplicates
		for(int i=0; i<dtm.getRowCount();++i) {
			Object v = dtm.getValueAt(i, 0);
			if(v != null && v.toString().equals(targetFile.getAbsolutePath())) {
				return;
			}
		}
		
		// everything is fine, add item
		dtm.addRow(new Object[] { targetFile.getAbsolutePath() });
	}

	private void recursivelyAddNode(DefaultMutableTreeNode last) {
		if(last.getChildCount() <= 0) {
			return;
		}
		
		for(int i= 0; i<last.getChildCount(); ++i) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) last.getChildAt(i);
			if(child.isLeaf()) {
				addNodeToUpdate(child);
			} else {
				recursivelyAddNode(child);
			}
		}
	}

	// scan workdir for component files
	private void scanForTargets() {
		ScanForComponentesRunner runner = new ScanForComponentesRunner(this);
		Thread scanThread = new Thread(runner);
		scanThread.start();
	}

	synchronized public void addAvailableTarget(String path) {
		addAvailableNode((DefaultMutableTreeNode) treeAvailable.getModel()
				.getRoot(), path);
		TreeModel tm = new DefaultTreeModel(
				(DefaultMutableTreeNode) treeAvailable.getModel().getRoot());
		treeAvailable.setModel(tm);
		for (int i = 0; i < treeAvailable.getRowCount(); ++i) {
			treeAvailable.expandRow(i);
		}
		spSelector.setDividerLocation(0.33);
		spSelected.setDividerLocation(0.44);
	}

	private void addAvailableNode(DefaultMutableTreeNode parent,
			String remaining) {
		if (remaining.isEmpty()) {
			return;
		}

		String next = "";
		if (remaining.contains(File.separator)) {
			next = remaining.substring(0, remaining.indexOf(File.separator));
			remaining = remaining.substring(next.length() + 1);
		} else {
			next = remaining;
			remaining = "";
		}

		for (int i = 0; i < parent.getChildCount(); ++i) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent
					.getChildAt(i);
			if (next.equals(child.getUserObject().toString())) {
				addAvailableNode(child, remaining);
				return;
			}
		}

		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(next);
		parent.add(newNode);
		addAvailableNode(newNode, remaining);
		repaint();
	}

	synchronized public void scanIsDone() {
		lblStatus.setBlinking(false);
		lblStatus.setText("Please select targets.");
	}
}
