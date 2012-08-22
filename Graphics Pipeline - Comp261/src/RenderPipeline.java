import java.awt.Color;
import java.awt.Rectangle;
import java.awt.font.TransformAttribute;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.JFrame;


public class RenderPipeline {
  ArrayList<Polygon> polys = new ArrayList<Polygon>();
  ArrayList<PVector> lights = new ArrayList<PVector>();
  
  ZBuffer zBuffer;

  public final int width = 500;
  public final int height = 500;
  
  public RenderPipeline() {
    zBuffer = new ZBuffer(width, height);
    File inFile; // = new File("data/blocks1.txt");
    JFileChooser fc  = new JFileChooser("data");
    fc.showOpenDialog(new JFrame());
    inFile = fc.getSelectedFile();
    if (inFile == null) throw new Error("bad file");
    loadFile(inFile);
  }
  
  private void loadFile(File inFile) {
    try {
      Scanner lineScan = new Scanner(inFile); // iterates over each line
      
      Scanner sc = new Scanner(lineScan.nextLine()); 
      lights.add(new PVector(sc.nextFloat(), sc.nextFloat(), sc.nextFloat()));
      // TODO read more lights
      
      while (lineScan.hasNextLine())
        polys.add(new Polygon(lineScan.nextLine()));
    }
    catch (FileNotFoundException e) { throw new Error(e); }
  }
  
  private void loadPolygon(String line) {
    Scanner sc = new Scanner(line);

    PVector[] vertices = new PVector[3];
    for (int v=0; v<3; ++v)
      vertices[v] = new PVector(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());

    Color reflectivity = new Color(sc.nextInt(), sc.nextInt(), sc.nextInt());
  }
  
  public void render_wireFrame() {
    Rectangle screenBounds = new Rectangle(width, height);
    Transform transform = Transform.newScale(2, 2, 2); // TODO: translate properly
    for (Polygon p : polys) {
      p.apply(transform);
      Rectangle polyBounds = p.bounds();
      if (! screenBounds.intersects(polyBounds)) continue; // don't even bother with things that aren't in our view
      int y = polyBounds.y;
      p.computeShade(lights.get(0), 0.5f);
      EdgeList[] lists = p.computeEdgeLists();
      
      zBuffer.lockLine(y);
      for (EdgeList list : lists) {
        if (list == null) continue;
        if (! screenBounds.contains(0, y)) continue; // don't bother with lines that aren't in the screen 
        
        int minX = (int) Math.floor(list.l_x);
        int maxX = (int) Math.floor(list.r_x); // we are flooring but iterating to include that pixel
        if (minX == maxX) ++maxX; // we should at least have a 1 pixel (so we don't get division by 0 below)
        if (maxX < 0  ||  minX >= height) continue; //don't draw anything that can't be seen

        zBuffer.add(p.getShade_int(), minX, y, list.l_z);
        zBuffer.add(p.getShade_int(), maxX, y, list.r_z);
        
        ++y; // next line
      }
      zBuffer.releaseLine(y);
    }
  }
  
  public void render() {
    Rectangle screenBounds = new Rectangle(width, height);
    Transform transform = Transform.newScale(2, 2, 2); // TODO: translate properly
    for (Polygon p : polys) {
      p.apply(transform);
      if (p.getNormal().z > 0) continue;
      Rectangle polyBounds = p.bounds();
      if (! screenBounds.intersects(polyBounds)) continue; // don't even bother with things that aren't in our view
      int y = polyBounds.y;
      p.computeShade(lights.get(0), 0.5f);
      EdgeList[] lists = p.computeEdgeLists();
      
      zBuffer.lockLine(y);
      for (EdgeList list : lists) {
        if (list == null) continue;
        if (! screenBounds.contains(0, y)) continue; // don't bother with lines that aren't in the screen 
        
        int minX = (int) Math.floor(list.l_x);
        int maxX = (int) Math.floor(list.r_x); // we are flooring but iterating to include that pixel
        if (minX == maxX) ++maxX; // we should at least have a 1 pixel (so we don't get division by 0 below)
        if (maxX < 0  ||  minX >= height) continue; //don't draw anything that can't be seen
        float deltaZ = (list.r_z - list.l_z) / (float)(maxX - minX);
        float z = list.l_z;
        
        for (int x=minX;  x<=maxX;  ++x, z+=deltaZ) {
          if (x<0 || x>=height) continue; // don't draw of the side of the screen (these would wrap)
          // TODO: complex normal-interpolation
          //System.out.println(x + "\t" + y);
          zBuffer.add(p.getShade_int(), x, y, z);
        }
        
        ++y; // next line
      }
      zBuffer.releaseLine(y);
    }
  }
  
  public static void main(String[] args) {
    RenderPipeline p = new RenderPipeline();
    //p.render_wireFrame();
    p.render();
    

    GUI gui = new GUI(p.zBuffer.colours, p.height, p.width);
    gui.mainLoop();
    
    
    System.out.println("done :)");
  }
}
