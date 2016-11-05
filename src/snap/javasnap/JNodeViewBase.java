package snap.javasnap;
import snap.gfx.*;
import snap.view.*;

/**
 * A pane that draws background for puzzle pieces.
 */
public class JNodeViewBase extends VBox {

    // The type
    Type          _type = Type.Piece;
    
    // The segment position, if piece
    Seg           _seg = Seg.Only;
    
    // The color
    Color         _color;
    
    // Background shape
    PathView      _bg;
    
    // The HBox
    HBox          _hbox;
    
    // The VBox
    VBox          _vbox;
    
    // The foreground shape
    PathView      _fg;
    
    // The selection shape
    Path          _selShape;
    
    // Constants for type
    public enum Type { Piece, BlockStmt, MemberDecl, None }
    
    // Constants for segment position
    public enum Seg { Only, First, Middle, Last }
    
    // Constant for piece height
    static double PieceHeight = 26, BlockTailHeight = 14;


/**
 * Create new background pane.
 */
protected JNodeViewBase()
{
    // Configure
    setAlign(Pos.TOP_LEFT); setFillWidth(true); //setSpacing(2);
    
    // Set background
    _bg = new PathView(); _bg.setManaged(false); //_bg.setStroke(null);
    //Light.Distant light = new Light.Distant(); light.setAzimuth(55); light.setElevation(115);
    //Lighting lighting = new Lighting(); lighting.setLight(light); lighting.setSurfaceScale(.4);
    EmbossEffect emb = new EmbossEffect(68,112,4);
    _bg.setEffect(emb);
    
    // Create HBox
    _hbox = new HBox(); _hbox.setMinHeight(PieceHeight); _hbox.setSpacing(2);
    //_hbox.setBorder(Color.RED, 2);
    
    // Create/set foreground
    _fg = new PathView(); _fg.setManaged(false); //_fg.setStroke(null); _fg.setMouseTransparent(true);
    setChildren(_bg, _fg, _hbox);//, _fg);
}

/**
 * Returns the type.
 */
public Type getType()  { return _type; }

/**
 * Sets the type.
 */
public void setType(Type aType)  { _type = aType; }

/**
 * Returns the segment position.
 */
public Seg getSeg()  { return _seg; }

/**
 * Sets the segment position.
 */
public void setSeg(Seg aSeg)  { _seg = aSeg; }

/**
 * Returns the color.
 */
public Color getColor()  { return _color; }

/**
 * Sets the color.
 */
public void setColor(Color aColor)
{
    _color = aColor;
    _bg.setFill(aColor); //_bg.setBorder(aColor!=null? aColor.darker() : null, 2);
}

/**
 * Returns the horizontal box.
 */
public HBox getHBox()  { return _hbox; }

/**
 * Returns the vertical box.
 */
public VBox getVBox()  { return _vbox!=null? _vbox : (_vbox=createVBox()); }

/**
 * Creates the vertical box.
 */
protected VBox createVBox()
{
    VBox vbox = new VBox(); vbox.setMinHeight(35); vbox.setFillWidth(true); //vbox.setSpacing(2);
    vbox.setPadding(3,1,BlockTailHeight,12);
    addChild(vbox);//,getChildren().size()-1);
    return vbox;
}

/**
 * Sets whether part is selected.
 */
public void setSelected(boolean aValue)
{
    _fg.setBorder(aValue? Border.createLineBorder(Color.get("#FFFFFFCC"),2) : null);
}

/**
 * Sets whether part is being dragged over.
 */
public void setUnderDrag(boolean aValue)  { _fg.setFill(aValue? Color.get("#FFFFFF88") : null); }

/**
 * Override to resize rects.
 */
protected void layoutChildren()
{
    super.layoutChildren();
    double w = getWidth(), h = getHeight(); resizeBG(_bg, w, h); resizeBG(_fg, w, h);
}

/**
 * Resizes background Path to size.
 */
protected void resizeBG(PathView aPath, double aW, double aH)
{
    aPath.setSize(aW,aH);
    switch(_type) {
        case Piece: resizeBGPiece(aPath, aW, aH); break;
        case BlockStmt: resizeBGBlock(aPath, aW, aH, true); break;
        case MemberDecl: resizeBGBlock(aPath, aW, aH, false); break;
    }
}

/**
 * Resizes background Path to size as simple puzzle piece.
 */
protected void resizeBGPiece(PathView aPath, double aW, double aH)
{
    switch(_seg) {
        case First: resizeBGPieceFirst(aPath, aW, aH); break;
        case Middle: resizeBGPieceMiddle(aPath, aW, aH); break;
        case Last: resizeBGPieceLast(aPath, aW, aH); break;
        default: resizeBGPieceOnly(aPath, aW, aH);
    }
}

/**
 * Resizes background Path to size as simple puzzle piece.
 */
protected void resizeBGPieceOnly(PathView aPath, double aW, double aH)
{
    Path p = aPath.getPath(); p.clear();
    double r = 5; p.moveTo(r,0);
    p.hlineTo(12); p.lineTo(15,3); p.hlineTo(25); p.lineTo(28,0); // Divit
    p.hlineTo(aW-r); p.arcTo(aW,0,aW,r);
    p.vlineTo(aH-r); p.arcTo(aW,aH,aW-r,aH);
    p.hlineTo(28); p.lineTo(25,aH+3); p.hlineTo(15); p.lineTo(12,aH); // Divit
    p.hlineTo(r); p.arcTo(0,aH,0,aH-r);
    p.vlineTo(r); p.arcTo(0,0,r,0); p.close();
}

/**
 * Resizes background Path to size as simple puzzle piece.
 */
protected void resizeBGPieceFirst(PathView aPath, double aW, double aH)
{
    Path p = aPath.getPath(); p.clear();
    double r = 5; p.moveTo(r,0);
    p.hlineTo(12); p.lineTo(15,3); p.hlineTo(25); p.lineTo(28,0); // Divit
    p.hlineTo(aW); //e.add(new HLineTo(aW-r)); e.add(new ArcTo(r,r,0,aW,r,false,true));
    p.vlineTo(aH); //e.add(new VLineTo(aH-r)); e.add(new ArcTo(r,r,0,aW-r,aH,false,true));
    p.hlineTo(28); p.lineTo(25,aH+3); p.hlineTo(15); p.lineTo(12,aH); // Divit
    p.hlineTo(r); p.arcTo(0,aH,0,aH-r);
    p.vlineTo(r); p.arcTo(0,0,r,0); p.close();
}

/**
 * Resizes background Path to size as simple puzzle piece.
 */
protected void resizeBGPieceMiddle(PathView aPath, double aW, double aH)
{
    Path p = aPath.getPath(); p.clear(); double r = 0; //8
    p.moveTo(r,0); p.hlineTo(aW-r); p.arcTo(aW,0,aW,r);
    p.vlineTo(aH-r); p.arcTo(aW,aH,aW-r,aH);
    p.hlineTo(r); p.arcTo(0,aH,0,aH-r);
    p.vlineTo(r); p.arcTo(0,0,r,0); p.close();
}

/**
 * Resizes background Path to size as simple puzzle piece.
 */
protected void resizeBGPieceLast(PathView aPath, double aW, double aH)
{
    Path p = aPath.getPath(); p.clear(); double r = 0; //e.add(new MoveTo(r,0));
    p.moveTo(0,0); p.hlineTo(aW-r); p.arcTo(aW,0,aW,r);
    p.vlineTo(aH-r); p.arcTo(aW,aH,aW-r,aH);
    p.hlineTo(0); //e.add(new HLineTo(r)); e.add(new ArcTo(r,r,0,0,aH-r,false,true));
    p.vlineTo(0); //e.add(new VLineTo(r)); e.add(new ArcTo(r,r,0,r,0,false,true)); e.add(new ClosePath());
}

/**
 * Resizes background Path to size as puzzle block.
 */
protected void resizeBGBlock(PathView aPath, double aW, double aH, boolean doOuter)
{
    Path p = aPath.getPath(); p.clear();
    double r = 5, h1 = PieceHeight, h2 = aH - BlockTailHeight; p.moveTo(r,0);
    if(doOuter) { p.hlineTo(12); p.lineTo(15,3); p.hlineTo(25); p.lineTo(28,0); }
    p.hlineTo(aW-r); p.arcTo(aW,0,aW,r);
    p.vlineTo(h1-r); p.arcTo(aW,h1,aW-r,h1);
    p.hlineTo(40); p.lineTo(37,h1+3); p.hlineTo(27); p.lineTo(24,h1); // Divit
    p.hlineTo(10+r); p.arcTo(10,h1,10,h1+r);
    p.vlineTo(h2-r); p.arcTo(10,h2,10+r,h2);
    p.hlineTo(24); p.lineTo(27,h2+3); p.hlineTo(37); p.lineTo(40,h2); // Divit
    p.hlineTo(aW-r); p.arcTo(aW,h2,aW,h2+r);
    p.vlineTo(aH-r); p.arcTo(aW,aH,aW-r,aH);
    if(doOuter) { p.hlineTo(28); p.lineTo(25,aH+3); p.hlineTo(15); p.lineTo(12,aH); }
    p.hlineTo(r); p.arcTo(0,aH,0,aH-r);
    p.vlineTo(r); p.arcTo(0,0,r,0); p.close();
}

}