package at.deder.copywiz;

import at.deder.copywiz.ui.UpdaterPanel;

/**
 * Listenes for changes of the update status
 * @author lycis
 *
 */
public interface IUpdateListener {
	public void updateStatusChanged(UpdaterPanel who, UpdaterPanel.Status newStatus);
}
