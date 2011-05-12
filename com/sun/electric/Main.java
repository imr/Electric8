/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Main.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric;

import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.AbstractUserInterface;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.UserInterfaceInitial;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.MessagesStream;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.menus.EMenuBar;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.ui.MessagesWindow;
import com.sun.electric.tool.user.ui.StatusBar;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JApplet;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * This class initializes Electric and starts the system. How to run Electric:
 * <P>
 * <P> <CODE>java -jar electric.jar [electric-options]</CODE> without plugins
 * <P> <CODE>java -classpath electric.jar<i>delim</i>{list of plugins} com.sun.electric.Launcher [electric-options]</CODE>
 * otherwise, where <i>delim</i> is OS-dependant separator
 * <P> And Electric options are:
 * <P> <CODE>         -mdi: multiple document interface mode </CODE>
 * <P> <CODE>         -sdi: single document interface mode </CODE>
 * <P> <CODE>         -NOMINMEM: ignore minimum memory provided for JVM </CODE>
 * <P> <CODE>         -s script name: bean shell script to execute </CODE>
 * <P> <CODE>         -version: version information </CODE>
 * <P> <CODE>         -v: brief version information </CODE>
 * <P> <CODE>         -debug: debug mode. Extra information is available </CODE>
 * <P> <CODE>         -server: dump strace of snapshots</CODE>
 * <P> <CODE>         -help: this message </CODE>
 * <P> <P>
 * See manual for more instructions.
 */
public class Main extends JApplet
{
	private static final long serialVersionUID = 1L;
    /** True if in MDI mode, otherwise SDI. */				private static UserInterfaceMain.Mode mode;
    														private static Main toplevel = null;
	/** The desktop pane (if MDI). */						public static JDesktopPane desktop = null;
	/** The only status bar (if MDI). */					private static StatusBar sb = null;
	/** The size of the screen. */							private static Dimension scrnSize;
	/** The size of the applet in the web browser. */		private static Dimension appSize;
	/** The messagesWindow window. */						private static MessagesWindow messagesWindow;
    /** The rate of double-clicks. */						private static int doubleClickDelay;
	/** The cursor being displayed. */						private static Cursor cursor;
    /** If the busy cursor is overriding the normal cursor */ private static boolean busyCursorOn = false;

    /** The menu bar */                                     public static EMenuBar.Instance menuBar;
    /** The tool bar */                                     public static ToolBar toolBar;

    /** true to resize initial MDI window forces redraw) */	private static final boolean MDIINITIALRESIZE = true;

	/**
     * Mode of Job manager
     */
    private static enum Mode {
        /** Thread-safe full screen run. */                                    FULL_SCREEN_SAFE,
        /** JonG: "I think batch mode implies 'no GUI', and nothing more." */  BATCH,
        /** Server side. */                                                    SERVER,
        /** Client side. */                                                    CLIENT;
    }

    private static final Mode DEFAULT_MODE = Mode.FULL_SCREEN_SAFE;

    private static Mode runMode;

    public static void main(String[] args) {
    	JFrame appframe = new JFrame("Electric");
    	Main applet = new Main();
    	
    	appframe.getContentPane().add(applet, BorderLayout.CENTER);
    	
    	applet.init();
    	applet.start();
    	
    	appframe.setSize(1024,800);
    	appframe.setVisible(true);
    }
    
