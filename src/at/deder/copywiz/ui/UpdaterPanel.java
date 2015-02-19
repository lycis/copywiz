package at.deder.copywiz.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import at.deder.copywiz.ConfigurationBean;
import at.deder.copywiz.IUpdateListener;
import at.deder.copywiz.ReportLog;
import at.deder.copywiz.ReportLogDialog;
import at.deder.copywiz.runner.CommandExecutionRunner;
import at.deder.copywiz.runner.CopyProcess;
import at.deder.copywiz.runner.DiscoverProcess;
import at.deder.copywiz.runner.IProcessCallback;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

public class UpdaterPanel extends JPanel implements IProcessCallback {
	
	public enum Status {
		INIT,
		READY,
		DISCOVER,
		COPY,
		COMMANDS,
		FINISHED,
		ERROR,
		CANCELED
	}
	
	private JProgressBar progressBar = new JProgressBar(0, 0);
	private Status currentStatus;
	private JButton buttonCancel = new JButton("Cancel");
	private List<Thread> operations = new ArrayList<>();
	private List<String> discoveredFiles = new ArrayList<>();
	private ConfigurationBean config = null;
	private String configFile = "";
	private boolean canceled = false;
	private int numDiscoveredFiles = 0;
	private int numCopiedFiles = 0;
	private JButton buttonShowReport = new JButton("Report");
	private JPanel panelButtons = new JPanel(new FlowLayout());
	private final ReportLog reportLog = new ReportLog();
	private List<IUpdateListener> interestedListeners = new ArrayList<>();
	
	public UpdaterPanel(String configFile) {
		super();
		this.configFile = configFile;
		
		// configure UI items
		progressBar.setStringPainted(true);
		setStatus(Status.INIT);
		
		buttonCancel.addActionListener(e -> {
			reportLog.addLine("Received cancel requested by user.");
			cancelAccordingToStatus();
			buttonCancel.setEnabled(false);
		});
		
		buttonShowReport.addActionListener(e -> {
			ReportLogDialog logDlg = new ReportLogDialog(null, reportLog);
			logDlg.setVisible(true);;
		});
		
		Border lineBorder = BorderFactory.createTitledBorder("~ unknown ~");
		setBorder(lineBorder);
		
		// build layout
		setLayout(new BorderLayout());
		add(progressBar, BorderLayout.CENTER);
		add(panelButtons, BorderLayout.EAST);
		panelButtons.add(buttonCancel);
		panelButtons.add(buttonShowReport);
	}
	
	public ReportLog getReportLog() {
		return reportLog;
	}
	
	private void setStatus(Status newStatus) {
		setStatus(newStatus, null);
	}
	
	private void setStatus(Status newStatus, String additionalText) {
		currentStatus = newStatus;
		
		String caption = "";
		
		switch(currentStatus) {
		case INIT: 
			caption = "Initialising...";
			reportLog.addLine("Starting initialisation");
			break;
		case READY:
			caption = "Ready";
			reportLog.addLine("Ready for start!");
			break;
		case DISCOVER:
			caption = "Discovering files";
			break;
		case COPY:
			caption = "Copying";
			break;
		case COMMANDS:
			caption = "Executing commands";
			runCommands(); // start running commands
			break;
		case FINISHED:
			caption = "Done.";
			progressBar.setMaximum(1);
			progressBar.setValue(1);
			buttonCancel.setEnabled(false);
			reportLog.addLine("Done.");
			break;
		case ERROR:
			caption = "Error: ";
			buttonCancel.setEnabled(false);
			break;
		case CANCELED:
			caption = "Canceled";
			buttonCancel.setEnabled(false);
			reportLog.addLine("Canceled.");
			break;
		default:
			caption = "Error: Unknown status";
			buttonCancel.setEnabled(false);
			reportLog.addLine("ERROR: unknown status");
			break;
		}
		
		// the status could have changed within the processing
		if(currentStatus != newStatus) {
			return;
		}
		
		if(additionalText != null && !additionalText.isEmpty()) {
			caption += " " + additionalText;
		}
		
		progressBar.setString(caption);
		
		// notify listeners
		SwingUtilities.invokeLater(() -> {
			interestedListeners.stream().forEach((l) -> {
				l.updateStatusChanged(this, newStatus);
			});			
		});
	}
	
