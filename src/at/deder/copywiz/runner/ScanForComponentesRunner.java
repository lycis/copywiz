package at.deder.copywiz.runner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import at.deder.copywiz.ConfigurationBean;
import at.deder.copywiz.ui.TargetSelectionFrame;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

public class ScanForComponentesRunner implements Runnable {
	private TargetSelectionFrame selectionFrame = null;
	
	public ScanForComponentesRunner(TargetSelectionFrame selectionFrame) {
		this.selectionFrame = selectionFrame;
	}

	@Override
	public void run() {
		// find viable targets
		FileUtils.listFiles(new File("."),
				new IOFileFilter() {
					@Override
					public boolean accept(File f) {
						if(!f.getAbsolutePath().endsWith(".yml")) {
							return false;
						}
						
						ConfigurationBean config = null;
						try {
							YamlReader configReader = new YamlReader(
									new FileReader(f));
							config = configReader.read(ConfigurationBean.class);
						} catch (FileNotFoundException | YamlException e) {
							// ignore file
							return false;
						}

						if (config == null) {
							return false; // some weird error... should never
											// happen
						}

						// is a valid config file
						selectionFrame.addAvailableTarget(f.getAbsolutePath().substring((new File(".")).getAbsolutePath().length()+1));
						return true; 
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
		
		selectionFrame.scanIsDone();
	}

}