	/**
	 * The main entry point of Electric.
	 * @param args the arguments to the program.
	 */
	public void init()
	{
		String param;
        List<String> argsList = null;
    /**
		// -v (short version)
		if (hasCommandLineOption(argsList, "-v"))
		{
			System.out.println(Version.getVersion());
			System.exit(0);
		}

		// -version
		if (hasCommandLineOption(argsList, "-version"))
		{
			System.out.println(Version.getApplicationInformation());
			System.out.println("\t"+Version.getVersionInformation());
			System.out.println("\t"+Version.getCopyrightInformation());
			System.out.println("\t"+Version.getWarrantyInformation());
			System.exit(0);
		}

        // -help
        if (hasCommandLineOption(argsList, "-help"))
		{
	        System.out.println("Usage (without plugins):");
	        System.out.println("\tjava -jar electric.jar [electric-options]");
	        System.out.println("Usage (with plugins):");
	        System.out.println("\tjava -classpath electric.jar<delim>{list of plugins} com.sun.electric.Launcher [electric-options]");
	        System.out.println("\t\twhere <delim> is OS-dependant separator (colon or semicolon)");
	        System.out.println("\nOptions:");
            System.out.println("\t-mdi: multiple document interface mode");
	        System.out.println("\t-sdi: single document interface mode");
	        System.out.println("\t-NOMINMEM: ignore minimum memory provided for JVM");
	        System.out.println("\t-s <script name>: bean shell script to execute");
	        System.out.println("\t-version: version information");
	        System.out.println("\t-v: brief version information");
	        System.out.println("\t-debug: debug mode. Extra information is available");
            System.out.println("\t-threads <numThreads>: recommended size of thread pool for Job execution.");
            System.out.println("\t-logging <filePath>: log server events in a binary file");
            System.out.println("\t-socket <socket>: socket port for client/server interaction");
	        System.out.println("\t-batch: batch mode implies 'no GUI', and nothing more");
            System.out.println("\t-server: dump trace of snapshots");
            System.out.println("\t-client <machine name>: replay trace of snapshots");
	        System.out.println("\t-help: this message");

			System.exit(0);
		}
	*/

        // set applet size
        appSize = getSize();
        Toolkit tk = Toolkit.getDefaultToolkit();
        scrnSize = tk.getScreenSize();
        
        runMode = DEFAULT_MODE;
        String pipeOptions = "";
        
		String numThreadsString = null;
        int numThreads = 0 ;
        if (numThreadsString != null) {
            numThreads = TextUtils.atoi(numThreadsString);
            if (numThreads > 0)
                pipeOptions += " -threads " + numThreads;
            else
                System.out.println("Invalid option -threads " + numThreadsString);
        }
        String loggingFilePath = null;
        if (loggingFilePath != null) {
            pipeOptions += " -logging " + loggingFilePath;
        }
        String socketString = null;
        int socketPort = 0;
        if (socketString != null) {
            socketPort = TextUtils.atoi(socketString);
            if (socketPort > 0)
                pipeOptions += " -socket " + socketPort;
            else
                System.out.println("Invalid option -socket " + socketString);
        }

        ActivityLogger.initialize("electric", true, true, true/*false*/, User.isEnableLog(), User.isMultipleLog());

        Client.OS os = Client.getOperatingSystem();
		try{
            switch (os)
            {
                case WINDOWS:
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                    break;
                case UNIX:
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                    break;
                case MACINTOSH:
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.MacLookAndFeel");
                    break;
			}
		} catch(Exception e) {}
		
		cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

		addComponentListener(new ReshapeComponentAdapter());
		
		// For 3D: LightWeight v/s heavy: mixing awt and swing
		try {
			javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);
			javax.swing.ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
			enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
        
        desktop = new JDesktopPane();
        desktop.setVisible(true);
        getContentPane().add(desktop);
        
        AbstractUserInterface ui;
        //switch ()
        //{
        //	case :
		//}
        //if (runMode == Mode.FULL_SCREEN_SAFE || runMode == Mode.CLIENT)
        	//ui = new AppletUserInterface();
            ui = new UserInterfaceMain(argsList, mode, this);
        //else
        //    ui = new UserInterfaceDummy();
        MessagesStream.getMessagesStream();

		// initialize database
        TextDescriptor.cacheSize();
        Tool.initAllTools();
        Pref.lockCreation();
        EDatabase database = new EDatabase(IdManager.stdIdManager.getInitialSnapshot(), "clientDB");
        EDatabase.setClientDatabase(database);
        Job.setUserInterface(new UserInterfaceInitial(database));
        InitDatabase job = new InitDatabase(argsList);
        EDatabase.setServerDatabase(new EDatabase(IdManager.stdIdManager.getInitialSnapshot(), "serverDB"));
        EDatabase.setCheckExamine();
        Job.initJobManager(numThreads, loggingFilePath, socketPort, ui, job);
   	}
	
	public void start(){
		
	}
	public void stop(){
		
	}

	public static void InitializeMessagesWindow() {
        messagesWindow = new MessagesWindow(appSize);
	}
	
    public static class UserInterfaceDummy extends AbstractUserInterface
	{
        public static final PrintStream stdout = System.out;

        public UserInterfaceDummy() {
        }

        public void startProgressDialog(String type, String filePath) {}
        public void stopProgressDialog() {}
        public void setProgressValue(int pct) {}
        public void setProgressNote(String message) {}
        public String getProgressNote() { return null; }

