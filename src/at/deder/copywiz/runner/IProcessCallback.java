package at.deder.copywiz.runner;

public interface IProcessCallback {
	public void error(String message);
	public void finished();
	public void report(Object o);
}
