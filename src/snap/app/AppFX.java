/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.app;
import com.apple.eawt.*;
import com.apple.eawt.AppEvent.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import snap.javafx.JFXViewEnv;
import snap.util.*;
import snap.viewx.ExceptionReporter;

/**
 * SnapCode Application entry point.
 */
public class AppFX extends Application {

/**
 * Main method to run panel.
 */
public static void main(String args[])
{
    // Hook to run as SnapApp instead
    if(args.length>0 && args[0].equals("-snap")) { snap.javafx.SnapApp.main(args); return; }
    
    // Install JavaFX
    JFXViewEnv.set();
    
    // Mac specific stuff
    if(SnapUtils.isMac) new AppleAppHandler().init();

    // Config/init JavaFX and invoke real main on event thread
    Application.launch(AppFX.class,args); //Platform.setImplicitExit(false);new JFXPanel();Platform.runLater(()->start());
}

/**
 * Main method to run panel.
 */
public void start(Stage aStage)
{
    // Set App Prefs class
    PrefsUtils.setPrefsClass(App.class);
    
    // Install Exception reporter
    ExceptionReporter er = new ExceptionReporter();
    er.setURL("http://www.reportmill.com/cgi-bin/cgiemail/email/snap-exception.txt");
    er.setInfo("SnapCode Version 1, Build Date: " + SnapUtils.getBuildInfo());
    Thread.setDefaultUncaughtExceptionHandler(er);

    // Show open data source panel
    WelcomePanel.getShared().setOnQuit(() -> quitApp());
    WelcomePanel.getShared().showPanel();
}

/**
 * Exits the application.
 */
public static void quitApp()  { Platform.runLater(() -> quitAppImpl()); }

/**
 * Exits the application (real version).
 */
private static void quitAppImpl()
{
    if(AppPane.getOpenAppPane()!=null) AppPane.getOpenAppPane().hide();
    PrefsUtils.flush();
    System.exit(0);
}

/**
 * A class to handle apple events.
 */
private static class AppleAppHandler implements PreferencesHandler, QuitHandler {

    /** Initializes Apple Application handling. */
    public void init()
    {
        //System.setProperty("apple.laf.useScreenMenuBar", "true"); // 1.4
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "SnapCode");
        com.apple.eawt.Application app = com.apple.eawt.Application.getApplication();
        app.setPreferencesHandler(this); app.setQuitHandler(this);
        _appHand = this;
    }

    /** Handle Preferences. */
    public void handlePreferences(PreferencesEvent arg0)
    {
        AppPane appPane = AppPane.getOpenAppPane(); if(appPane==null) return;
        appPane.getBrowser().setFile(appPane.getRootSite().getRootDir());
    }

    /** Handle QuitRequest. */
    public void handleQuitRequestWith(QuitEvent arg0, QuitResponse arg1)  { AppFX.quitApp(); }
} static AppleAppHandler _appHand;

}