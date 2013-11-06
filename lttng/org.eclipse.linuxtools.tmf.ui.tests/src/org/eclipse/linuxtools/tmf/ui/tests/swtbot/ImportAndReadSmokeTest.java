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
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.ui.tests.swtbot;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
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
import org.eclipse.linuxtools.tmf.ui.project.model.TmfProjectRegistry;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.linuxtools.tmf.ui.project.wizards.importtrace.BatchImportTraceWizard;
import org.eclipse.linuxtools.tmf.ui.tests.swtbot.conditions.ConditionHelpers;
import org.eclipse.linuxtools.tmf.ui.views.TracingPerspectiveFactory;
import org.eclipse.linuxtools.tmf.ui.views.histogram.HistogramView;
import org.eclipse.linuxtools.tmf.ui.views.statistics.TmfStatisticsView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.views.properties.PropertySheet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * SWTBot Smoke test. base for other tests
 *
 * @author Matthew Khouzam
 */
public class ImportAndReadSmokeTest {

    //---------------------------------------------
    // Constants
    //---------------------------------------------

    private static final String TRACING_PERSPECTIVE_ID = TracingPerspectiveFactory.ID;
    private static final String JOB_FAMILY = "jobs.smoketest";
    private static final String TRACE_PROJECT_NAME = "test";

    //---------------------------------------------
    // Attributes
    //---------------------------------------------

    private static SWTWorkbenchBot bot;
    private static final Wizard[] wizard = new Wizard[1];
    private static CtfTmfTestTrace ctt = CtfTmfTestTrace.SYNTHETIC_TRACE;
    private static final String TRACE_NAME = "synthetic-trace";
    private ITmfEvent fDesired1;
    private ITmfEvent fDesired2;

    /** The Log4j logger instance. */
    private static final Logger logger = Logger.getRootLogger();

