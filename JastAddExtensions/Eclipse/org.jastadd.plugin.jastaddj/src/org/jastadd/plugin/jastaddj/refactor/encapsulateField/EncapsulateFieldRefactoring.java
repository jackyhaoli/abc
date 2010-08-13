package org.jastadd.plugin.jastaddj.refactor.encapsulateField;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IEditorPart;
import org.jastadd.plugin.compiler.ast.IJastAddNode;

import AST.FieldDeclaration;

public class EncapsulateFieldRefactoring extends Refactoring {

	private IJastAddNode selectedNode;
	private RefactoringStatus status;
	private Change changes;

	public EncapsulateFieldRefactoring(IEditorPart editorPart,
			IFile editorFile, ISelection selection, IJastAddNode selectedNode) {
		super();
		this.selectedNode = selectedNode;
	}

	public String getName() {
		return "Encapsulate Field";
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		if(selectedNode instanceof FieldDeclaration)
			/*OK*/;
		else
			status.addFatalError("Not a field.");
		return status;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		if(status != null)
			return status;
		status = new RefactoringStatus();
		FieldDeclaration fd = (FieldDeclaration)selectedNode;
//		try {
//			fd.encapsulate();
//			Stack<ASTChange> ch = fd.programRoot().cloneUndoStack();
//			fd.programRoot().undo();
//			ChangeAccumulator accu = new ChangeAccumulator("EncapsulateField");
//			accu.addAllEdits(ch.iterator());
//			changes = accu.getChange();
//		} catch (RefactoringException rfe) {
//			status.addFatalError(rfe.getMessage());
//			fd.programRoot().undo();
//			changes = null;
//		}
		return status;
	}

	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		return changes;
	}
}
