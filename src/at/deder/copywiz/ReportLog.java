package at.deder.copywiz;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.UUID;

import javax.swing.JOptionPane;

public class ReportLog {
	private StringBuilder report = new StringBuilder();
	private String file = UUID.randomUUID().toString()+"_updatelog.txt";
	private boolean newline = true;
	
	public void addLine(String line) {
		addString(line+"\n");
	}
	
	synchronized public void addString(String str) {
		if(newline) {
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			str = ts.toString()+" - "+str;
			newline = false;
		}
		
		if(str.endsWith("\n")){
			newline = true;
		}
		
		report.append(str);
	}

	public String toString() {
		return report.toString();
	}
	
	public String getFile() {
		return file;
	}
	
	public void saveTo(File f) throws IOException {
		if(f.exists()) {
			int choice = JOptionPane.showConfirmDialog(null, "File "+f.getAbsolutePath()+" already exists. Overwrite?", "File exists", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(choice != JOptionPane.YES_OPTION) {
				return;
			}
		}
		
		PrintWriter writer = new PrintWriter(f);
		writer.print(toString());
		writer.close();
	}
}
