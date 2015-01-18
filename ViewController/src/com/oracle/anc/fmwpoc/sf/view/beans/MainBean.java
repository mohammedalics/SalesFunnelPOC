package com.oracle.anc.fmwpoc.sf.view.beans;

import javax.faces.event.ActionEvent;

import oracle.ui.pattern.dynamicShell.TabContext;

public class MainBean {
    public MainBean() {
    }

    public void launchLeadCreate(ActionEvent actionEvent) {
        _launchActivity("New Lead", "/WEB-INF/lead-create-btf.xml", true);
}

    private void _launchActivity(String title, String taskflowId,
                                 boolean newTab) {
        try {
            if (newTab) {
                TabContext.getCurrentInstance().addTab(title, taskflowId);
            } else {
                TabContext.getCurrentInstance().addOrSelectTab(title,
                                                               taskflowId);
            }
        } catch (TabContext.TabOverflowException toe) {
            // causes a dialog to be displayed to the user saying that there are
            // too many tabs open - the new tab will not be opened...
            toe.handleDefault();
        }

    }

    public void launchMyDashboard(ActionEvent actionEvent) {
        _launchActivity("My Dashboard", "/WEB-INF/emp-dashboard-btf.xml", false);    }
}
