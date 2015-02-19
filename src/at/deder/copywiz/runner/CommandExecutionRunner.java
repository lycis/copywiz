package at.deder.copywiz.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import at.deder.copywiz.ReportLog;

public class CommandExecutionRunner extends AbstractReportingRunner {
	private String command = "";
	private File workdir = null;
	private ReportLog log = null;
	
	
	public CommandExecutionRunner(IProcessCallback callback, String command, File workdir, ReportLog log) {
		super(callback);
		this.command = command;
		this.workdir = workdir;
		this.log = log;
	}

	@Override
	protected void execute() {
		
		// checks
		if(!workdir.exists()) {
			reportError("Target directory '"+workdir.getAbsolutePath()+"' does not exist!");
			return;
		}
		
		// create process
		List<String> cmdDef = Arrays.asList(command.split(" "));
		ProcessBuilder pb = new ProcessBuilder(cmdDef);
		pb.directory(workdir);
		
		
		// run
		Process commandProcess = null;
		try {
			 commandProcess = pb.start();			
		} catch (IOException e) {
			reportError(e.getMessage());
			return;
		}
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(commandProcess.getInputStream()))) {
			String line;
			while((line=br.readLine()) != null) {
				log.addLine(line);
			}
		} catch(IOException e) {
			reportError(e.getMessage());
			return;
		}
		
		if(commandProcess.isAlive()) {
			reportError("output ended while process still alive!");
			return;
		}
	}

}