    //---------------------------------------------
    // Helpers for Junit
    //---------------------------------------------
    /** Test Class setup */
    @BeforeClass
    public static void init() {
        /*
         * set up for swtbot
         */
        SWTBotPreferences.TIMEOUT = 50000; /* 50 second timeout */
        Appender nullAppender = new NullAppender();
        logger.addAppender(nullAppender);
        closeJobs();
        bot = new SWTWorkbenchBot();
        final List<SWTBotView> openViews = bot.views();
        for (SWTBotView view : openViews) {
            if (view.getTitle().equals("Welcome")) {
                view.close();
                bot.waitUntil(ConditionHelpers.ViewIsClosed(view));
            }
        }

        /*
         * Switch perspectives
         */
        switchTracingPerspective();
        /*
         * finish waiting for eclipse to load
         */
        waitForJobs();

        /*
         * Make a new test
         */
        TmfProjectRegistry.createProject(TRACE_PROJECT_NAME, null, new NullProgressMonitor());

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        assertNotNull(window);
        // Get the selection
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        final SWTBotView projectViewBot = bot.viewById(IPageLayout.ID_PROJECT_EXPLORER);
        projectViewBot.setFocus();
        waitForJobs();
        IWorkbenchPart part = page.getActivePart();

        final Composite widget = (Composite) projectViewBot.getWidget();
        final Composite control = (Composite) widget.getChildren()[0];
        Tree tree = (Tree) control.getChildren()[0];
        assertNotNull(part);
        SWTBotTree treeBot = new SWTBotTree(tree);
        final SWTBotTreeItem treeItem = treeBot.getTreeItem(TRACE_PROJECT_NAME);
        if (treeItem != null) {
            try {
                treeItem.select();
                Job j = new Job("Delete") {

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            monitor.beginTask("Delete", 10);
                            String shellText = "Delete Resources";
                            bot.waitUntil(Conditions.shellIsActive(shellText));
                            final SWTBotShell shell = bot.shell(shellText);

                            // find modal window
                            monitor.worked(5);
                            bot = new SWTWorkbenchBot();
                            final Button[] desiredWidget = new Button[1];

                            SWTBotButton[] but = new SWTBotButton[2];
                            but [0] = bot.button("OK");
                            but[1] = bot.button(2);
                            Display.getDefault().syncExec(new Runnable() {

                                @Override
                                public void run() {
                                    // this should find the OK button. Hard
                                    // coded
                                    desiredWidget[0] = (Button) ((Composite) ((Composite) ((Composite) shell.widget.getShell().getChildren()[0]).getChildren()[2]).getChildren()[0]).getChildren()[2];
                                }
                            });
                            final SWTBotButton okButton = new SWTBotButton(desiredWidget[0]);
                            bot.waitUntil(Conditions.widgetIsEnabled(okButton));
                            okButton.click();
                            monitor.done();
                        } catch (TimeoutException e) {
                            fail(e.toString());
                        }

                        return Status.OK_STATUS;

                    }
                };
                j.schedule();
                final SWTBotMenu contextMenu = treeItem.contextMenu("Delete");
                contextMenu.click();
                j.join();
            } catch (InterruptedException e) {
                fail(e.toString());
            }

        }
        waitForJobs();
    }

    /** Finalize, close extra jobs */
    @AfterClass
    public static void finish() {
        closeJobs();
    }

    private static void closeJobs() {
        // LogManager.shutdown();
        Job.getJobManager().cancel(JOB_FAMILY);
        try {
            Job.getJobManager().join(JOB_FAMILY, new NullProgressMonitor());
        } catch (OperationCanceledException e) {
            fail(e.getMessage());
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    private static void switchTracingPerspective() {
        {
            try {
                PlatformUI.getWorkbench().showPerspective(TRACING_PERSPECTIVE_ID,
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow());
            } catch (WorkbenchException e) {
                fail(e.getMessage());
            }
        }
    }

    private static void focusMainWindow() {
        for (SWTBotShell shellBot : bot.shells()) {
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
        refreshUI(bot.getDisplay());
    }

    /**
     * Sleeps current thread or GUI thread for a given time.
     *
     * @param waitTimeMillis
     *            time in milliseconds to wait
     */
    protected static void delay(final long waitTimeMillis) {
        final Display display = bot.getDisplay();
        if (display != null) {
            final long endTimeMillis = System.currentTimeMillis() + waitTimeMillis;
            while (System.currentTimeMillis() < endTimeMillis) {
                refreshUI(display);
                display.update();
            }
        } else {
            try {
                Thread.sleep(waitTimeMillis);
            } catch (final InterruptedException e) {
                // Ignored
            }
        }
    }


    //---------------------------------------------
    // Test case(s)
    //---------------------------------------------

    /**
     * Main test case
     *
     * @throws InterruptedException
     *             exception
     */
    @Test
    public void test() throws InterruptedException {
        bot = new SWTWorkbenchBot();
        createProject();
        // reset
        bot = new SWTWorkbenchBot();
        final SWTBotTree tree[] = new SWTBotTree[1];
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                final SWTBotView projectExplorerBot = bot.viewById(IPageLayout.ID_PROJECT_EXPLORER);
                Widget w = projectExplorerBot.getWidget();
                tree[0] = new SWTBotTree((Tree) ((Composite) ((Composite) w).getChildren()[0]).getChildren()[0]);
            }
        });

        final SWTBotTreeItem treeItem = tree[0].getTreeItem(TRACE_PROJECT_NAME);
        if (treeItem.isExpanded()) {
            treeItem.collapse();
        }
        final Display display = Display.getDefault();

        refreshUI(display);

        treeItem.select();
        treeItem.click();
        treeItem.expand();

        treeItem.getNode("Traces [0]").select();
        waitForJobs();

        Job job = new Job("Batch Import testing") {
            /* treeControl to be used in jobs */
            final Tree[] treeControl = new Tree[1];
            final String traceTypeName = "Generic CTF Trace";

            @Override
            public boolean belongsTo(Object family) {
                return JOB_FAMILY.equals(family);
            }

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    initModalPage(monitor);
                    selecTraceType(monitor);
                    addDirectoryPage(monitor);
                    selectTrace(monitor);
                    targetPage(monitor);
                    finishBitw();
                    monitor.done();
                } catch (TimeoutException e) {
                    fail(e.toString());
                }
                return Status.OK_STATUS;
            }

            private void initModalPage(IProgressMonitor monitor) {
                monitor.beginTask("BITW testing", 100);
                bot.waitUntil(ConditionHelpers.isWizardReady(wizard));
            }

            private void targetPage(IProgressMonitor monitor) {
                monitor.worked(2);
                monitor.subTask("Target");
            }

            private void selecTraceType(IProgressMonitor monitor) {
                bot = new SWTWorkbenchBot();

                monitor.subTask("TraceType");
                bot.waitUntil(ConditionHelpers.isWizardReady(wizard));
                bot = new SWTWorkbenchBot();
                monitor.worked(10);
                final IWizardPage selectTraceType = wizard[0].getPages()[0];
                treeControl[0] = null;
                Display.getDefault().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        for (Control c : ((Composite) selectTraceType.getControl().getParent().getChildren()[0]).getChildren()) {
                            if (c instanceof Tree) {
                                treeControl[0] = (Tree) c;
                            }
                        }
                    }
                });
                assertNotNull(treeControl[0]);
                SWTBotTree treeBot = new SWTBotTree(treeControl[0]);
                final String ctfId = "Common Trace Format";
                final String lttngId = traceTypeName;
                monitor.worked(1);
                bot.waitUntil(ConditionHelpers.IsTreeNodeAvailable(ctfId, treeBot));
                monitor.worked(1);
                bot.waitUntil(ConditionHelpers.IsTreeChildNodeAvailable(lttngId, treeBot.getTreeItem(ctfId)));
                while (!treeBot.getTreeItem(ctfId).getNode(lttngId).isChecked()) {
                    treeBot.getTreeItem(ctfId).getNode(lttngId).check();
                }
                clickNextBitw();
            }

            private void addDirectoryPage(IProgressMonitor monitor) {
                bot = new SWTWorkbenchBot();
                monitor.subTask("Directory");
                Display.getDefault().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        ((BatchImportTraceWizard) wizard[0]).addFileToScan(ctt.getPath());
                    }
                });
                final SWTBotButton removeButton = bot.button("Remove");
                bot.waitUntil(Conditions.widgetIsEnabled(removeButton));
                removeButton.click();
                bot.waitUntil(Conditions.tableHasRows(bot.table(), 1));
                monitor.worked(10);

                clickNextBitw();
            }

            private void selectTrace(IProgressMonitor monitor) {
                monitor.subTask("Select Trace Type");
                bot = new SWTWorkbenchBot();
                SWTBotTree treeBot;
                treeBot = bot.tree();
                bot.waitUntil(Conditions.widgetIsEnabled(treeBot));
                final SWTBotTreeItem genericCtfTreeItem = treeBot.getTreeItem(traceTypeName);
                bot.waitUntil(Conditions.widgetIsEnabled(genericCtfTreeItem));
                genericCtfTreeItem.expand();
                genericCtfTreeItem.check();
                monitor.worked(10);
                clickNextBitw();
            }

            private void finishBitw() {
                bot = new SWTWorkbenchBot();
                SWTBotShell shell = bot.activeShell();
                final SWTBotButton finishButton = bot.button("Finish");
                finishButton.click();
                bot.waitUntil(Conditions.shellCloses(shell));

            }

            private void clickNextBitw() {
                bot = new SWTWorkbenchBot();
                IWizardPage currentPage = wizard[0].getContainer().getCurrentPage();
                IWizardPage desiredPage = wizard[0].getNextPage(currentPage);
                SWTBotButton nextButton = bot.button("Next >");
                nextButton.click();
                bot.waitUntil(ConditionHelpers.isWizardOnPage(wizard[0], desiredPage));
            }

        };
        job.schedule();
        openBITW();
        job.join();
        waitForJobs();

        List<String> nodes = treeItem.getNodes();
        String nodeName = "";
        for (String node : nodes) {
            if (node.startsWith("Traces")) {
                nodeName = node;
            }
        }
        bot.waitUntil(ConditionHelpers.IsTreeChildNodeAvailable(nodeName, treeItem));
        treeItem.getNode(nodeName).expand();
        bot.waitUntil(ConditionHelpers.IsTreeChildNodeAvailable(TRACE_NAME, treeItem.getNode(nodeName)));
        treeItem.getNode(nodeName).getNode(TRACE_NAME).select();
        treeItem.getNode(nodeName).getNode(TRACE_NAME).doubleClick();
        display.sleep();
        Thread.sleep(1000);
        waitForJobs();
        IEditorReference[] ieds = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
        assertNotNull(ieds);
        IEditorPart iep = null;
        for (IEditorReference ied : ieds) {
            if (ied.getTitle().equals(TRACE_NAME)) {
                iep = ied.getEditor(true);
                break;
            }
        }
        assertNotNull(iep);
        fDesired1 = getEvent(100);
        fDesired2 = getEvent(10000);
        TmfEventsEditor tmfEd = (TmfEventsEditor) iep;

        tmfEd.setFocus();
        tmfEd.selectionChanged(new SelectionChangedEvent(tmfEd, new StructuredSelection(fDesired1)));

        waitForJobs();
        Thread.sleep(1000);
        assertNotNull(tmfEd);
        IViewReference[] viewRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
        for (IViewReference viewRef : viewRefs) {
            IViewPart vp = viewRef.getView(true);
            if (vp.getTitle().equals("Histogram")) {
                testHV(vp);
            } else if (vp.getTitle().equals("Properties")) {
                testPV(vp);
            } else if (vp.getTitle().equals("Statistics")) {
                testSV(vp);
            }
        }
    }

    //---------------------------------------------
    // Helpers for testing views
    //---------------------------------------------

    private static void testPV(IViewPart vp) {
        PropertySheet pv = (PropertySheet) vp;
        assertNotNull(pv);
    }

    private void testHV(IViewPart vp) {
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

    private static void testSV(IViewPart vp) {
        TmfStatisticsView sv = (TmfStatisticsView) vp;
        assertNotNull(sv);
    }

    //---------------------------------------------
    // Trace helpers
    //---------------------------------------------

    private static CtfTmfEvent getEvent(int rank) {
        CtfTmfTrace trace = ctt.getTrace();
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

    private static void refreshUI(final Display display) {
        if (!display.readAndDispatch()) {
            display.sleep();
        }
    }

    private static void createProject() throws InterruptedException {
        bot = new SWTWorkbenchBot();
        Job j = new Job("Create Project") {

            @Override
            public boolean belongsTo(Object family) {
                return JOB_FAMILY.equals(family);
            }

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    bot = new SWTWorkbenchBot();
                    bot.waitUntil(Conditions.shellIsActive("New Project"));
                    newProject(monitor);
                    setProjectName(monitor);
                    monitor.done();
                } catch (TimeoutException e) {
                    fail(e.toString());
                }
                return Status.OK_STATUS;
            }

            private void newProject(IProgressMonitor monitor) {
                Thread.currentThread().setName("NewProject");
                bot = new SWTWorkbenchBot();
                final SWTBotTree[] tree = new SWTBotTree[1];
                Display.getDefault().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        SWTBotShell botShell = bot.activeShell();
                        Control[] controls = botShell.widget.getChildren();
                        Composite composite = (Composite) controls[0];
                        composite = (Composite) composite.getChildren()[0];
                        composite = (Composite) composite.getChildren()[0];
                        composite = (Composite) composite.getChildren()[1];
                        composite = (Composite) composite.getChildren()[0];
                        composite = (Composite) composite.getChildren()[1];
                        composite = (Composite) composite.getChildren()[0];
                        composite = (Composite) composite.getChildren()[0];
                        composite = (Composite) composite.getChildren()[1];
                        final Tree tree2 = (Tree) composite.getChildren()[0];
                        tree[0] = new SWTBotTree(tree2);
                    }
                });

                assertNotNull(tree[0]);
                final String tracingKey = "Tracing";
                bot.waitUntil(ConditionHelpers.IsTreeNodeAvailable(tracingKey, tree[0]));
                final SWTBotTreeItem tracingNode = tree[0].expandNode(tracingKey);

                keyEvent(tree[0], SWT.PAGE_DOWN);
                tracingNode.select();
                monitor.worked(20);
                final String projectKey = "Tracing Project";
                bot.waitUntil(ConditionHelpers.IsTreeChildNodeAvailable(projectKey, tracingNode));
                final SWTBotTreeItem tracingProject = tracingNode.getNode(projectKey);
                assertNotNull(tracingProject);

                tracingProject.select();
                tracingProject.click();

                monitor.worked(40);
                clickNext();
            }

            private void keyEvent(final SWTBotTree tree, int keyStroke) {
                Thread.currentThread().setName("KeyEvent");
                Event e = new Event();
                e.type = SWT.KeyDown;
                e.time = (int) System.currentTimeMillis();
                e.widget = tree.widget;
                e.display = tree.display;
                e.keyCode = keyStroke;
                final Event keyDown = e;
                e.display.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        keyDown.widget.notifyListeners(keyDown.type, keyDown);
                    }
                });
                e.type = SWT.KeyUp;
                e.time = (int) System.currentTimeMillis();
                final Event keyUp = e;
                e.display.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        keyUp.widget.notifyListeners(keyUp.type, keyUp);
                    }
                });
            }

            private void setProjectName(IProgressMonitor monitor) {
                Thread.currentThread().setName("SetProjectName");
                bot = new SWTWorkbenchBot();
                final SWTBotText text = bot.text();
                text.setText(TRACE_PROJECT_NAME);

                monitor.worked(80);
                bot.button("Finish").click();
            }

            private void clickNext() {
                bot = new SWTWorkbenchBot();
                SWTBotButton nextButton = bot.button("Next >");
                bot.waitUntil(Conditions.widgetIsEnabled(nextButton));
                nextButton.click();
                bot.waitUntil(Conditions.shellIsActive("Tracing Project"));
            }
        };
        j.schedule();
        focusMainWindow();
        bot.menu("File").menu("New").menu("Project...").click();
        j.join();

    }

    private static void openBITW() {
        TmfTraceFolder traceFolder = getTraceFolder();
        if (traceFolder == null) {
            wizard[0] = null;
            return;
        }
        final IWorkbench workbench = PlatformUI.getWorkbench();
        // Fire the Import Trace Wizard
        if (workbench != null) {
            final IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
            if (activeWorkbenchWindow != null) {
                Shell shell = activeWorkbenchWindow.getShell();

                wizard[0] = new BatchImportTraceWizard();
                ((BatchImportTraceWizard) wizard[0]).init(PlatformUI.getWorkbench(), new StructuredSelection(traceFolder));
                WizardDialog dialog = new WizardDialog(shell, wizard[0]);
                dialog.open();

                traceFolder.refresh();
            }
        }
    }

    private static TmfTraceFolder getTraceFolder() {
        // Check if we are closing down
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }

        // Get the selection
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IWorkbenchPart part = page.getActivePart();
        if (part == null) {
            return null;
        }
        ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
        if (selectionProvider == null) {
            return null;
        }
        ISelection selection = selectionProvider.getSelection();

        TmfTraceFolder traceFolder = null;
        if (selection instanceof TreeSelection) {
            TreeSelection sel = (TreeSelection) selection;
            // There should be only one item selected as per the plugin.xml
            Object element = sel.getFirstElement();
            if (element instanceof TmfTraceFolder) {
                traceFolder = (TmfTraceFolder) element;
            }
        }
        return traceFolder;
    }
}
