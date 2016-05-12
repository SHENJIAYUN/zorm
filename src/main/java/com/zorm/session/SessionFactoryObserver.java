package com.zorm.session;

import java.io.Serializable;

public interface SessionFactoryObserver extends Serializable{
	/**
	 * Callback to indicate that the given factory has been created and is now ready for use.
	 *
	 * @param factory The factory initialized.
	 */
	public void sessionFactoryCreated(SessionFactory factory);

	/**
	 * Callback to indicate that the given factory has been closed.  Care should be taken
	 * in how (if at all) the passed factory reference is used since it is closed.
	 *
	 * @param factory The factory closed.
	 */
	public void sessionFactoryClosed(SessionFactory factory);
}