    	public EDatabase getDatabase() {
            return EDatabase.clientDatabase();
        }
		public EditWindow_ getCurrentEditWindow_() { return null; }
		public EditWindow_ needCurrentEditWindow_()
		{
			System.out.println("Batch mode Electric has no needed windows");
			return null;
		}
        /** Get current cell from current library */
		public Cell getCurrentCell()
        {
            throw new IllegalStateException("Batch mode Electric has no current Cell");
        }

		public Cell needCurrentCell()
		{
            throw new IllegalStateException("Batch mode Electric has no current Cell");
		}
		public void repaintAllWindows() {}

        public void adjustReferencePoint(Cell cell, double cX, double cY) {};
		public int getDefaultTextSize() { return 14; }
//		public Highlighter getHighlighter();
		public EditWindow_ displayCell(Cell cell) { return null; }

        public void termLogging(final ErrorLogger logger, boolean explain, boolean terminate) {
            System.out.println(logger.getInfo());
        }

        /**
         * Method to return the error message associated with the current error.
         * Highlights associated graphics if "showhigh" is nonzero.
         */
        public String reportLog(ErrorLogger.MessageLog log, boolean showhigh, boolean separateWindow, int position)
        {
            // return the error message
            return log.getMessageString();
        }

        /**
         * Method to show an error message.
         * @param message the error message to show.
         * @param title the title of a dialog with the error message.
         */
        public void showErrorMessage(String message, String title)
        {
        	System.out.println(message);
        }

        /**
         * Method to show an error message.
         * @param message the error message to show.
         * @param title the title of a dialog with the error message.
         */
        public void showErrorMessage(String[] message, String title)
        {
        	System.out.println(message);
        }

        /**
         * Method to show an informational message.
         * @param message the message to show.
         * @param title the title of a dialog with the message.
         */
        public void showInformationMessage(String message, String title)
        {
        	System.out.println(message);
        }

        private PrintWriter printWriter = null;

        /**
         * Method print a message.
         * @param message the message to show.
         * @param newLine add new line after the message
         */
        public void printMessage(String message, boolean newLine) {
            if (newLine) {
                stdout.println(message);
                if (printWriter != null)
                    printWriter.println(message);
            } else {
                stdout.print(message);
                if (printWriter != null)
                    printWriter.print(message);
            }
        }

        /**
         * Method to start saving messages.
         * @param filePath file to save
         */
        public void saveMessages(final String filePath) {
            try
            {
                if (printWriter != null) {
                    printWriter.close();
                    printWriter = null;
                }
                if (filePath == null) return;
                printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
            } catch (IOException e)
            {
                System.err.println("Error creating " + filePath);
                System.out.println("Error creating " + filePath);
                return;
            }

            System.out.println("Messages will be saved to " + filePath);
        }

        /**
         * Method to show a message and ask for confirmation.
         * @param message the message to show.
         * @return true if "yes" was selected, false if "no" was selected.
         */
        public boolean confirmMessage(Object message) { return true; }

        /**
         * Method to ask for a choice among possibilities.
         * @param message the message to show.
         * @param title the title of the dialog with the query.
         * @param choices an array of choices to present, each in a button.
         * @param defaultChoice the default choice.
         * @return the index into the choices array that was selected.
         */
        public int askForChoice(String message, String title, String [] choices, String defaultChoice)
        {
        	System.out.println(message + " CHOOSING " + defaultChoice);
        	for(int i=0; i<choices.length; i++) if (choices[i].equals(defaultChoice)) return i;
        	return 0;
        }

        /**
         * Method to ask for a line of text.
         * @param message the prompt message.
         * @param title the title of a dialog with the message.
         * @param def the default response.
         * @return the string (null if cancelled).
         */
        public String askForInput(Object message, String title, String def) { return def; }

        @Override
        protected void terminateJob(Job.Key jobKey, String jobName, Tool tool,
            Job.Type jobType, byte[] serializedJob,
            boolean doItOk, byte[] serializedResult, Snapshot newSnapshot) {
            printMessage("Job " + jobKey, true);
            if (!jobType.isExamine()) {
                endChanging();
            }
        }

        @Override
        protected void showJobQueue(Job.Inform[] jobQueue) {
            printMessage("JobQueue: ", false);
            for (Job.Inform jobInfo: jobQueue)
                printMessage(" " + jobInfo, false);
            printMessage("", true);
        }

