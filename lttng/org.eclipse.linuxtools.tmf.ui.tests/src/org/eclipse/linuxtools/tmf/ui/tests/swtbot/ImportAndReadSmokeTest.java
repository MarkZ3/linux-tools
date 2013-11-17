/*******************************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *   Marc-Andre Laperle
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.ui.tests.swtbot;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.signal.TmfTimeSynchSignal;
import org.eclipse.linuxtools.tmf.core.tests.shared.CtfTmfTestTrace;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.ui.editors.TmfEventsEditor;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfProjectRegistry;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.linuxtools.tmf.ui.project.wizards.importtrace.BatchImportTraceWizard;
import org.eclipse.linuxtools.tmf.ui.tests.swtbot.conditions.ConditionHelpers;
import org.eclipse.linuxtools.tmf.ui.views.TracingPerspectiveFactory;
import org.eclipse.linuxtools.tmf.ui.views.histogram.HistogramView;
import org.eclipse.linuxtools.tmf.ui.views.statistics.TmfStatisticsView;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.views.properties.PropertySheet;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * SWTBot Smoke test. base for other tests
 *
 * @author Matthew Khouzam
 */
public class ImportAndReadSmokeTest {

    private static final String TRACING_PERSPECTIVE_ID = TracingPerspectiveFactory.ID;
    private static final String TRACE_PROJECT_NAME = "test";

    private static SWTWorkbenchBot fBot;
    private static Wizard fWizard;
    private static CtfTmfTestTrace fTrace = CtfTmfTestTrace.SYNTHETIC_TRACE;
    private static final String TRACE_NAME = "synthetic-trace";
    private ITmfEvent fDesired1;
    private ITmfEvent fDesired2;

    /** The Log4j logger instance. */
    private static final Logger fLogger = Logger.getRootLogger();

    /** Test Class setup */
    @BeforeClass
    public static void init() {
        if (Display.getCurrent() != null && Display.getCurrent().getThread() == Thread.currentThread()) {
            fail("SWTBot test needs to run in a non-UI thread. Make sure that \"Run in UI thread\" is unchecked in your launch configuration or"
                    + " that useUIThread is set to false in the pom.xml");
        }

        /*
         * set up for swtbot
         */
        SWTBotPreferences.TIMEOUT = 50000000; /* 50 second timeout */
        fLogger.addAppender(new ConsoleAppender(new SimpleLayout(), "System.out"));
        fBot = new SWTWorkbenchBot();

        final List<SWTBotView> openViews = fBot.views();
        for (SWTBotView view : openViews) {
            if (view.getTitle().equals("Welcome")) {
                view.close();
                fBot.waitUntil(ConditionHelpers.ViewIsClosed(view));
            }
        }

        switchToTracingPerspective();
        /*
         * finish waiting for eclipse to load
         */
        waitForJobs();
    }

