/**********************************************************************
 * Copyright (c) 2012, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.linuxtools.internal.lttng2.control.stubs.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.linuxtools.internal.lttng2.control.ui.views.dialogs.IEnableEventsDialog;
import org.eclipse.linuxtools.internal.lttng2.control.ui.views.model.impl.TraceDomainComponent;
import org.eclipse.linuxtools.internal.lttng2.control.ui.views.model.impl.TraceProviderGroup;

/**
 * Enable events dialog stub implementation.
 */
@SuppressWarnings("javadoc")
public class EnableEventsDialogStub implements IEnableEventsDialog {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    private boolean fIsKernel;
    private boolean fIsTracePoints;
    private boolean fIsAllTracePoints;
    private boolean fIsSysCalls;
    private boolean fIsDynamicProbe;
    private String fProbeEventName;
    private String fDynamicProbe;
    private boolean fIsFunctionProbe;
    private String fFunctionEventName;
    private String fFunctionProbe;
    private List<String> fNames = new ArrayList<>();

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------
    public void setIsKernel(boolean isKernel) {
        fIsKernel = isKernel;
    }

    public void setIsTracePoints(boolean isTracePoints) {
        fIsTracePoints = isTracePoints;
    }

    public void setIsAllTracePoints(boolean isAllTracePoints) {
        fIsAllTracePoints = isAllTracePoints;
    }

    public void setIsSysCalls(boolean isSysCalls) {
        this.fIsSysCalls = isSysCalls;
    }

    public void setIsDynamicProbe(boolean isDynamicProbe) {
        fIsDynamicProbe = isDynamicProbe;
    }

    public void setProbeEventName(String probeEventName) {
        fProbeEventName = probeEventName;
    }

    public void setDynamicProbe(String dynamicProbe) {
        fDynamicProbe = dynamicProbe;
    }

    public void setIsFunctionProbe(boolean isFunctionProbe) {
        fIsFunctionProbe = isFunctionProbe;
    }

    public void setFunctionEventName(String functionEventName) {
        fFunctionEventName = functionEventName;
    }

    public void setFunctionProbe(String functionProbe) {
        fFunctionProbe = functionProbe;
    }

    public void setNames(List<String> names) {
        fNames = names;
    }

    @Override
    public boolean isTracepoints() {
        return fIsTracePoints;
    }

    @Override
    public boolean isAllTracePoints() {
        return fIsAllTracePoints;
    }

    @Override
    public boolean isSysCalls() {
        return fIsSysCalls;
    }

    @Override
    public boolean isAllSysCalls() {
        return fIsSysCalls;
    }

    @Override
    public List<String> getEventNames() {
        return fNames;
    }

    @Override
    public boolean isDynamicProbe() {
        return fIsDynamicProbe;
    }

    @Override
    public String getProbeEventName() {
        return fProbeEventName;
    }

    @Override
    public String getProbeName() {
        return fDynamicProbe;
    }

    @Override
    public boolean isDynamicFunctionProbe() {
        return fIsFunctionProbe;
    }

    @Override
    public String getFunctionEventName() {
        return fFunctionEventName;
    }

    @Override
    public String getFunction() {
        return fFunctionProbe;
    }

    @Override
    public boolean isKernel() {
        return fIsKernel;
    }

    @Override
    public void setTraceProviderGroup(TraceProviderGroup providerGroup) {
    }

    @Override
    public void setTraceDomainComponent(TraceDomainComponent domain) {
    }

    @Override
    public int open() {
        return 0;
    }

}