        @Override
        protected void addEvent(Client.ServerEvent serverEvent) {
            serverEvent.run();
        }
	}

	/**
	 * Class to init all technologies.
	 */
	private static class InitDatabase extends Job
	{
		private static final long serialVersionUID = 1L;
		private Map<String,Object> paramValuesByXmlPath = Technology.getParamValuesByXmlPath();
        private String softTechnologies = StartupPrefs.getSoftTechnologies();
		private List<String> argsList;
        private Library mainLib;

		private InitDatabase(List<String> argsList)
		{
			super("Init database", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.argsList = argsList;
		}

        @Override
		public boolean doIt() throws JobException
		{
            //System.out.println("InitDatabase");
            // initialize all of the technologies
            Technology.initAllTechnologies(getDatabase(), paramValuesByXmlPath, softTechnologies);

            // open no name library first
            Library clipLib = Library.newInstance(Clipboard.CLIPBOARD_LIBRAY_NAME, null);
            clipLib.setHidden();
            Cell clipCell = Cell.newInstance(clipLib, Clipboard.CLIPBOARD_CELL_NAME);
            assert clipCell.getId().cellIndex == Clipboard.CLIPBOARD_CELL_INDEX;
            clipCell.setTechnology(getTechPool().getGeneric());

            mainLib = Library.newInstance("noname", null);
            if (mainLib == null) return false;
            fieldVariableChanged("mainLib");
            mainLib.clearChanged();
            return true;
		}

        @Override
        public void terminateOK() {
            new InitProjectSettings(argsList).startJobOnMyResult();
            User.setCurrentLibrary(mainLib);
        }

        @Override
        public void terminateFail(Throwable jobException) {
            System.out.println("Initialization failed");
            System.exit(1);
        }
	}

	/**
	 * Class to init project preferences.
	 */
	private static class InitProjectSettings extends Job
	{
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Setting.SettingChangeBatch changeBatch = new Setting.SettingChangeBatch();
		private InitProjectSettings(List<String> argsList)
		{
			super("Init project preferences", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            Preferences prefRoot = Pref.getPrefRoot();
            for (Map.Entry<Setting,Object> e: getDatabase().getSettings().entrySet()) {
                Setting setting = e.getKey();
                Object value = setting.getValueFromPreferences(prefRoot);
                if (value.equals(e.getValue())) continue;
                changeBatch.add(setting, value);
            }
		}

        @Override
		public boolean doIt() throws JobException
		{
            getDatabase().implementSettingChanges(changeBatch);
            return true;
		}

        @Override
        public void terminateOK() {
            Job.getExtendedUserInterface().finishInitialization();
        }
	}

    /**
	 * Method to return the amount of memory being used by Electric.
	 * Calls garbage collection and delays to allow completion, so the method is SLOW.
	 * @return the number of bytes being used by Electric.
	 */
	public static long getMemoryUsage()
	{
		collectGarbage();
		collectGarbage();
		long totalMemory = Runtime.getRuntime().totalMemory();

		collectGarbage();
		collectGarbage();
		long freeMemory = Runtime.getRuntime().freeMemory();

		return (totalMemory - freeMemory);
	}

	private static void collectGarbage()
	{
		try
		{
			System.gc();
			Thread.sleep(100);
			System.runFinalization();
			Thread.sleep(100);
		} catch (InterruptedException ex)
		{
			ex.printStackTrace();
		}
	}

    /**
     * Method to return a String that gives the path to the Electric JAR file.
     * If the path has spaces in it, it is quoted.
     * @return the path to the Electric JAR file.
     */
    public static String getJarLocation()
    {
		String jarPath = System.getProperty("java.class.path", ".");
		if (jarPath.indexOf(' ') >= 0) jarPath = "\"" + jarPath + "\"";
		return jarPath;
    }
    
    public Dimension getScrnSize()
    {
    	return scrnSize;
    }
    
    public Dimension getAppSize()
    {
    	return appSize;
    }
    
	public static MessagesWindow getMessagesWindow() { return messagesWindow; }

    /**
	 * Method to return status bar associated with this TopLevel.
	 * @return the status bar associated with this TopLevel.
	 */
	public static StatusBar getStatusBar() { return sb; }

