/**
 * <copyright>
 *
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: AbstractEMFOperationTest.java,v 1.1 2006/01/30 16:26:01 cdamus Exp $
 */
package org.eclipse.emf.workspace.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.examples.extlibrary.Book;
import org.eclipse.emf.examples.extlibrary.EXTLibraryFactory;
import org.eclipse.emf.examples.extlibrary.Library;
import org.eclipse.emf.examples.extlibrary.Writer;
import org.eclipse.emf.transaction.Transaction;
import org.eclipse.emf.workspace.AbstractEMFOperation;
import org.eclipse.emf.workspace.tests.fixtures.ItemDefaultPublicationDateTrigger;
import org.eclipse.emf.workspace.tests.fixtures.LibraryDefaultBookTrigger;
import org.eclipse.emf.workspace.tests.fixtures.LibraryDefaultNameTrigger;
import org.eclipse.emf.workspace.tests.fixtures.TestListener;
import org.eclipse.emf.workspace.tests.fixtures.TestOperation;
import org.eclipse.emf.workspace.tests.fixtures.TestUndoContext;


/**
 * Tests the {@link AbstractEMFOperation} framework.
 *
 * @author Christian W. Damus (cdamus)
 */
public class AbstractEMFOperationTest extends AbstractTest {

	public AbstractEMFOperationTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new TestSuite(AbstractEMFOperationTest.class, "EMF Operation Tests"); //$NON-NLS-1$
	}
	
	public void test_execute_undo_redo() {
		startReading();
		
		final Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
		assertNotNull(book);
		final String oldTitle = book.getTitle();
		final Writer oldAuthor = book.getAuthor();
		
		final String newTitle = "New Title"; //$NON-NLS-1$
		final Writer newAuthor = (Writer) find("root/level1/Level1 Writer"); //$NON-NLS-1$
		assertNotNull(newAuthor);
		
		commit();
		
		IUndoContext ctx = new TestUndoContext();
		
		IUndoableOperation oper = new TestOperation(domain) {
			protected void doExecute() {
				book.setTitle(newTitle);
				newAuthor.getBooks().add(book);
			}};
		
		try {
			oper.addContext(ctx);
			history.execute(oper, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were applied
		assertSame(newTitle, book.getTitle());
		assertSame(newAuthor, book.getAuthor());
		
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
		assertSame(oldAuthor, book.getAuthor());
		
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
		assertSame(newAuthor, book.getAuthor());
		
		commit();
	}
	
	/**
	 * Tests that trigger commands are executed correctly when executing operations,
	 * including undo and redo.
	 */
	public void test_triggerCommands() {
		// one trigger sets default library names
		domain.addResourceSetListener(new LibraryDefaultNameTrigger());
		
		// another (distinct) trigger creates default books in new libraries
		domain.addResourceSetListener(new LibraryDefaultBookTrigger());
		
		final Library[] newLibrary = new Library[1];
		
		IUndoContext ctx = new TestUndoContext();
		
		IUndoableOperation oper = new TestOperation(domain) {
			protected void doExecute() {
				// add a new library.  Our triggers will set a default name and book
				newLibrary[0] = EXTLibraryFactory.eINSTANCE.createLibrary();
				root.getBranches().add(newLibrary[0]);
				
				assertNull(newLibrary[0].getName());
				assertTrue(newLibrary[0].getBranches().isEmpty());
			}};
		
		try {
			oper.addContext(ctx);
			history.execute(oper, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		assertEquals("New Library", newLibrary[0].getName()); //$NON-NLS-1$
		assertEquals(1, newLibrary[0].getBooks().size());
		assertEquals("New Book", ((Book) newLibrary[0].getBooks().get(0)).getTitle()); //$NON-NLS-1$
		
		commit();

		try {
			assertTrue(history.canUndo(ctx));
			history.undo(ctx, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were undone
		assertFalse(root.getBranches().contains(newLibrary[0]));
		
		commit();
		
		try {
			assertTrue(history.canRedo(ctx));
			history.redo(ctx, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were redone
		assertTrue(root.getBranches().contains(newLibrary[0]));
		assertEquals("New Library", newLibrary[0].getName()); //$NON-NLS-1$
		assertEquals(1, newLibrary[0].getBooks().size());
		assertEquals("New Book", ((Book) newLibrary[0].getBooks().get(0)).getTitle()); //$NON-NLS-1$
		
		commit();
	}
	
	/**
	 * Tests that a command resulting from a pre-commit (trigger) listener will,
	 * itself, trigger further changes.
	 */
	public void test_triggerCommands_cascading() {
		// add the trigger to create a default book in a new library
		domain.addResourceSetListener(new LibraryDefaultBookTrigger());
		
		// add another trigger that will set default publication dates for new items
		domain.addResourceSetListener(new ItemDefaultPublicationDateTrigger());
		
		final Library[] newLibrary = new Library[1];
		
		IUndoContext ctx = new TestUndoContext();
		
		IUndoableOperation oper = new TestOperation(domain) {
			protected void doExecute() {
				// add a new library.  Our triggers will set a default name and book
				newLibrary[0] = EXTLibraryFactory.eINSTANCE.createLibrary();
				root.getBranches().add(newLibrary[0]);
				
				assertNull(newLibrary[0].getName());
				assertTrue(newLibrary[0].getBranches().isEmpty());
			}};
		
		try {
			oper.addContext(ctx);
			history.execute(oper, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		// the book is created by the first trigger
		assertEquals(1, newLibrary[0].getBooks().size());
		Book book = (Book) newLibrary[0].getBooks().get(0);
		assertEquals("New Book", book.getTitle()); //$NON-NLS-1$
		
		// the publication date is created by the cascaded trigger
		assertNotNull(book.getPublicationDate());
		
		commit();

		try {
			assertTrue(history.canUndo(ctx));
			history.undo(ctx, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were undone
		assertFalse(root.getBranches().contains(newLibrary[0]));
		
		commit();
		
		try {
			assertTrue(history.canRedo(ctx));
			history.redo(ctx, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		}
		
		startReading();
		
		// verify that the changes were redone
		assertTrue(root.getBranches().contains(newLibrary[0]));
		assertEquals(1, newLibrary[0].getBooks().size());
		book = (Book) newLibrary[0].getBooks().get(0);
		assertEquals("New Book", book.getTitle()); //$NON-NLS-1$
		assertNotNull(book.getPublicationDate());
		
		commit();
	}
	
	/**
	 * Tests that validation correctly rolls back changes and fails execution.
	 */
	public void test_validation() {
		startReading();
		
		final Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
		assertNotNull(book);
		final String oldTitle = book.getTitle();
		final Writer oldAuthor = book.getAuthor();
		
		final String newTitle = null; // will fail validation
		final Writer newAuthor = (Writer) find("root/level1/Level1 Writer"); //$NON-NLS-1$
		assertNotNull(newAuthor);
		
		commit();
		
		IUndoContext ctx = new TestUndoContext();
		
		IUndoableOperation oper = new TestOperation(domain) {
			protected void doExecute() {
				book.setTitle(newTitle);
				newAuthor.getBooks().add(book);
			}};
		
		IStatus status = null;
			
		try {
			validationEnabled = true;
			oper.addContext(ctx);
			status = history.execute(oper, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		} finally {
			validationEnabled = false;
		}
		
		assertNotNull(status);
		assertTrue(status.matches(IStatus.ERROR));
		
		status = findValidationStatus(status, IStatus.ERROR);
		assertNotNull(status);
		
		startReading();
		
		// verify that the changes were rolled back
		assertSame(oldTitle, book.getTitle());
		assertSame(oldAuthor, book.getAuthor());
		
		commit();
	}
	
	/**
	 * Tests the application of options to the transaction used for executing.
	 */
	public void test_options_124741() {
		startReading();
		
		final Book book = (Book) find("root/Root Book"); //$NON-NLS-1$
		assertNotNull(book);
		
		final String newTitle = null; // would cause validation failure
		final Writer newAuthor = (Writer) find("root/level1/Level1 Writer"); //$NON-NLS-1$
		assertNotNull(newAuthor);
		
		commit();
		
		TestListener listener = new TestListener();
		domain.addResourceSetListener(listener);
		
		IUndoContext ctx = new TestUndoContext();
		
		IUndoableOperation oper = new TestOperation(
				domain,
				makeOptions(new Object[] {
					Transaction.OPTION_NO_NOTIFICATIONS, Boolean.TRUE,
					Transaction.OPTION_NO_TRIGGERS, Boolean.TRUE,
					Transaction.OPTION_NO_VALIDATION, Boolean.TRUE,
					Transaction.OPTION_NO_UNDO, Boolean.TRUE
				})) {
			protected void doExecute() {
				book.setTitle(newTitle);
				newAuthor.getBooks().add(book);
			}};
		
		IStatus status = null;
		try {
			validationEnabled = true;
			oper.addContext(ctx);
			status = history.execute(oper, new NullProgressMonitor(), null);
		} catch (ExecutionException e) {
			fail(e);
		} finally {
			validationEnabled = false;
		}
		
		startReading();
		
		// verify that the changes were applied.  This asserts that execution did
		//    succeed
		assertSame(newTitle, book.getTitle());
		assertSame(newAuthor, book.getAuthor());
		
		commit();

		// no validation was performed
		assertNotNull(status);
		assertTrue(status.isOK());
		
		// no undo information was recorded
		assertFalse(history.canUndo(ctx));
		
		// no triggers were invoked
		assertNull(listener.precommit);
		
		// no listeners were notified
		assertNull(listener.postcommit);
	}
}