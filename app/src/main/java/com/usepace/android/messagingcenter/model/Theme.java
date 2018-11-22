package com.usepace.android.messagingcenter.model;

public class Theme {

    private String toolbar_title = null;


    //Theme Constructor
    public Theme(String toolbar_title) {
        this.toolbar_title = toolbar_title;
    }

    /**
     *
     * @return
     */
    public String getToolbarTitle() {
        return toolbar_title;
    }
}
