/**********************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 **********************************************************************/

package org.eclipse.linuxtools.internal.tracing.rcp.ui.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.linuxtools.internal.tracing.rcp.ui.TracingRcpPlugin;
import org.eclipse.linuxtools.internal.tracing.rcp.ui.messages.Messages;
import org.eclipse.linuxtools.tmf.core.TmfCommonConstants;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.editors.TmfEditorInput;
import org.eclipse.linuxtools.tmf.ui.editors.TmfEventsEditor;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfNavigatorContentProvider;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfProjectRegistry;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Open trace helper, takes a file and opens it.
 *
 * @author Matthew Khouzam
 */
public class OpenTraceHelper {

    private static final String TRACES_DIRECTORY = "Traces"; //$NON-NLS-1$
    private static final String DEFAULT_TRACE_ICON_PATH = "icons" + File.separator + "elcl16" + File.separator + "trace.gif"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    /**
     * Opens a dialog to select trace type, imports the trace and opens it
     *
     * @param file
     *            the file to import
     * @param shell
     *            the shell to use for dialogs
     * @return IStatus OK if successful
     * @throws CoreException
     *             core exceptions if something is not well set up in the back
     *             end
     */
    public IStatus open(String file, Shell shell) throws CoreException {
        TmfTraceType tt = TmfTraceType.getInstance();
        String[] traceTypes = tt.getAvailableTraceTypes();
        ArrayList<String> candidates = new ArrayList<String>();
        for (String traceType : traceTypes) {
            String[] traceInfo = traceType.split(":", 2); //$NON-NLS-1$
            String traceTypeId = tt.getTraceTypeId(traceInfo[0], traceInfo[1]);

            if (tt.validate(traceTypeId, file)) {
                candidates.add(traceType);
            }
        }
        String traceTypeToSet;
        if (candidates.isEmpty()) {
            MessageBox mb = new MessageBox(shell);
            final String errorMsg = Messages.OpenTraceHelper_NoTraceTypeMatch + file;
            mb.setMessage(errorMsg);
            mb.open();
            return new Status(IStatus.ERROR, TracingRcpPlugin.PLUGIN_ID,
                    errorMsg);
        } else if (candidates.size() != 1) {
            traceTypeToSet = getTraceTypeToSet(candidates, shell);
            if (traceTypeToSet == null) {
                return Status.CANCEL_STATUS;
            }
        } else {
            traceTypeToSet = candidates.get(0);
        }

        String[] traceInfo = traceTypeToSet.split(":", 2); //$NON-NLS-1$
        String traceTypeId = tt.getTraceTypeId(traceInfo[0], traceInfo[1]);
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(Messages.ApplicationWorkbenchWindowAdvisor_DefaultProjectName);
        IFolder folder = project.getFolder(TRACES_DIRECTORY);
        String traceName = getTraceName(file, folder);
        if (traceExists(file, folder)) {
            return openTrace(traceName);
        }
        final IPath tracePath = folder.getFullPath().append(traceName);
        final IPath pathString = Path.fromOSString(file);
        IResource linkedTrace = createLink(folder, pathString, traceName);
        if (linkedTrace != null && linkedTrace.exists()) {
            IStatus ret = setTraceType(tracePath, traceTypeId);
            if (ret.isOK()) {
                ret = openTrace(traceName);
            }
            return ret;
        }
        return new Status(IStatus.ERROR, TracingRcpPlugin.PLUGIN_ID,
                Messages.OpenTraceHelper_LinkFailed);
    }

    private static boolean traceExists(String file, IFolder folder) {
        String val = getTraceName(file, folder);
        return (folder.findMember(val) != null);
    }

    private static boolean isWrongMember(IFolder folder, String ret,
            final File traceFile) {
        final IResource candidate = folder.findMember(ret);
        if (candidate != null) {
            final IPath rawLocation = candidate.getRawLocation();
            final File file = rawLocation.toFile();
            return !file.equals(traceFile);
        }
        return false;
    }

    /**
     * Gets the display name, either "filename" or "filename(n)" if there is
     * already a filename existing where n is the next non-used integer starting
     * from 2
     *
     * @param file
     *            the file with path
     * @param folder
     *            the folder to import to
     * @return the filename
     */
    private static String getTraceName(String file, IFolder folder) {
        String ret;
        final File traceFile = new File(file);
        ret = traceFile.getName();
        for (int i = 2; isWrongMember(folder, ret, traceFile); i++) {
            ret = traceFile.getName() + '(' + i + ')';
        }
        return ret;
    }

    private static IStatus setTraceType(IPath path, String traceTypeID)
            throws CoreException {
        IResource resource = ResourcesPlugin.getWorkspace().getRoot()
                .findMember(path);
        String TRACE_NAME = path.lastSegment();
        String traceBundle = null, traceTypeId = traceTypeID, traceIcon = null;
        IConfigurationElement ce = TmfTraceType.getInstance().getTraceAttributes(traceTypeId);
        traceBundle = ce.getContributor().getName();
        traceIcon = ce.getAttribute(TmfTraceType.ICON_ATTR);
        final String traceType = traceTypeId;

        if (TmfTraceType.isCustomTrace(traceType)) {
            traceTypeId = TmfTraceType.getCustomTraceTypeId(traceTypeId);
            traceBundle = TracingRcpPlugin.getDefault().getBundle().getSymbolicName();
            traceIcon = DEFAULT_TRACE_ICON_PATH;
        }

        resource.setPersistentProperty(TmfCommonConstants.TRACEBUNDLE, traceBundle);
        resource.setPersistentProperty(TmfCommonConstants.TRACETYPE, traceTypeId);
        resource.setPersistentProperty(TmfCommonConstants.TRACEICON, traceIcon);

        TmfProjectElement tmfProject = TmfProjectRegistry.getProject(resource.getProject());
        if (tmfProject != null) {
            final TmfTraceFolder tracesFolder = tmfProject.getTracesFolder();
            tracesFolder.refresh();

            List<TmfTraceElement> traces = tracesFolder.getTraces();
            boolean found = false;
            for (TmfTraceElement traceElement : traces) {
                if (traceElement.getName().equals(resource.getName())) {
                    traceElement.refreshTraceType();
                    found = true;
                    break;
                }
            }
            if (!found) {
                TmfTraceElement te = new TmfTraceElement(TRACE_NAME, resource, tracesFolder);
                te.refreshTraceType();
                traces = tracesFolder.getTraces();
                for (TmfTraceElement traceElement : traces) {
                    traceElement.refreshTraceType();
                }
            }
        }
        return Status.OK_STATUS;

    }