	private void enterErrorState(String title, String message) {
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
		reportLog.addLine("ERROR: "+title);
		reportLog.addLine("Details: ");
		reportLog.addLine(message);
		cancelAllOperations();
		setStatus(Status.ERROR, title);
	}
	
	public void checkAndInit() {
		// Check and read config file
		setStatus(Status.INIT);
		reportLog.addString("Reading configuration...");
		try {
			YamlReader configReader = new YamlReader(new FileReader(configFile));
			config = configReader.read(ConfigurationBean.class);
		} catch (FileNotFoundException | YamlException e) {
			enterErrorState(e.getMessage(), getStackTrace(e));
			return;
		}
		reportLog.addLine("ok");
		
		// get name of target
		if(config.getName().isEmpty()) {
			config.setName("~ unknown ~");
			reportLog.addLine("Target: [no name]");
		}
		Border lineBorder = BorderFactory.createTitledBorder(config.getName());
		setBorder(lineBorder);
		
		// init done
		setStatus(Status.READY);
	}
	
	public void startUpdate() {
		reportLog.addLine("Starting update procedure.");
		if(canceled) {
			setStatus(Status.CANCELED);
			return;
		}
		
		if(currentStatus == Status.FINISHED) {
			return; // just ignore
		}
		
		if(currentStatus != Status.READY) {
			enterErrorState("Not ready!", "Update invoked while updater was not ready!");
			return;
		}
		
		if(config.getIncludedFiles().isEmpty()) {
			reportLog.addLine("No files included. Nothing to copy.");
			setStatus(Status.COMMANDS);
			return;
		}
		
		// discover files to copy
		setStatus(Status.DISCOVER);
		reportLog.addLine("Discovering files from '"+config.getInstallBasePath()+"'");
		DiscoverProcess dp = new DiscoverProcess(this, config);
		dp.setReportLog(reportLog);
		Thread discoverThread = new Thread(dp);
		operations.add(discoverThread);
		discoverThread.start();
	}
	
	public void cancelAccordingToStatus() {
		if(currentStatus != Status.ERROR) {
			cancelAllOperations();
		}
		canceled = true;
	}
	
	private void cancelAllOperations() {
		reportLog.addLine("Canceling all active operations:");
		operations.stream().forEach(op -> {
			if(op.isAlive()) {
				reportLog.addLine("* "+op.getName());
				op.interrupt();
			}
		});
		operations.clear();
		reportLog.addLine("Operations cancled.");
	}
	
	public static String getStackTrace(final Throwable throwable) {
	     final StringWriter sw = new StringWriter();
	     final PrintWriter pw = new PrintWriter(sw, true);
	     throwable.printStackTrace(pw);
	     return sw.getBuffer().toString();
	}

