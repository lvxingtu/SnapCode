package snap.playground;
import snap.gfx.HPos;
import snap.view.*;

/**
 * A pane to do Java REPL (Read, Eval, Print, Loop).
 */
public class Playground extends ViewOwner {
    
    // The TextPane
    PGTextPane        _textPane = new PGTextPane(this);
    
    // The TabPane
    PGTabPane         _tabPane = new PGTabPane(this);
    
    // The Console
    PGConsole         _console = new PGConsole(this);
    
    // The evaluator
    PGEvaluator       _evaluator = new PGEvaluator(this);

/**
 * Creates a new Playground.
 */
public Playground()  { }

/**
 * Returns the console.
 */
public PGConsole getConsole()  { return _console; }

/**
 * Creates the UI.
 */
protected View createUI()
{
    //Button rbtn = new Button("Run"); rbtn.setName("RunButton"); rbtn.setPrefSize(60,20);
    //HBox hbox = new HBox(); hbox.setChildren(rbtn);
    //VBox vbox = new VBox(); vbox.setFillWidth(true); vbox.setChildren(_textPane.getUI(), hbox);
    SplitView split = new SplitView(); split.setPrefSize(1000,900); split.setVertical(true);
    split.setItems(_textPane.getUI(), _console.getUI()); //(vbox,_tabPane.getUI());
    _console.getUI().setPrefHeight(180);
    return split;
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    super.initUI();
    
    // Add Button to TextPane
    Button rbtn = new Button("Run"); rbtn.setName("RunButton"); rbtn.setPrefSize(100,20); rbtn.setLeanX(HPos.RIGHT);
    rbtn.setOwner(this);
    _textPane.getToolBarPane().addChild(rbtn); _textPane.getToolBarPane().setPadding(0,20,0,4);
}

/**
 * Respond to UI.
 */
protected void respondUI(ViewEvent anEvent)
{
    if(anEvent.equals("RunButton")) {
        _evaluator.eval(_textPane.getTextView().getText());
        _textPane._evalView.updateLines();
    }
}

/**
 * Standard main method.
 */
public static void main(String args[])
{
    Playground pg = new Playground(); pg.getWindow().setTitle("Java Playground");
    pg.setWindowVisible(true);
}

}