    private static IResource createLink(IFolder parentFolder, IPath location,
            String targetName) throws CoreException {
        File source = new File(location.toString());
        IResource res = null;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        if (source.isDirectory()) {
            IFolder folder = parentFolder.getFolder(targetName);
            IStatus result = workspace.validateLinkLocation(folder, location);
            if (result.isOK()) {
                folder.createLink(location, IResource.REPLACE, new NullProgressMonitor());
            } else {
                TracingRcpPlugin.getDefault().logError(result.getMessage());
            }
        } else {
            IFile file = parentFolder.getFile(targetName);
            IStatus result = workspace.validateLinkLocation(file, location);
            if (result.isOK()) {
                file.createLink(location, IResource.REPLACE,
                        new NullProgressMonitor());
            } else {
                TracingRcpPlugin.getDefault().logError(result.getMessage());
            }
        }
        final TmfNavigatorContentProvider ncp = new TmfNavigatorContentProvider();
        // force the model to be populated
        ncp.getChildren(parentFolder.getProject());
        res = parentFolder.findMember(location.lastSegment());
        return res;
    }

    private String fCandidate;

    private String getTraceTypeToSet(ArrayList<String> candidates, Shell shell) {
        Shell shellToShow = new Shell(shell);
        for (String candidate : candidates) {
            Button b = new Button(shellToShow, SWT.RADIO);
            b.setText(candidate);
            b.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    final Button source = (Button) e.getSource();
                    setCandidate(source.getText());
                    source.getParent().dispose();
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {

                }
            });
        }
        shellToShow.setLayout(new RowLayout(SWT.VERTICAL));
        shellToShow.pack();
        shellToShow.open();

        Display display = shellToShow.getDisplay();
        while (!shellToShow.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return fCandidate;
    }

    private synchronized void setCandidate(String fCandidate) {
        this.fCandidate = fCandidate;
    }

    private static IStatus openTrace(String traceName) {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IWorkspaceRoot root = workspace.getRoot();
        IProject project = root
                .getProject(Messages.ApplicationWorkbenchWindowAdvisor_DefaultProjectName);
        final TmfProjectElement project2 = TmfProjectRegistry.getProject(
                project, true);
        final TmfTraceFolder tracesFolder = project2.getTracesFolder();
        final List<TmfTraceElement> traces = tracesFolder.getTraces();
        TmfTraceElement found = null;
        for (TmfTraceElement candidate : traces) {
            if (candidate.getName().equals(traceName)) {
                found = candidate;
            }
        }
        if (found == null) {
            return new Status(IStatus.ERROR, TracingRcpPlugin.PLUGIN_ID,
                    Messages.OpenTraceHelper_OpenError);
        }
        final TmfTraceElement traceElement = found;
        Thread thread = new Thread() {
            @Override
            public void run() {

                final ITmfTrace trace = traceElement.instantiateTrace();
                final ITmfEvent traceEvent = traceElement.instantiateEvent();
                if ((trace == null) || (traceEvent == null)) {
                    if (trace != null) {
                        trace.dispose();
                    }
                    return;
                }

                // Get the editor_id from the extension point
                String traceEditorId = traceElement.getEditorId();
                final String editorId = (traceEditorId != null) ? traceEditorId : TmfEventsEditor.ID;
                try {
                    trace.initTrace(traceElement.getResource(), traceElement.getLocation().getPath(), traceEvent.getClass());
                } catch (final TmfTraceException e) {
                    trace.dispose();
                    return;
                }

                final IFile file;
                try {
                    file = traceElement.createBookmarksFile();
                } catch (final CoreException e) {
                    TracingRcpPlugin.getDefault().logError(Messages.OpenTraceHelper_OpenTraceError + traceElement.getName());
                    trace.dispose();
                    return;
                }

                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final IEditorInput editorInput = new TmfEditorInput(file, trace);
                            final IWorkbench wb = PlatformUI.getWorkbench();
                            final IWorkbenchPage activePage = wb.getActiveWorkbenchWindow().getActivePage();
                            final IEditorPart editor = activePage.findEditor(new FileEditorInput(file));
                            if ((editor != null) && (editor instanceof IReusableEditor)) {
                                activePage.reuseEditor((IReusableEditor) editor, editorInput);
                                activePage.activate(editor);
                            } else {
                                activePage.openEditor(editorInput, editorId);
                                IDE.setDefaultEditor(file, editorId);
                                // editor should dispose the trace on close
                            }
                        } catch (final PartInitException e) {
                            TracingRcpPlugin.getDefault().logError(Messages.OpenTraceHelper_ErrorOpeningTrace + traceElement.getName());
                            trace.dispose();
                        }
                    }
                });
            }
        };
        thread.start();
        return Status.OK_STATUS;
    }
}