    /**
     * Get the tool bar associated with this TopLevel
     * @return the ToolBar.
     */
    public ToolBar getToolBar() { return toolBar; }

    /** Get the Menu Bar. Unfortunately named because getMenuBar() already exists */
    public static EMenuBar.Instance getTheMenuBar() { return menuBar; }

    /** Get the Menu Bar. Unfortunately named because getMenuBar() already exists */
    public static EMenuBar getEMenuBar() { return menuBar.getMenuBarGroup(); }

    /**
     * Method to return the speed of double-clicks (in milliseconds).
     * @return the speed of double-clicks (in milliseconds).
     */
    public static int getDoubleClickSpeed() { return doubleClickDelay; }

    /**
	 * Method to return the size of the screen that Electric is on.
	 * @return the size of the screen that Electric is on.
	 */
	public static Dimension getScreenSize()
	{
		return new Dimension(scrnSize);
	}

	/**
	 * Method to add an internal frame to the desktop.
	 * This only makes sense in MDI mode, where the desktop has multiple subframes.
	 * @param jif the internal frame to add.
	 */
	public static void addToDesktop(JInternalFrame jif)
	{
        if (desktop.isVisible() && !Job.isClientThread())
            SwingUtilities.invokeLater(new ModifyToDesktopSafe(jif, true)); else
            	(new ModifyToDesktopSafe(jif, true)).run();
    }

	/**
	 * Method to remove an internal frame from the desktop.
	 * This only makes sense in MDI mode, where the desktop has multiple subframes.
	 * @param jif the internal frame to remove.
	 */
	public static void removeFromDesktop(JInternalFrame jif)
	{
        if (desktop.isVisible() && !Job.isClientThread())
            SwingUtilities.invokeLater(new ModifyToDesktopSafe(jif, false)); else
            	(new ModifyToDesktopSafe(jif, false)).run();
    }

    private static class ModifyToDesktopSafe implements Runnable
    {
        private JInternalFrame jif;
        private boolean add;

        private ModifyToDesktopSafe(JInternalFrame jif, boolean add) { this.jif = jif;  this.add = add; }

        public void run()
        {
        	if (add)
        	{
	            desktop.add(jif);
	            try
	            {
	            	jif.show();
	            } catch (ClassCastException e)
	            {
	            	// Jake Baker keeps getting a ClassCastException here, so for now, let's catch it
	            	System.out.println("ERROR: Could not show new window: " + e.getMessage());
	            }
        	} else
        	{
                desktop.remove(jif);
        	}
        }
    }

	public static Cursor getCurrentCursor() { return cursor; }

	public static synchronized void setCurrentCursor(Cursor newcursor)
	{
        cursor = newcursor;
        setCurrentCursorPrivate(cursor);
    }

    private static synchronized void setCurrentCursorPrivate(Cursor cursor)
    {
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
            WindowFrame wf = it.next();
            wf.setCursor(cursor);
        }
	}

    public static synchronized List<ToolBar> getToolBars() {
        ArrayList<ToolBar> toolBars = new ArrayList<ToolBar>();
        for (Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); ) {
        	WindowFrame wf = it.next();
//        	toolBars.add(wf.getFrame().getToolBar());
        }
        return toolBars;
    }

    public static synchronized List<EMenuBar.Instance> getMenuBars() {
        ArrayList<EMenuBar.Instance> menuBars = new ArrayList<EMenuBar.Instance>();
        for (Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); ) {
        	WindowFrame wf = it.next();
//        	menuBars.add(wf.getFrame().getTheMenuBar());
        }
        return menuBars;
    }

    /**
     * The busy cursor overrides any other cursor.
     * Call clearBusyCursor to reset to last set cursor
     */
    public static synchronized void setBusyCursor(boolean on) {
        if (on) {
            if (!busyCursorOn)
                setCurrentCursorPrivate(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            busyCursorOn = true;
        } else {
            // if the current cursor is a busy cursor, set it to the last normal cursor
            if (busyCursorOn)
                setCurrentCursorPrivate(getCurrentCursor());
            busyCursorOn = false;
        }
    }

	private static Pref cacheWindowLoc = Pref.makeStringPref("WindowLocation", User.getUserTool().prefs, "");
	
	/**
	 * Method to return the current JFrame on the screen.
	 * @return the current JFrame.
	 */
	public static Frame getCurrentJFrame()
	{
        return getCurrentJFrame(false);
	}

	/**
	 * Method to return the current JFrame on the screen.
     * @param makeNewFrame whether or not to make a new WindowFrame if no current frame
	 * @return the current JFrame.
	 */
	public static Frame getCurrentJFrame(boolean makeNewFrame)
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame(makeNewFrame);
        if (wf == null) return null;
		return null; //TODO Fix this
	}

