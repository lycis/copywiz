package at.deder.copywiz.runner;

abstract public class AbstractReportingRunner implements Runnable {
	private IProcessCallback callback;
	
	public AbstractReportingRunner(IProcessCallback callback) {
		this.callback = callback;
	}
	
	protected void reportToCallback(Object o) {
		callback.report(o);
	}
	
	protected void reportEnd() {
		callback.finished();
	}
	
	protected void reportError(String message) {
		callback.error(message);
	}
	
	abstract protected void execute();

	@Override
	public void run() {
		execute();
		reportEnd();
	}

}
