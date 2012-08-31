import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.JFileChooser;
import javax.swing.JFrame;


public class RenderPipeline {
  public static final int threadCount = 2;
  
  ArrayList<Polygon> polys = new ArrayList<Polygon>();
  ArrayList<Light> lights = new ArrayList<Light>();
  
  ZBuffer zBuffer;
  
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
    if (inFile == null) System.exit(-1); //throw new Error("bad file");
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
    // no multithreading for a wireframe
    // note: this is old code
    
    Transform transform = calculateTransform();
    zBuffer.clear();
    for (Polygon p : polys) {
      p.apply(transform);
      Rectangle polyBounds = p.bounds();
      if (! screenBounds.intersects(polyBounds)) continue; // don't even bother with things that aren't in our view
      int y = polyBounds.y;
      EdgeList[] lists = p.computeEdgeLists();
      
      for (EdgeList list : lists) {
        if (list == null) continue;
        zBuffer.lockLine(y);
        // if (! screenBounds.contains(0, y)) continue; // don't bother with lines that aren't in the screen 
        
        int minX = (int) Math.ceil(list.l_x);
        int maxX = (int) Math.floor(list.r_x); // we are flooring but iterating to include that pixel
        if (minX == maxX) ++maxX; // we should at least have a 1 pixel (so we don't get division by 0 below)

        zBuffer.add(p.getShade_int(), minX, y, list.l_z);
        zBuffer.add(p.getShade_int(), maxX, y, list.r_z);
        
        zBuffer.releaseLine(y);
        ++y; // next line
      }
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

    ArrayBlockingQueue<Polygon> polyQueue= new ArrayBlockingQueue<Polygon>(polys.size(), false, polys);
    ArrayList<RenderThread> threads = new ArrayList<RenderThread>(threadCount);
    for (int i=0; i<threadCount; ++i)
      threads.add(new RenderThread(zBuffer, lights.get(0), screenBounds, polyQueue));

    long startTime = System.nanoTime();
    for (RenderThread t : threads) t.start();
    try {
      for (RenderThread t : threads) t.join();
    }
    catch (InterruptedException e) { }
    long endTime = System.nanoTime();
    System.out.println(endTime - startTime);

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
    
    GUI gui = new GUI(p);
    gui.mainLoop();
    
    
    System.out.println("done :)");
    System.exit(0);
  }
}
