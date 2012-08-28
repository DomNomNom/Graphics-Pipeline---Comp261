import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.JFrame;


public class RenderPipeline {
  ArrayList<Polygon> polys = new ArrayList<Polygon>();
  ArrayList<Light> lights = new ArrayList<Light>();
  
  ZBuffer zBuffer;

  enum renderMode { WIREFRAME, FLAT, PHONG };
  
  public final int width = 500;
  public final int height = 500;
  public final PVector size = new PVector(width, height);
  Rectangle screenBounds = new Rectangle(width, height);

  public float scale = 1.5f;

  public PVector customTranslation = PVector.mult(size, 0.5f);
  public PVector objectRotation = new PVector();
  public PVector lightRotation = new PVector();
  
  public RenderPipeline() {
    zBuffer = new ZBuffer(width, height);
    File inFile = new File("data/monkey.txt");
    JFileChooser fc  = new JFileChooser("data");
    fc.showOpenDialog(new JFrame());
    inFile = fc.getSelectedFile();  // TODO: uncomment
    if (inFile == null) throw new Error("bad file");
    loadFile(inFile);
  }
  
  private void loadFile(File inFile) {
    try {
      Scanner lineScan = new Scanner(inFile); // iterates over each line
      
      Scanner sc = new Scanner(lineScan.nextLine()); 
      lights.add(new Light(new PVector(sc.nextFloat(), sc.nextFloat(), sc.nextFloat())));
      // TODO read more lights
      
      while (lineScan.hasNextLine())
        polys.add(loadPolygon(lineScan.nextLine()));
    }
    catch (FileNotFoundException e) { throw new Error(e); }
  }
  
  private Polygon loadPolygon(String line) {
    Scanner sc = new Scanner(line);

    Vertex[] vertices = new Vertex[3];
    for (int v=0; v<3; ++v)
      vertices[v] = Vertex.vertex(new PVector(sc.nextFloat(), sc.nextFloat(), sc.nextFloat()));

    Color reflectivity = new Color(sc.nextInt(), sc.nextInt(), sc.nextInt());
    Polygon newPoly = new Polygon(reflectivity, vertices);
    
    for (Vertex v : vertices)
      v.addPolygon(newPoly);
    
    return newPoly;
  }
  
  public void render_wireFrame() {
    Transform transform = calculateTransform();
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
    zBuffer.clear();

    Transform lightTransform = Transform.identity();
    lightTransform = Transform.newXRotation(lightRotation.y ).compose(lightTransform);
    lightTransform = Transform.newYRotation(lightRotation.x ).compose(lightTransform);
    for (Light light : lights)
      light.apply(lightTransform);
    
    
    Transform transform = calculateTransform();
    for (Polygon p : polys)
      p.apply(transform);

    for (Polygon p : polys) {
      if (p.getNormal().z > 0) continue; // don't draw polys that are facing away
      Rectangle polyBounds = p.bounds();
      if (! screenBounds.intersects(polyBounds)) continue; // don't even bother with things that aren't in our view
      int y = polyBounds.y;
      p.computeShade(lights.get(0), 0.1f);
      EdgeList[] lists = p.computeEdgeLists();
      
      zBuffer.lockLine(y);
      for (EdgeList list : lists) {
        if (list == null) continue;
        //if (! screenBounds.contains(0, y)) continue; // don't bother with lines that aren't in the screen 
        
        int minX = (int) Math.ceil(list.l_x);
        int maxX = (int) Math.floor(list.r_x); // we are flooring but iterating to include that pixel
        if (minX == maxX) ++maxX; // we should at least have a 1 pixel (so we don't get division by 0 below)
        //if (maxX < 0  ||  minX > width)  continue; //don't draw anything that can't be seen
        // note: the previous check was removed as it somehow was omitting things that should be viewed on screen edges
        int steps = (int) (maxX - minX);
        float z = list.l_z;
        float deltaZ = (list.r_z - list.l_z) / (steps);

        PVector normal = list.l_normal;
        PVector normalizedNormal = new PVector();
        PVector deltaNormal = PVector.sub(list.r_normal, list.l_normal);
        deltaNormal.div(steps);
        
        for (int x=minX;  x<=maxX;  ++x, z+=deltaZ, normal.add(deltaNormal)) {
          if (x<0 || x>=width) continue; // don't draw of the side of the screen (these would wrap)
          normal.normalize(normalizedNormal);
          //zBuffer.add(p.getShade_int(), x, y, z); // flat shading
          zBuffer.add(p.computeShade_phong(lights.get(0), normalizedNormal, 0.1f), x, y, z);
        }
        
        ++y; // next line
      }
      zBuffer.releaseLine(y);
    }
  }
  
  public void render_phong() {
    // TODO
  }
  
  private Transform calculateTransform() {
    PVector average = new PVector();
    for (Vertex v : Vertex.allVerticies)
      average.add(v);
    average.div(-Vertex.allVerticies.size());
    
    Transform transform = Transform.identity();
    transform = Transform.newTranslation(average             ).compose(transform); // move center to (0, 0)
    transform = Transform.newScale(scale, scale, scale       ).compose(transform); // scale
    transform = Transform.newXRotation(objectRotation.y      ).compose(transform);
    transform = Transform.newYRotation(objectRotation.x      ).compose(transform);
    
    transform = Transform.newTranslation(customTranslation  ).compose(transform); // move to mouse
    //transform = Transform.newTranslation(PVector.div(size, 2)).compose(transform); // move to screen center
    
    return transform;
  }
  
  public static void main(String[] args) {
    RenderPipeline p = new RenderPipeline();
    //p.render_wireFrame();
    
    GUI gui = new GUI(p);
    gui.mainLoop();
    
    
    System.out.println("done :)");
    System.exit(0);
  }
}
