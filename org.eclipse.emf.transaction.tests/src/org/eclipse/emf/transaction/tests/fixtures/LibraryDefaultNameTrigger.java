/**
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM - Initial API and implementation
 */
package org.eclipse.emf.transaction.tests.fixtures;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.examples.extlibrary.EXTLibraryPackage;
import org.eclipse.emf.examples.extlibrary.Library;
import org.eclipse.emf.transaction.NotificationFilter;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.TriggerListener;

/**
 * A trigger listener that sets a default name on new libraries.
 *
 * @author Christian W. Damus (cdamus)
 */
public class LibraryDefaultNameTrigger extends TriggerListener {
	public LibraryDefaultNameTrigger() {
		super(NotificationFilter.createFeatureFilter(
					EXTLibraryPackage.eINSTANCE.getLibrary_Branches()).and(
							NotificationFilter.createEventTypeFilter(
									Notification.ADD)));
	}
	
	@Override
	protected Command trigger(TransactionalEditingDomain domain, Notification notification) {
		Command result = null;
		
		Library newLibrary = (Library) notification.getNewValue();
		if ((newLibrary.getName() == null) || (newLibrary.getName().length() == 0)) {
			result= new SetCommand(
					domain,
					newLibrary,
					EXTLibraryPackage.eINSTANCE.getLibrary_Name(),
					"New Library"); //$NON-NLS-1$
		}
		
		return result;
	}
}
