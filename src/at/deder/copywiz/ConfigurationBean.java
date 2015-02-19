package at.deder.copywiz;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationBean {
	private String installBasePath = "";
	private List<String> includedFiles = new ArrayList<>();
	private List<String> excludedFiles = new ArrayList<>();
	private String target = "";
	private String name = "";
	private List<String> runCommands = new ArrayList<>();
	public String getInstallBasePath() {
		return installBasePath;
	}
	public void setInstallBasePath(String installBasePath) {
		this.installBasePath = installBasePath;
	}
	public List<String> getIncludedFiles() {
		return includedFiles;
	}
	public void setIncludedFiles(List<String> includedFiles) {
		if(includedFiles == null) {
			includedFiles = new ArrayList<>();
			return;
		}
		this.includedFiles = includedFiles;
	}
	public List<String> getExcludedFiles() {
		return excludedFiles;
	}
	public void setExcludedFiles(List<String> excludedFiles) {
		if(excludedFiles == null) {
			excludedFiles = new ArrayList<>();
			return;
		}
		this.excludedFiles = excludedFiles;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<String> getRunCommands() {
		return runCommands;
	}
	public void setRunCommands(List<String> runCommands) {
		if(runCommands == null) {
			runCommands = new ArrayList<>();
			return;
		}
		this.runCommands = runCommands;
	}
	
}
