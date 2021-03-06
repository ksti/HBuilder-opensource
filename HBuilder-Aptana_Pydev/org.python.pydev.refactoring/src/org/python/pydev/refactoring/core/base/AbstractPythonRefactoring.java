/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.base;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.refactoring.core.change.CompositeChangeProcessor;
import org.python.pydev.refactoring.core.change.IChangeProcessor;

public abstract class AbstractPythonRefactoring extends Refactoring {
    protected RefactoringStatus status;
    protected RefactoringInfo info;

    public AbstractPythonRefactoring(RefactoringInfo info) {
        status = new RefactoringStatus();
        this.info = info;
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
        return new RefactoringStatus();
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
        return status;
    }

    @Override
    public Change createChange(IProgressMonitor pm) {
        IChangeProcessor changeProcessor = new CompositeChangeProcessor(getName(), getChangeProcessors());

        try {
            return changeProcessor.createChange();
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract List<IChangeProcessor> getChangeProcessors();
}
