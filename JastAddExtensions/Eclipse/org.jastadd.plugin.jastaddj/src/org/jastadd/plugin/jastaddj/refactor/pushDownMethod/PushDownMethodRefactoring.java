package org.jastadd.plugin.jastaddj.refactor.pushDownMethod;

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

import AST.MethodDecl;

public class PushDownMethodRefactoring extends Refactoring {

	private IJastAddNode selectedNode;
	private RefactoringStatus status;
	private Change changes;

	public PushDownMethodRefactoring(IEditorPart editorPart,
			IFile editorFile, ISelection selection, IJastAddNode selectedNode) {
		super();
		this.selectedNode = selectedNode;
	}

	public String getName() {
		return "Push Down Method";
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		if(selectedNode instanceof MethodDecl)
			/*OK*/;
		else
			status.addFatalError("Not a method.");
		return status;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		if(status != null)
			return status;
		status = new RefactoringStatus();
		MethodDecl m = (MethodDecl)selectedNode;
//		try {
//			m.pushDown();
//			Stack<ASTChange> ch = m.programRoot().cloneUndoStack();
//			m.programRoot().undo();
//			ChangeAccumulator accu = new ChangeAccumulator("PushDownMethod");
//			accu.addAllEdits(ch.iterator());
//			changes = accu.getChange();
//		} catch (RefactoringException rfe) {
//			status.addFatalError(rfe.getMessage());
//			m.programRoot().undo();
//			changes = null;
//		}
		return status;
	}

	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		return changes;
	}
}