    private static void deleteProject() {
        try {
            getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
        }

        waitForJobs();

        final SWTBotView projectViewBot = fBot.viewById(IPageLayout.ID_PROJECT_EXPLORER);
        projectViewBot.setFocus();

        SWTBotTree treeBot = fBot.tree();
        SWTBotTreeItem treeItem = treeBot.getTreeItem(TRACE_PROJECT_NAME);
        SWTBotMenu contextMenu = treeItem.contextMenu("Delete");
        contextMenu.click();

        String shellText = "Delete Resources";
        fBot.waitUntil(Conditions.shellIsActive(shellText));
        final SWTBotShell shell = fBot.shell(shellText);

        // find modal window
        final Button[] desiredWidget = new Button[1];

        SWTBotButton[] but = new SWTBotButton[2];
        but[0] = fBot.button("OK");
        but[1] = fBot.button(2);

        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                // this should find the OK button. Hard
                // coded
                desiredWidget[0] = (Button) ((Composite) ((Composite) ((Composite) shell.widget.getShell().getChildren()[0]).getChildren()[2]).getChildren()[0]).getChildren()[2];
            }
        });
        final SWTBotButton okButton = new SWTBotButton(desiredWidget[0]);
        fBot.waitUntil(Conditions.widgetIsEnabled(okButton));
        okButton.click();

        waitForJobs();
    }

    private static void switchToTracingPerspective() {
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                try {
                    PlatformUI.getWorkbench().showPerspective(TRACING_PERSPECTIVE_ID, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                } catch (WorkbenchException e) {
                    fail(e.getMessage());
                }
            }
        });
    }

    private static void focusMainWindow() {
        for (SWTBotShell shellBot : fBot.shells()) {
            if (shellBot.getText().toLowerCase().contains("eclipse")) {
                shellBot.activate();
            }
        }
    }

    /**
     * Waits for all Eclipse jobs to finish
     */
    protected static void waitForJobs() {
        while (!Job.getJobManager().isIdle()) {
            delay(100);
        }
        refreshUI();
    }

    /**
     * Sleeps current thread or GUI thread for a given time.
     *
     * @param waitTimeMillis
     *            time in milliseconds to wait
     */
    protected static void delay(final long waitTimeMillis) {
        final Display display = fBot.getDisplay();
        if (display != null) {
            final long endTimeMillis = System.currentTimeMillis() + waitTimeMillis;
            while (System.currentTimeMillis() < endTimeMillis) {
                UIThreadRunnable.syncExec(new VoidResult() {
                    @Override
                    public void run() {
                        refreshUI();
                        display.update();
                    }

                });

            }
        } else {
            try {
                Thread.sleep(waitTimeMillis);
            } catch (final InterruptedException e) {
                // Ignored
            }
        }
    }

    private IEditorReference[] ieds;
    private IEditorPart iep;
    final String traceTypeName = "Generic CTF Trace";

    /**
     * Main test case
     *
     * @throws InterruptedException
     *             exception
     */
    @Test
    public void test() throws InterruptedException {

        createProject();
        // reset
        final SWTBotView projectExplorerBot = fBot.viewById(IPageLayout.ID_PROJECT_EXPLORER);
        projectExplorerBot.setFocus();

        final SWTBotTree tree = fBot.tree();
        final SWTBotTreeItem treeItem = tree.getTreeItem(TRACE_PROJECT_NAME);

        UIThreadRunnable.syncExec(new VoidResult() {

            @Override
            public void run() {
                refreshUI();

                treeItem.select();
                treeItem.click();
                treeItem.expand();

                treeItem.getNode("Traces [0]").select();
                waitForJobs();
            }

        });

        fWizard = new BatchImportTraceWizard();
        UIThreadRunnable.asyncExec(new VoidResult() {
            @Override
            public void run() {
                openBatchImportTraceWizard();
            }
        });
        fBot.waitUntil(ConditionHelpers.isWizardReady(fWizard));

        selecTraceType();
        addDirectoryPage();
        selectTrace();
        finishBitw();

        waitForJobs();

        List<String> nodes = treeItem.getNodes();
        String nodeName = "";
        for (String node : nodes) {
            if (node.startsWith("Traces")) {
                nodeName = node;
            }
        }
        fBot.waitUntil(ConditionHelpers.IsTreeChildNodeAvailable(nodeName, treeItem));
        treeItem.getNode(nodeName).expand();
        fBot.waitUntil(ConditionHelpers.IsTreeChildNodeAvailable(TRACE_NAME, treeItem.getNode(nodeName)));
        treeItem.getNode(nodeName).getNode(TRACE_NAME).select();
        treeItem.getNode(nodeName).getNode(TRACE_NAME).doubleClick();
        UIThreadRunnable.syncExec(new VoidResult() {

            @Override
            public void run() {
                fBot.getDisplay().sleep();
            }

        });
        Thread.sleep(1000);
        waitForJobs();
        UIThreadRunnable.syncExec(new VoidResult() {

            @Override
            public void run() {
                ieds = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
                assertNotNull(ieds);
                iep = null;
                for (IEditorReference ied : ieds) {
                    if (ied.getTitle().equals(TRACE_NAME)) {
                        iep = ied.getEditor(true);
                        break;
                    }
                }
            }

        });
        assertNotNull(iep);
        fDesired1 = getEvent(100);
        fDesired2 = getEvent(10000);
        final TmfEventsEditor tmfEd = (TmfEventsEditor) iep;

        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                tmfEd.setFocus();
                tmfEd.selectionChanged(new SelectionChangedEvent(tmfEd, new StructuredSelection(fDesired1)));
            }
        });

        waitForJobs();
        Thread.sleep(1000);
        assertNotNull(tmfEd);
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                IViewReference[] viewRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
                for (IViewReference viewRef : viewRefs) {
                    IViewPart vp = viewRef.getView(true);
                    if (vp.getTitle().equals("Histogram")) {
                        testHistogramView(vp);
                    } else if (vp.getTitle().equals("Properties")) {
                        testPropertyView(vp);
                    } else if (vp.getTitle().equals("Statistics")) {
                        testStatisticsView(vp);
                    }
                }
            }
        });

        deleteProject();
    }

    private void selecTraceType() {
        final SWTBotTree tree = fBot.tree();
        final String ctfId = "Common Trace Format";
        final String lttngId = traceTypeName;
        fBot.waitUntil(ConditionHelpers.IsTreeNodeAvailable(ctfId, tree));
        fBot.waitUntil(ConditionHelpers.IsTreeChildNodeAvailable(lttngId, tree.getTreeItem(ctfId)));
        tree.getTreeItem(ctfId).getNode(lttngId).check();
        clickNextBitw();
    }

    private static void addDirectoryPage() {
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                ((BatchImportTraceWizard) fWizard).addFileToScan(fTrace.getPath());
            }
        });
        final SWTBotButton removeButton = fBot.button("Remove");
        fBot.waitUntil(Conditions.widgetIsEnabled(removeButton));
        removeButton.click();
        fBot.waitUntil(Conditions.tableHasRows(fBot.table(), 1));

        clickNextBitw();
    }

    private void selectTrace() {
        SWTBotTree tree = fBot.tree();
        fBot.waitUntil(Conditions.widgetIsEnabled(tree));
        final SWTBotTreeItem genericCtfTreeItem = tree.getTreeItem(traceTypeName);
        fBot.waitUntil(Conditions.widgetIsEnabled(genericCtfTreeItem));
        genericCtfTreeItem.expand();
        genericCtfTreeItem.check();
        clickNextBitw();
    }

    private static void finishBitw() {
        SWTBotShell shell = fBot.activeShell();
        final SWTBotButton finishButton = fBot.button("Finish");
        finishButton.click();
        fBot.waitUntil(Conditions.shellCloses(shell));

    }

    private static void clickNextBitw() {
        IWizardPage currentPage = fWizard.getContainer().getCurrentPage();
        IWizardPage desiredPage = fWizard.getNextPage(currentPage);
        SWTBotButton nextButton = fBot.button("Next >");
        nextButton.click();
        fBot.waitUntil(ConditionHelpers.isWizardOnPage(fWizard, desiredPage));
    }

    // ---------------------------------------------
    // Helpers for testing views
    // ---------------------------------------------

    private static void testPropertyView(IViewPart vp) {
        PropertySheet pv = (PropertySheet) vp;
        assertNotNull(pv);
    }

    private void testHistogramView(IViewPart vp) {
        SWTBotView hvBot = (new SWTWorkbenchBot()).viewById(HistogramView.ID);
        List<SWTBotToolbarButton> hvTools = hvBot.getToolbarButtons();
        for (SWTBotToolbarButton hvTool : hvTools) {
            if (hvTool.getToolTipText().toLowerCase().contains("lost")) {
                hvTool.click();
            }
        }
        HistogramView hv = (HistogramView) vp;
        final TmfTimeSynchSignal signal = new TmfTimeSynchSignal(hv, fDesired1.getTimestamp());
        final TmfTimeSynchSignal signal2 = new TmfTimeSynchSignal(hv, fDesired2.getTimestamp());
        hv.updateTimeRange(100000);
        waitForJobs();
        hv.currentTimeUpdated(signal);
        hv.broadcast(signal);
        waitForJobs();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        hv.updateTimeRange(1000000000);
        waitForJobs();
        hv.currentTimeUpdated(signal2);
        hv.broadcast(signal2);
        waitForJobs();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        assertNotNull(hv);
    }

    private static void testStatisticsView(IViewPart vp) {
        TmfStatisticsView sv = (TmfStatisticsView) vp;
        assertNotNull(sv);
    }

    // ---------------------------------------------
    // Trace helpers
    // ---------------------------------------------

    private static CtfTmfEvent getEvent(int rank) {
        CtfTmfTrace trace = fTrace.getTrace();
        if (trace == null) {
            return null;
        }
        ITmfContext ctx = trace.seekEvent(0);
        for (int i = 0; i < rank; i++) {
            trace.getNext(ctx);
        }
        final CtfTmfEvent retVal = trace.getNext(ctx);
        trace.dispose();
        return retVal;
    }

    private static void refreshUI() {
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                Display display = fBot.getDisplay();
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        });
    }

    private static void createProject() {
        focusMainWindow();
        fBot.menu("File").menu("New").menu("Project...").click();

        fBot.waitUntil(Conditions.shellIsActive("New Project"));
        SWTBotTree tree = fBot.tree();
        assertNotNull(tree);
        final String tracingKey = "Tracing";
        fBot.waitUntil(ConditionHelpers.IsTreeNodeAvailable(tracingKey, tree));
        final SWTBotTreeItem tracingNode = tree.expandNode(tracingKey);

        tracingNode.select();
        final String projectKey = "Tracing Project";
        fBot.waitUntil(ConditionHelpers.IsTreeChildNodeAvailable(projectKey, tracingNode));
        final SWTBotTreeItem tracingProject = tracingNode.getNode(projectKey);
        assertNotNull(tracingProject);

        tracingProject.select();
        tracingProject.click();

        SWTBotButton nextButton = fBot.button("Next >");
        fBot.waitUntil(Conditions.widgetIsEnabled(nextButton));
        nextButton.click();
        fBot.waitUntil(Conditions.shellIsActive("Tracing Project"));

        final SWTBotText text = fBot.text();
        text.setText(TRACE_PROJECT_NAME);

        fBot.button("Finish").click();
    }

    private static void openBatchImportTraceWizard() {
        IProject project = getProject();
        assertNotNull(project);

        TmfProjectElement projectElement = TmfProjectRegistry.getProject(project);
        TmfTraceFolder traceFolder = projectElement.getTracesFolder();
        if (traceFolder == null) {
            return;
        }

        final IWorkbench workbench = PlatformUI.getWorkbench();
        // Fire the Import Trace Wizard
        if (workbench != null) {
            final IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
            if (activeWorkbenchWindow != null) {
                Shell shell = activeWorkbenchWindow.getShell();

                ((BatchImportTraceWizard) fWizard).init(PlatformUI.getWorkbench(), new StructuredSelection(traceFolder));
                WizardDialog dialog = new WizardDialog(shell, fWizard);
                dialog.open();

                traceFolder.refresh();
            }
        }
    }

    private static IProject getProject() {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(TRACE_PROJECT_NAME);
        return project;
    }
}
