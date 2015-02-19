package at.deder.copywiz.runner;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import at.deder.copywiz.ReportLog;

public class CopyProcess extends AbstractReportingRunner {
	
	private String source;
	private String target;
	
	private ReportLog log = null;
	
	public CopyProcess(IProcessCallback callback, String source, String target) {
		super(callback);
		this.source = source;
		this.target = target;
	}
	
	public void setReportLog(ReportLog log) {
		this.log = log;
	}
	
	@Override
	public void execute() {
		if(source.isEmpty() || target.isEmpty()) {
			reportError("invalid source or target file");
			return;
		}
		
		File sourceFile = new File(source);
		File destinationFile = new File(target);
		if(!sourceFile.exists()) {
			reportError("source file does not exist");
			return;
		}
		
		try {
			FileUtils.copyFile(sourceFile, destinationFile);
		} catch (IOException e) {
			reportError(e.getMessage());
			return;
		}
		
	}

}