	@Override
	public void error(String message) {
		if(canceled) {
			reportLog.addLine("Received error report while already canceled: "+message);
			setStatus(Status.CANCELED);
			return;
		}
		
		switch(currentStatus) {
		case DISCOVER:
			enterErrorState("Discovering failed", message);
			break;
		case COMMANDS:
			enterErrorState("Command failed: ", message);
			break;
		case COPY:
			int choice = JOptionPane.showConfirmDialog(this, "Copy failed: "+message+"\nContinue?", "Copy failed", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(choice == JOptionPane.YES_OPTION) {
				reportLog.addLine("Continuing on user request.");
				copyNextFile();
			} else {
				setStatus(Status.ERROR, message);
			}
			break;
		default:
			enterErrorState("Processing error", "an error occurred in an unknown state: "+message);
			break;
		}
	}

	@Override
	public void finished() {
		if(canceled) {
			setStatus(Status.CANCELED);
			return;
		}
		
		switch(currentStatus) {
		case DISCOVER:
			reportLog.addLine("Discovered "+discoveredFiles.size()+" eligible files");
			startCopy();
			break;
		case COPY:
			progressBar.setValue(progressBar.getValue() + 1);
			reportLog.addLine("Copy done. ("+numCopiedFiles+" / "+numDiscoveredFiles);
			copyNextFile();
			break;
		case COMMANDS:
			progressBar.setValue(progressBar.getValue() + 1);
			runNextCommand();
			break;
		default:
			setStatus(Status.FINISHED);
			break;
		}
	}

	@Override
	synchronized public void report(Object o) {
		if(canceled) {
			setStatus(Status.CANCELED);
			return;
		}
		
		switch(currentStatus) {
		case DISCOVER:
			if(o == null) {
				// a reporting with NULL indicates a new finding TODO improve
				setStatus(Status.DISCOVER, (++numDiscoveredFiles+" found"));
				return;
			}
			
			if(!(o instanceof List<?>)) {
				// we only accept lists (of files)
				enterErrorState("Broken reporting", "received unexpected report object of type "+o.getClass()+" during discovery");
				return;
			}
			
			// the reported list contains the discovered file paths
			discoveredFiles = (List<String>) o;
			break;
		default:
			break;
		}
	}
	
	private void startCopy() {
		if(canceled) {
			setStatus(Status.CANCELED);
			return;
		}
		
		progressBar.setMaximum(discoveredFiles.size());
		progressBar.setMinimum(0);
		progressBar.setValue(0);
		reportLog.addLine("Copy process started.");
		copyNextFile();
	}
	
	private void copyNextFile() {
		if(canceled) {
			setStatus(Status.CANCELED);
			return;
		}
		
		if(discoveredFiles.isEmpty()) {
			reportLog.addLine("All files copied.");
			setStatus(Status.COMMANDS);
			return;
		}
		
		String filePath = discoveredFiles.remove(0);
		String src = config.getInstallBasePath()+File.separator+filePath;
		String dst = config.getTarget()+File.separator+filePath;	
		
		Thread copyThread = new Thread(new CopyProcess(this, src, dst));
		operations.add(copyThread);
		String displayPath = filePath;
		if(displayPath.length() > 15) {
			displayPath = "..."+displayPath.substring(displayPath.length()-13);
		}
		setStatus(Status.COPY, " "+displayPath+" ("+(++numCopiedFiles)+" of "+numDiscoveredFiles+")");
		reportLog.addLine("copy '"+src+"' to '"+dst+"'");
		copyThread.start();
	}
	
	// execute commands when applicable
	private void runCommands() {
		if(canceled) {
			setStatus(Status.CANCELED);
			return;
		}
		
		if(config.getRunCommands().isEmpty()) {
			reportLog.addLine("No commands to execute.");
			setStatus(Status.FINISHED);
			return;
		}
		
		progressBar.setValue(0);
		progressBar.setMaximum(config.getRunCommands().size());
		
		runNextCommand();
	}

	private void runNextCommand() {
		if(canceled) {
			setStatus(Status.CANCELED);
			return;
		}
		
		if(config.getRunCommands().isEmpty()) {
			// we're done
			reportLog.addLine("All commands executed.");
			setStatus(Status.FINISHED);
			return;
		}
		
		String command = config.getRunCommands().remove(0);
		reportLog.addLine("Running command: "+command);
		CommandExecutionRunner cer = new CommandExecutionRunner(this, command, new File(config.getTarget()), reportLog);
		Thread t = new Thread(cer);
		operations.add(t);
		t.start();
	}
	
	public void addUpdateListener(IUpdateListener l) {
		interestedListeners.add(l);
	}
	
	public void removeUpdateListener(IUpdateListener l) {
		interestedListeners.remove(l);
	}
}
