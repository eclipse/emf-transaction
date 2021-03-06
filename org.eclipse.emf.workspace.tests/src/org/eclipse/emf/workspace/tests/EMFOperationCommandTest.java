/**
 * Copyright (c) 2005, 2008 IBM Corporation, Zeligsoft Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM - Initial API and implementation
 *   Zeligsoft - Bug 234868
 */
package org.eclipse.emf.workspace.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.examples.extlibrary.Book;
import org.eclipse.emf.examples.extlibrary.EXTLibraryPackage;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.RollbackException;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.TriggerListener;
import org.eclipse.emf.workspace.EMFOperationCommand;
import org.eclipse.emf.workspace.tests.fixtures.ContextAdder;
import org.eclipse.emf.workspace.tests.fixtures.ExternalDataOperation;
import org.eclipse.emf.workspace.tests.fixtures.TestOperation;
import org.eclipse.emf.workspace.tests.fixtures.TestUndoContext;


/**
 * Tests the {@link EMFOperationCommand} class.
 *
 * @author Christian W. Damus (cdamus)
 */
public class EMFOperationCommandTest extends AbstractTest {

	public EMFOperationCommandTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new TestSuite(EMFOperationCommandTest.class, "EMF Operation Command Tests"); //$NON-NLS-1$
	}
	
	/**
	 * Tests execution, undo, and redo of operations wrapped within an
	 * EMF command.
	 */
	public void test_execute_undo_redo() {
		startReading();
		
		final Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
		assertNotNull(book);
		final String oldTitle = book.getTitle();
		
		final String[] externalData = new String[] {"external"}; //$NON-NLS-1$
		final String oldExternalData = externalData[0];
		final String newExternalData = "newValue"; //$NON-NLS-1$
		
		final String newTitle = "New Title"; //$NON-NLS-1$
		
		commit();
		
		IUndoContext ctx = new TestUndoContext();
		
		Command cmd = new SetCommand(
				domain,
				book,
				EXTLibraryPackage.eINSTANCE.getBook_Title(),
				newTitle);
		IUndoableOperation oper = new ExternalDataOperation(
				externalData,
				newExternalData);
		
		cmd = cmd.chain(new EMFOperationCommand(domain, oper));
		
		try {
			history.addOperationHistoryListener(new ContextAdder(ctx));
			getCommandStack().execute(cmd, null);
		} catch (Exception e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were applied
		assertSame(newTitle, book.getTitle());
		assertEquals(newExternalData, externalData[0]);
		
		commit();

		try {
			assertTrue(history.canUndo(ctx));
			history.undo(ctx, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were undone
		assertSame(oldTitle, book.getTitle());
		assertEquals(oldExternalData, externalData[0]);
		
		commit();
		
		try {
			assertTrue(history.canRedo(ctx));
			history.redo(ctx, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were redone
		assertSame(newTitle, book.getTitle());
		assertEquals(newExternalData, externalData[0]);
		
		commit();
	}
	
	/**
	 * Tests execution, undo, and redo of operations wrapped in commands as
	 * pre-commit triggers.
	 */
	public void test_execute_undo_redo_trigger() {
		startReading();
		
		final Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
		assertNotNull(book);
		final String oldTitle = book.getTitle();
		
		final String[] externalData = new String[] {"external"}; //$NON-NLS-1$
		final String oldExternalData = externalData[0];
		final String newExternalData = "newValue"; //$NON-NLS-1$
		
		final String newTitle = "New Title"; //$NON-NLS-1$
		
		commit();
		
		IUndoContext ctx = new TestUndoContext();
		
		Command cmd = new SetCommand(
				domain,
				book,
				EXTLibraryPackage.eINSTANCE.getBook_Title(),
				newTitle);
		
		domain.addResourceSetListener(new TriggerListener() {
		
			@Override
			protected Command trigger(TransactionalEditingDomain domain, Notification notification) {
				Command result = null;
				
				if ((notification.getNotifier() == book)
						&& newTitle.equals(notification.getNewValue())) {
					
					trace("Adding external data trigger command"); //$NON-NLS-1$
					
					IUndoableOperation oper = new ExternalDataOperation(
							externalData,
							newExternalData);
					
					result = new EMFOperationCommand(domain, oper);
				}
				
				return result;
			}});
		
		try {
			history.addOperationHistoryListener(new ContextAdder(ctx));
			getCommandStack().execute(cmd, null);
		} catch (Exception e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were applied
		assertSame(newTitle, book.getTitle());
		assertEquals(newExternalData, externalData[0]);
		
		commit();

		try {
			assertTrue(history.canUndo(ctx));
			history.undo(ctx, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were undone
		assertSame(oldTitle, book.getTitle());
		assertEquals(oldExternalData, externalData[0]);
		
		commit();
		
		try {
			assertTrue(history.canRedo(ctx));
			history.redo(ctx, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were redone
		assertSame(newTitle, book.getTitle());
		assertEquals(newExternalData, externalData[0]);
		
		commit();
	}
	
	/**
	 * Tests rollback of operations wrapped in commands as pre-commit triggers
	 * when the transactions that include the triggers roll back.
	 */
	public void test_rollback_trigger() {
		startReading();
		
		final Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
		assertNotNull(book);
		final String oldTitle = book.getTitle();
		
		final String[] externalData = new String[] {"external"}; //$NON-NLS-1$
		final String oldExternalData = externalData[0];
		final String newExternalData = "newValue"; //$NON-NLS-1$
		
		commit();
		
		IUndoContext ctx = new TestUndoContext();
		
		Command cmd = new SetCommand(
				domain,
				book,
				EXTLibraryPackage.eINSTANCE.getBook_Title(),
				null); // books must have titles
		
		domain.addResourceSetListener(new TriggerListener() {
		
			@Override
			protected Command trigger(TransactionalEditingDomain domain, Notification notification) {
				Command result = null;
				
				if ((notification.getNotifier() == book)
						&& (notification.getNewValue() == null)) {
					
					trace("Adding external data trigger command"); //$NON-NLS-1$
					
					IUndoableOperation oper = new ExternalDataOperation(
							externalData,
							newExternalData);
					
					result = new EMFOperationCommand(domain, oper);
				}
				
				return result;
			}});
		
		try {
			history.addOperationHistoryListener(new ContextAdder(ctx));
			getCommandStack().execute(cmd, null);
			
			fail("Should have thrown RollbackException"); //$NON-NLS-1$
		} catch (RollbackException e) {
			// success
			trace("Got expected exception: " + e.getLocalizedMessage()); //$NON-NLS-1$
		} catch (Exception e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were not applied
		assertSame(oldTitle, book.getTitle());
		assertEquals(oldExternalData, externalData[0]);
		
		commit();
	}
	
	/**
	 * Tests execution, undo, and redo of operations wrapped in commands as
	 * pre-commit triggers in a {@link RecordingCommand} context (where
	 * undo/redo of triggers is different from other commands).
	 */
	public void test_execute_undo_redo_trigger_recordingCommand() {
		startReading();
		
		final Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
		assertNotNull(book);
		final String oldTitle = book.getTitle();
		
		final String[] externalData = new String[] {"external"}; //$NON-NLS-1$
		final String oldExternalData = externalData[0];
		final String newExternalData = "newValue"; //$NON-NLS-1$
		
		final String newTitle = "New Title"; //$NON-NLS-1$
		
		commit();
		
		IUndoContext ctx = new TestUndoContext();
		
		Command cmd = new RecordingCommand(domain, "Testing") { //$NON-NLS-1$
		
			@Override
			protected void doExecute() {
				book.setTitle(newTitle);
			}};
		
		domain.addResourceSetListener(new TriggerListener() {
		
			@Override
			protected Command trigger(TransactionalEditingDomain domain, Notification notification) {
				Command result = null;
				
				if ((notification.getNotifier() == book)
						&& newTitle.equals(notification.getNewValue())) {
					
					trace("Adding external data trigger command"); //$NON-NLS-1$
					
					IUndoableOperation oper = new ExternalDataOperation(
							externalData,
							newExternalData);
					
					result = new EMFOperationCommand(domain, oper);
				}
				
				return result;
			}});
		
		try {
			history.addOperationHistoryListener(new ContextAdder(ctx));
			getCommandStack().execute(cmd, null);
		} catch (Exception e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were applied
		assertSame(newTitle, book.getTitle());
		assertEquals(newExternalData, externalData[0]);
		
		commit();

		try {
			assertTrue(history.canUndo(ctx));
			history.undo(ctx, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were undone
		assertSame(oldTitle, book.getTitle());
		assertEquals(oldExternalData, externalData[0]);
		
		commit();
		
		try {
			assertTrue(history.canRedo(ctx));
			history.redo(ctx, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were redone
		assertSame(newTitle, book.getTitle());
		assertEquals(newExternalData, externalData[0]);
		
		commit();
	}
	
	/**
	 * Tests that the EMFOperationCommand tests its wrapped operation for
	 * redoability.
	 */
	public void test_nonredoableOperation_138287() {
		IUndoableOperation operation = new TestOperation(domain) {
			@Override
			protected void doExecute() {
				// nothing to do
			}
			
			@Override
			public boolean canRedo() {
				return false;
			}};
		
		getCommandStack().execute(new EMFOperationCommand(domain, operation));
		
		assertTrue(getCommandStack().canUndo());
		
		getCommandStack().undo();
		
		assertFalse(getCommandStack().canRedo());
	}

	/**
	 * Tests that the EMFOperationCommand tests its wrapped operation for
	 * multiple disposability.
	 */
	public void test_multipleDisposableOperation_209491() {

		IUndoableOperation operation = new TestOperation(domain) {
			@Override
			protected void doExecute() {
				// nothing to do
			}

			@Override
			public boolean canRedo() {
				return false;
			}};

		EMFOperationCommand operationCommand = new EMFOperationCommand(domain, operation);
		getCommandStack().execute(operationCommand);

		operationCommand.dispose();

		Exception exception;
		try {
			// Confirm that the operation has been nulled by testing that this throws a null pointer exception.
			operationCommand.canExecute();
			exception = null;
		}
		catch (NullPointerException nullPointerException) {
			exception = nullPointerException;
		}
		assertNotNull(exception);

		try {
			// This should not throw a null pointer exception.
			operationCommand.dispose();
			exception = null;
		}
		catch (NullPointerException nullPointerException) {
			exception = nullPointerException;
		}
		assertNull(exception);
	}
	
	/**
	 * Tests that failure of an EMFOperationCommand used as a trigger will
	 * roll back a transaction.
	 */
	public void test_operationTriggerFails_234868() {
		final TestOperation trigger = new TestOperation(domain) {
		
			@Override
			protected void doExecute()
					throws ExecutionException {
				
				throw new ExecutionException("I should fail"); //$NON-NLS-1$
			}};
		
		TriggerListener listener = new TriggerListener() {
		
			@Override
			protected Command trigger(TransactionalEditingDomain domain,
					Notification notification) {
				return new EMFOperationCommand(domain, trigger);
			}};
		
		try {
			domain.addResourceSetListener(listener);
			
			startWriting();
			Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
			book.setCopies(book.getCopies() + 30);
			commitWithRollback();  // should roll back due to trigger
			
			fail("Should have rolled back."); //$NON-NLS-1$
		} catch (RollbackException rbe) {
			// success
			System.out.println("Got expected exception: " + rbe.getLocalizedMessage()); //$NON-NLS-1$
		} finally {
			domain.removeResourceSetListener(listener);
		}
	}
	
	/**
	 * Tests that execution of an EMFOperationCommand used as a trigger will
	 * roll back a transaction when the operation status is an ERROR.
	 */
	public void test_operationTriggerErrorStatus_234868() {
		final TestOperation trigger = new TestOperation(domain) {
		
			@Override
			protected void doExecute() {
				
				setStatus(new Status(IStatus.ERROR,
					"org.eclipse.emf.workspace.tests", "I should fail")); //$NON-NLS-1$ //$NON-NLS-2$
			}};
		
		TriggerListener listener = new TriggerListener() {
		
			@Override
			protected Command trigger(TransactionalEditingDomain domain,
					Notification notification) {
				return new EMFOperationCommand(domain, trigger);
			}};
		
		try {
			domain.addResourceSetListener(listener);
			
			startWriting();
			Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
			book.setCopies(book.getCopies() + 30);
			commitWithRollback();  // should roll back due to trigger
			
			fail("Should have rolled back."); //$NON-NLS-1$
		} catch (RollbackException rbe) {
			// success
			System.out.println("Got expected exception: " + rbe.getLocalizedMessage()); //$NON-NLS-1$
		} finally {
			domain.removeResourceSetListener(listener);
		}
	}
	
	/**
	 * Tests that execution of a non-EMF operation used as a trigger will
	 * roll back a transaction when the operation status is a ERROR.
	 */
	public void test_operationTriggerErrorStatus_nonEMF_234868() {
		final IUndoableOperation trigger = new AbstractOperation("Non-EMF Changes") { //$NON-NLS-1$
		
			@Override
			public IStatus execute(IProgressMonitor monitor, IAdaptable info)
					throws ExecutionException {
				return new Status(IStatus.ERROR,
					"org.eclipse.emf.workspace.tests", "I should fail"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			@Override
			public IStatus undo(IProgressMonitor monitor, IAdaptable info)
					throws ExecutionException {
				return Status.OK_STATUS;
			}
			
			@Override
			public IStatus redo(IProgressMonitor monitor, IAdaptable info)
					throws ExecutionException {
				return Status.OK_STATUS;
			}};
		
		TriggerListener listener = new TriggerListener() {
		
			@Override
			protected Command trigger(TransactionalEditingDomain domain,
					Notification notification) {
				return new EMFOperationCommand(domain, trigger);
			}};
		
		try {
			domain.addResourceSetListener(listener);
			
			startWriting();
			Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
			book.setCopies(book.getCopies() + 30);
			commitWithRollback();  // should roll back due to trigger
			
			fail("Should have rolled back."); //$NON-NLS-1$
		} catch (RollbackException rbe) {
			// success
			System.out.println("Got expected exception: " + rbe.getLocalizedMessage()); //$NON-NLS-1$
		} finally {
			domain.removeResourceSetListener(listener);
		}
	}
	
	/**
	 * Tests that undo of a non-EMF operation used as a trigger will
	 * roll back a transaction when the operation status is a ERROR.
	 */
	public void test_operationTriggerErrorStatus_nonEMF_undo_234868() {
		final IUndoableOperation trigger = new AbstractOperation("Non-EMF Changes") { //$NON-NLS-1$
		
			@Override
			public IStatus execute(IProgressMonitor monitor, IAdaptable info)
					throws ExecutionException {
				return Status.OK_STATUS;
			}
			
			@Override
			public IStatus undo(IProgressMonitor monitor, IAdaptable info)
					throws ExecutionException {
				return new Status(IStatus.ERROR,
					"org.eclipse.emf.workspace.tests", "I should fail"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			@Override
			public IStatus redo(IProgressMonitor monitor, IAdaptable info)
					throws ExecutionException {
				return Status.OK_STATUS;
			}};
		
		TriggerListener listener = new TriggerListener() {
		
			@Override
			protected Command trigger(TransactionalEditingDomain domain,
					Notification notification) {
				return new EMFOperationCommand(domain, trigger);
			}};
		
		try {
			domain.addResourceSetListener(listener);
			
			IUndoableOperation op = new TestOperation(domain) {
				protected void doExecute() throws ExecutionException {
					Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
					book.setCopies(book.getCopies() + 30);
				}};
			
			try {
				op.execute(null, null);
			} catch (ExecutionException e) {
				fail("Should not fail to execute: " + e.getLocalizedMessage()); //$NON-NLS-1$
			}
			
			try {
				op.undo(null, null);
				fail("Should have failed to undo."); //$NON-NLS-1$
			} catch (ExecutionException e) {
				// success
				System.out.println("Got expected exception: " + e.getLocalizedMessage()); //$NON-NLS-1$
			}
		} finally {
			domain.removeResourceSetListener(listener);
		}
	}
	
	/**
	 * Tests that redo of a non-EMF operation used as a trigger will
	 * roll back a transaction when the operation status is a ERROR.
	 */
	public void test_operationTriggerErrorStatus_nonEMF_redo_234868() {
		final IUndoableOperation trigger = new AbstractOperation("Non-EMF Changes") { //$NON-NLS-1$
		
			@Override
			public IStatus execute(IProgressMonitor monitor, IAdaptable info)
					throws ExecutionException {
				return Status.OK_STATUS;
			}
			
			@Override
			public IStatus undo(IProgressMonitor monitor, IAdaptable info)
					throws ExecutionException {
				return Status.OK_STATUS;
			}
			
			@Override
			public IStatus redo(IProgressMonitor monitor, IAdaptable info)
					throws ExecutionException {
				return new Status(IStatus.ERROR,
					"org.eclipse.emf.workspace.tests", "I should fail"); //$NON-NLS-1$ //$NON-NLS-2$
			}};
		
		TriggerListener listener = new TriggerListener() {
		
			@Override
			protected Command trigger(TransactionalEditingDomain domain,
					Notification notification) {
				return new EMFOperationCommand(domain, trigger);
			}};
		
		try {
			domain.addResourceSetListener(listener);
			
			IUndoableOperation op = new TestOperation(domain) {
				protected void doExecute() throws ExecutionException {
					Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
					book.setCopies(book.getCopies() + 30);
				}};
			
			try {
				op.execute(null, null);
			} catch (ExecutionException e) {
				fail("Should not fail to execute: " + e.getLocalizedMessage()); //$NON-NLS-1$
			}
			
			try {
				op.undo(null, null);
			} catch (ExecutionException e) {
				fail("Should not fail to undo: " + e.getLocalizedMessage()); //$NON-NLS-1$
			}
			
			try {
				op.redo(null, null);
				fail("Should have failed to redo."); //$NON-NLS-1$
			} catch (ExecutionException e) {
				// success
				System.out.println("Got expected exception: " + e.getLocalizedMessage()); //$NON-NLS-1$
			}
		} finally {
			domain.removeResourceSetListener(listener);
		}
	}
	
	//
	// Test fixtures
	//
	
	@Override
	protected void doSetUp()
		throws Exception {
		
		super.doSetUp();
		
		// enable validation
		validationEnabled = true;
	}
	
	@Override
	protected void doTearDown()
		throws Exception {
		
		// disable validation
		validationEnabled = false;
		
		super.doTearDown();
	}
}
