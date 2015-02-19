package at.deder.copywiz.runner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import at.deder.copywiz.ConfigurationBean;
import at.deder.copywiz.ReportLog;

public class DiscoverProcess extends AbstractReportingRunner {
	private ConfigurationBean config = null;
	private ReportLog log = null;

	public DiscoverProcess(IProcessCallback callback, ConfigurationBean config) {
		super(callback);
		this.config = config;
	}

	public void setReportLog(ReportLog log) {
		this.log = log;
	}

	@Override
	public void execute() {
		if (config == null) {
			reportError("config not given for discover!");
			return;
		}

		File baseDir = new File(config.getInstallBasePath());
		if (!baseDir.exists() || !baseDir.isDirectory()) {
			reportError("source directory does not exist");
			return;
		}

		Collection<File> sourceFiles = FileUtils.listFiles(baseDir,
				new IOFileFilter() {
					@Override
					public boolean accept(File f) {
						if (log != null) {
							log.addString("Checking file: "
									+ f.getAbsolutePath() + " -> ");
						}
						String path = removeBasePath(f.getAbsolutePath());

						// check for exclusion
						if (config.getExcludedFiles() != null) { // only check
																	// when
																	// exclusions
																	// are given
							for (String mask : config.getExcludedFiles()) {
								if (path.matches(mask)) {
									if (log != null) {
										log.addLine("excluded");
									}
									return false;
								}
							}
						}

						// check for inclusion
						for (String mask : config.getIncludedFiles()) {
							if (path.matches(mask)) {
								if (log != null) {
									log.addLine("included");
								}
								reportToCallback(null);
								return true;
							}
						}

						return false;
					}

					@Override
					public boolean accept(File dir, String name) {
						File f = new File(dir.getAbsolutePath()
								+ File.separator + name);
						if (!f.exists()) {
							return false;
						}

						return accept(f);
					}
				}, TrueFileFilter.TRUE);

		List<String> filePaths = new ArrayList<>();
		for (File f : sourceFiles) {
			filePaths.add(removeBasePath(f.getAbsolutePath()));
		}
		reportToCallback(filePaths);
	}

	private String removeBasePath(String fname) {
		String bp = config.getInstallBasePath().replaceAll("\\\\", "\\\\\\\\");
		return fname.replaceFirst(bp, "");
	}

}
