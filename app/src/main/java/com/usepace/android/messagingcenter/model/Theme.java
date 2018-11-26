package com.usepace.android.messagingcenter.model;

public class Theme {

    private String toolbar_title = null;
    private String toolbar_subtitle = null;
    private String welcome_message = null;

    //Empty  Constructor
    public Theme() {
    }

    //Theme Constructor
    public Theme(String toolbar_title, String toolbar_subtitle, String welcome_message) {
        this.toolbar_title = toolbar_title;
        this.toolbar_subtitle = toolbar_subtitle;
        this.welcome_message = welcome_message;
    }

    /**
     **/
    public void setToolbarTitle(String toolbar_title) {
        this.toolbar_title = toolbar_title;
    }
    public String getToolbarTitle() {
        return toolbar_title;
    }


    /**
     **/
    public void setToolbarSubtitle(String toolbar_subtitle) {
        this.toolbar_subtitle = toolbar_subtitle;
    }
    public String getToolbarSubtitle() {
        return toolbar_subtitle;
    }

    /**
     **/
    public void setWelcomeMessage(String welcome_message) {
        this.welcome_message = welcome_message;
    }
    public String getWelcomeMessage() {
        return welcome_message;
    }
}