//	/**
//	 * Method to set the WindowFrame associated with this top-level window.
//	 * This only makes sense for SDI applications where a WindowFrame is inside of a TopLevel.
//	 * @param wf the WindowFrame to associatd with this.
//	 */
//	public void setWindowFrame(WindowFrame wf) { this.wf = wf; }

    /**
     * Method called when done with this Frame.  Both the menuBar
     * and toolBar have persistent state in static hash tables to maintain
     * consistency across different menu bars and tool bars in SDI mode.
     * Those references must be nullified for garbage collection to reclaim
     * that memory.  This is really for SDI mode, because in MDI mode the
     * TopLevel is only closed on exit, and all the application memory will be freed.
     * <p>
     * NOTE: JFrame does not get garbage collected after dispose() until
     * some arbitrary point later in time when the garbage collector decides
     * to free it.
     */
    public void finished()
    {
        //System.out.println(this.getClass()+" being disposed of");
        // clean up menubar
        setJMenuBar(null);
        // TODO: figure out why Swing still sends events to finished menuBars
        menuBar.finished(); menuBar = null;
        // clean up toolbar
        Container container = getContentPane();
        if (container != null) container.remove(toolBar);
//        getContentPane().remove(toolBar);
        toolBar.finished(); toolBar = null;
        // clean up scroll bar
        if (container != null) container.remove(sb);
        sb.finished(); sb = null;
        /* Note that this gets called from WindowFrame, and
            WindowFrame has a reference to EditWindow, so
            WindowFrame will call wnd.finished(). */
        // dispose of myself
    }

    /**
     * Method to return a list of possible window areas.
     * On MDI systems, there is just one window areas.
     * On SDI systems, there is one window areas for each display head on the computer.
     * @return an array of window areas.
     */
    public static Rectangle [] getWindowAreas()
	{
		Rectangle [] areas;
		areas = getDisplays();
		return areas;
	}

    /**
     * Method to return a list of display areas, one for each display head on the computer.
     * @return an array of display areas.
     */
    public static Rectangle [] getDisplays()
	{
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice [] gs = ge.getScreenDevices();
		Rectangle [] areas = new Rectangle[gs.length];
		for (int j = 0; j < gs.length; j++)
		{
			GraphicsDevice gd = gs[j];
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			areas[j] = gc.getBounds();
		}
		return areas;
	}

	/**
     * Print error message <code>msg</code> and stack trace
     * if <code>print</code> is true.
     * @param print print error message and stack trace if true
     * @param msg error message to print
     */
    public static void printError(boolean print, String msg)
    {
        if (print) {
            Throwable t = new Throwable(msg);
            System.out.println(t.toString());
            ActivityLogger.logException(t);
        }
    }

	private static class ReshapeComponentAdapter extends ComponentAdapter
	{
		public void componentMoved (ComponentEvent e) { saveLocation(e); }
		public void componentResized (ComponentEvent e) { saveLocation(e); }

		private void saveLocation(ComponentEvent e)
		{
			JApplet frame = (Main)e.getSource();
			Rectangle bounds = frame.getBounds();
			cacheWindowLoc.setString(bounds.getMinX() + "," + bounds.getMinY() + " " +
				bounds.getWidth() + "x" + bounds.getHeight());
		}
	}
	
	public static Icon getFrameIcon()
	{
		return null;
	}

	public static void InitializeWindows(Main thing) {
		try{
            menuBar = MenuCommands.menuBar().genInstance();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        thing.setJMenuBar(menuBar);

		// create the tool bar
		thing.toolBar = new ToolBar();
		thing.getContentPane().add(toolBar, BorderLayout.NORTH);
		// create the status bar
		thing.sb = new StatusBar(null, true);
		thing.getContentPane().add(thing.sb, BorderLayout.SOUTH);
		
		WindowFrame.createEditWindow(null);
		toplevel = thing;
	}
	
	public static Point getCursorLocation() {
		return toplevel.getLocationOnScreen();
	}
}
