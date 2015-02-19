package at.deder.copywiz;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;

public class ReportLogDialog extends JDialog {
	private JTextArea txtLog = new JTextArea();
	private ReportLog reportLog = null;
	private Timer refreshTimer = null;
	
	public ReportLogDialog(JFrame parent, ReportLog log) {
		super(parent, "Report", true);
		this.reportLog = log;
		setLayout(new BorderLayout());
		add(new JScrollPane(txtLog), BorderLayout.CENTER);
		txtLog.setText(log.toString());
		txtLog.setFont(new Font("Courier New", Font.PLAIN, 12));
		txtLog.setEditable(false);
		buildMenuStructure();
		setSize(500, 300);
		
		DefaultCaret caret = (DefaultCaret) txtLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	}
	
	@Override
	public void setVisible(boolean visible) {
		if(visible) {
			refreshTimer = new Timer();
			refreshTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					refresh();			
				}
			}, 5000, 5000);
		}else {
			if(refreshTimer != null) {
				refreshTimer.cancel();
			}
		}
		super.setVisible(visible);
	}
	
	private void buildMenuStructure() {
		JMenuBar  menuBar         = new JMenuBar();
		JMenu     menuFile        = new JMenu("File");
		JMenuItem menuItemSave    = new JMenuItem("Save as...");
		JMenuItem menuItemRefresh = new JMenuItem("Refresh");
		
		menuItemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		menuItemRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
		
		menuItemSave.addActionListener(e -> {
			JFileChooser fc = new JFileChooser();
			fc.setSelectedFile(new File(reportLog.getFile()));
			int rc = fc.showSaveDialog(this);
			if(rc != JFileChooser.APPROVE_OPTION) {
				return;
			}
			try {
				reportLog.saveTo(fc.getSelectedFile());
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(this, e1.getStackTrace(), e1.getMessage(), ERROR);
			}
		});
		
		menuItemRefresh.addActionListener(e -> {
			refresh();		
		});
		
		menuFile.add(menuItemSave);
		menuFile.add(menuItemRefresh);
		menuBar.add(menuFile);
		setJMenuBar(menuBar);
	}

	private void refresh() {
		txtLog.setText(reportLog.toString());
		try {
			txtLog.setCaretPosition(txtLog.getLineStartOffset(txtLog.getLineCount() - 1));
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
}
