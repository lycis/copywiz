package at.deder.copywiz.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import se.datadosen.component.RiverLayout;
import at.deder.copywiz.IUpdateListener;
import at.deder.copywiz.ui.UpdaterPanel.Status;

public class MainFrame extends JFrame implements IUpdateListener {
	private Map<UpdaterPanel, UpdaterPanel.Status> uPanels = new HashMap<>();
	private JPanel panelPanel = new JPanel(new GridLayout(0, 1));
	private JPanel controlPanel = new JPanel(new RiverLayout());
	private JButton startButton = new JButton("Start");
	private JButton cancelButton = new JButton("Exit");
	private JProgressBar totalProgress = new JProgressBar(0,0);
	private JButton detailsButton = new JButton(">");
	private JButton redoButton = new JButton("New Update");
	
	public MainFrame() {
		super("CopyWiz 0.6");
		
		setLayout(new BorderLayout());
		add(controlPanel, BorderLayout.NORTH);
		add(panelPanel, BorderLayout.CENTER);
		controlPanel.add("p center", startButton);
		controlPanel.add(redoButton);
		controlPanel.add(cancelButton);
		controlPanel.add("p center hfill", totalProgress);
		controlPanel.add(detailsButton);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		cancelButton.addActionListener(e -> {
			uPanels.keySet().stream().forEach(p -> {
				p.cancelAccordingToStatus();
			});
			// close window
			dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		});
		
		startButton.addActionListener(e -> {
			startUpdate();
		});
		
		detailsButton.addActionListener(e -> {
			if(panelPanel.isVisible()) {
				panelPanel.setVisible(false);
				detailsButton.setText(">");
			} else {
				panelPanel.setVisible(true);
				detailsButton.setText("v");
			}
			pack();
			this.setSize(500, getHeight());
		});
		
		redoButton.addActionListener(e -> {
			TargetSelectionFrame selectionFrame = new TargetSelectionFrame();
			selectionFrame.setVisible(true);
			this.setVisible(false);
		});
		
		panelPanel.setVisible(false);
		redoButton.setEnabled(false);
	}
	
	public void addUpdatePanel(String config) {
		UpdaterPanel panel = new UpdaterPanel(config);
		panel.checkAndInit();
		uPanels.put(panel, UpdaterPanel.Status.INIT);
		panelPanel.add(panel);
		pack();
		this.setSize(500, getHeight());
		setLocationRelativeTo(null);
		panel.addUpdateListener(this);
	}
	
	public void startUpdate() {
		totalProgress.setMaximum(uPanels.size()*4); // there are four states
		totalProgress.setMinimum(0);
		totalProgress.setValue(0);
		uPanels.keySet().stream().forEach(p -> {
			p.startUpdate();
		});
	}

	@Override
	public void updateStatusChanged(UpdaterPanel who, Status newStatus) {
		uPanels.put(who, newStatus);
		
		// calculate current completion
		int completion = 0;
		for(UpdaterPanel.Status status: uPanels.values()) {
			switch(status) {
			case DISCOVER: completion += 1; break;
			case COPY: completion += 2; break;
			case COMMANDS: completion += 3; break;
			case CANCELED:
			case ERROR:
			case FINISHED: completion += 4; break;
			default: completion += 0; break;
			}
		}
		
		totalProgress.setValue(completion);
		if(totalProgress.getMaximum() != 0 && totalProgress.getValue() == totalProgress.getMaximum()) {
			redoButton.setEnabled(true);
			startButton.setEnabled(false);
		}
	}
}
