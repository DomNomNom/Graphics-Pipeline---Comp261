import java.util.*;
//import comp100.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;

/**
 * A triangular polygon
 * 
 */
public class Polygon {

  private Vertex[] vertices         = new Vertex[3];
  private Vertex[] originalVertices = new Vertex[3];
  private Color reflectivity;
  private Color averageReflectivity;
  private PVector normal;

  // state: computed during rendering.
  private boolean hidden = false;
  private Rectangle bounds = null;
  private double shinynessExponent = 20;

  public Polygon(Color r, Vertex v1, Vertex v2, Vertex v3) {
    this.reflectivity = r;
    originalVertices[0] = v1;
    originalVertices[1] = v2;
    originalVertices[2] = v3;
    copyVertices();
    calculateNormal();
  }

  public Polygon(Color r, Vertex[] vs) {
    reflectivity = r;
    originalVertices[0] = vs[0];
    originalVertices[1] = vs[1];
    originalVertices[2] = vs[2];
    copyVertices();
    calculateNormal();
  }
  
  /*
   * Assumes that the string has 12 numbers: 3 vertices and a
   * colour/reflectivity
   */
  public Polygon(String s) {
    // System.out.println("s=" + s);
    Scanner sc = new Scanner(s);
    for (int v = 0; v < 3; v++)
      originalVertices[v] = Vertex.vertex(new PVector(sc.nextFloat(), sc.nextFloat(), sc.nextFloat()));
    reflectivity = new Color(sc.nextInt(), sc.nextInt(), sc.nextInt());
    copyVertices();
    calculateNormal();
  }

  private void calculateNormal() {
    normal = PVector.sub(vertices[1],vertices[0]).cross(PVector.sub(vertices[2],vertices[1]));
    normal.normalize();
  }
  
  public void apply(Transform t) {
    for (int v = 0; v<3; v++)
      vertices[v] = originalVertices[v].applyTransformCopy(t); //t.multiply(originalVertices[v]);
    calculateNormal();
    bounds = null;
  }

  private void copyVertices() {
    for (int v = 0; v<3; v++)
      vertices[v] = originalVertices[v];
  }

  public PVector getNormal() {
    return normal;
  }

  public void hide() {
    hidden = true;
  }
  public boolean hidden() {
    return hidden;
  }

  public int computeShade_phong(Light lightSource, PVector mySurfaceNormal) {
    // fall back to flat shading if we need to
    if (mySurfaceNormal==null || !Flags.smoothSurface) mySurfaceNormal = normal;
    
    // diffuse
    float cosAngle = PVector.cosTheta(mySurfaceNormal, lightSource);
    float diffuse = ((cosAngle > 0) ? cosAngle : 0);
    
    // Phong Specular Highlight
    // see: http://en.wikipedia.org/wiki/Specular_highlight
    // and for the following: http://www.gamedev.net/topic/411626-mirror-a-vector-around-a-plane/
    // mirror = V-2*n*(V . n)
    PVector mirroredLight = PVector.sub(lightSource, PVector.mult(mySurfaceNormal, 2*mySurfaceNormal.dot(lightSource)));
    mirroredLight.normalize();
    if (mirroredLight.z < 0) mirroredLight.z = 0;
    float specular = (float) Math.pow(mirroredLight.z, shinynessExponent );
    
    
    float ambient =  1;
    
    // multiply by the weighting defined by the light
    ambient  *= lightSource.ambient;
    diffuse  *= lightSource.diffuse;
    specular *= lightSource.specular;
    
    // unless one of the flags overrides it
    // optimisation: we could just tell it to not-do those in the first place
    if (Flags.noAmbient ) ambient  = 0;
    if (Flags.noDiffuse ) diffuse  = 0;
    if (Flags.noSpecular) specular = 0;
    
    // now calculate the final colour
    // note: specular light is defined as white
    // Optimisation: we could use less ints by using bitshifts earlier 
    Color reflectivity;
    if (Flags.colourAveraging) reflectivity = averageReflectivity();
    else                       reflectivity = reflectivity();
    int red   = Math.max(0, Math.min(255, (int) (reflectivity.getRed()   * (diffuse+ambient)  + 255*specular)));
    int green = Math.max(0, Math.min(255, (int) (reflectivity.getGreen() * (diffuse+ambient)  + 255*specular)));
    int blue  = Math.max(0, Math.min(255, (int) (reflectivity.getBlue()  * (diffuse+ambient)  + 255*specular)));
    
    // and turn it into int form
    return red<<16 | green<<8 | blue;
  }
  
  public Color reflectivity() {
    return reflectivity;
  }
  
  /** it is the average RGB value of the vertex colours (which in turn are averages) */
  private Color averageReflectivity() {
    // use cached version if possible
    if (averageReflectivity!=null) return averageReflectivity;
    
    int r=0, g=0, b=0;
    for (Vertex v : vertices) {
      Color c = v.reflectivity();
      r += c.getRed();
      g += c.getGreen();
      b += c.getBlue();
    }
    r /= vertices.length;
    g /= vertices.length;
    b /= vertices.length;
    averageReflectivity = new Color(r, g, b);
    return averageReflectivity;
  }
    
  public int getShade_int() {
    return reflectivity.getRed()<<16 | reflectivity.getGreen()<<8 | reflectivity.getBlue();
  }

  public void reset() {
    hidden = false;
  }

  /**
   * Returns the (integer) rectangle bounding box of the polygon in the x-y
   * plane.
   */
  public Rectangle bounds() {
    // TODO: caching the previous result would make it FASTER/less encapsulated
    if (bounds == null) {
      int minX = (int) Math.floor(Math.min(Math.min(vertices[0].x, vertices[1].x), vertices[2].x));
      int minY = (int) Math.floor(Math.min(Math.min(vertices[0].y, vertices[1].y), vertices[2].y));
      int maxX = (int) Math.ceil( Math.max(Math.max(vertices[0].x, vertices[1].x), vertices[2].x));
      int maxY = (int) Math.ceil( Math.max(Math.max(vertices[0].y, vertices[1].y), vertices[2].y));
      bounds = new Rectangle(minX, minY, (maxX - minX), (maxY - minY));
    }
    return bounds;
  }
 
  /**
   * Returns the edgeLists of the polygon: Each EdgeList corresponds to one
   * scanline and has the x and z values at the start and the x and z values at
   * the end. There are bounds.height+1 EdgeLists and the y values correspond to
   * bounds.y .. bounds.y+bounds.height We have to step along each edge, from
   * the end with the smallest y, stepping along y in steps of 1, adding the x
   * and z values to the edgeList each time. We compute the slope of x vs y and
   * z vs y and compute x and z incrementally.
   */
  public EdgeList[] computeEdgeLists() {
    bounds();
    // System.out.println("bounds: " +
    // bounds.x+","+bounds.y+","+bounds.width+","+bounds.height);
    EdgeList[] ans = new EdgeList[bounds.height + 1];
    for (int edge = 0; edge < 3; edge++) { // for all 3 edges
      Vertex v1 = vertices[edge];
      Vertex v2 = vertices[(edge + 1) % 3];
      if (v1.y > v2.y) {
        v1 = v2;
        v2 = vertices[edge];
      }

      int scanLine = (int)(v1.y - bounds.y);
      //int maxScanLine = Math.round(v2.y - bounds.y);
      int numSteps = (int)(v2.y - v1.y) + 1; 
      float x = v1.x;
      float z = v1.z;
      PVector norm = new PVector(v1.normal()); // our interpolated normal
      //System.out.println(norm);
      
      // the step-change for the above variables
      float mx = (v2.x - v1.x) / (numSteps);
      float mz = (v2.z - v1.z) / (numSteps);
      PVector m_norm = PVector.sub(v2.normal(), v1.normal()); // our interpolated normal step
      m_norm.div(numSteps);

      for (int i=0; i<numSteps; ++i, scanLine++, x += mx, z += mz, norm.add(m_norm)) {
        // System.out.println(" x=" + x + " z=" + z + "@scanLine=" + scanLine);
        if (ans[scanLine] == null)
          ans[scanLine] = new EdgeList(x, z, norm);
        else
          ans[scanLine].add(x, z, norm);
      }
    }
    return ans;
  }

  /** NOT IMPLEMENTED YET! DO NOT USE! */
  public EdgeList[] computeEdgeLists_bresehams() {
    // TODO
    bounds();
    EdgeList[] ans = new EdgeList[bounds.height + 1];
    for (int edge = 0; edge < 3; edge++) // for all 3 edges put the points into a edgeList
      drawEdgeLine(ans, vertices[edge], vertices[(edge+1)%3] );
    return ans;
  }
  
  /** 
   * uses Bresenham's line algorithm for nicer edges. 
   * see: http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm 
   */
  public void drawEdgeLine(EdgeList[] canvas, PVector A, PVector B){
    /*
     TODO: http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
     function line(x0, y0, x1, y1)
       dx := abs(x1-x0)
       dy := abs(y1-y0) 
       if x0 < x1 then sx := 1 else sx := -1
       if y0 < y1 then sy := 1 else sy := -1
       err := dx-dy
     
       loop
         setPixel(x0,y0)
         if x0 = x1 and y0 = y1 exit loop
         e2 := 2*err
         if e2 > -dy then 
           err := err - dy
           x0 := x0 + sx
         end if
         if e2 <  dx then 
           err := err + dx
           y0 := y0 + sy 
         end if
       end loop
   */
  }

  public String toString() {
    StringBuilder ans = new StringBuilder("Poly:");
    Formatter f = new Formatter(ans);
    ans.append(hidden ? 'h' : ' ');
    for (int i = 0; i < 3; i++) {
      f.format("(%8.3f,%8.3f,%8.3f)", vertices[i].x, vertices[i].y, vertices[i].z);
    }
    f.format("n:(%6.3f,%6.3f,%6.3f)", normal.x, normal.y, normal.z);
    f.format("c:(%3d-%3d-%3d)", reflectivity.getRed(), reflectivity.getGreen(), reflectivity.getBlue());
    bounds();
    f.format("b:(%3d %3d %3d %3d)", bounds.x, bounds.y, bounds.width, bounds.height);
    if (reflectivity != null)
      f.format("s:(%3d-%3d-%3d)", reflectivity.getRed(), reflectivity.getGreen(), reflectivity.getBlue());
    return ans.toString();
  }

  public void printToFile(PrintStream ps) {
    for (int i = 0; i < 3; i++)
      ps.print(vertices[i].x + " " + vertices[i].y + " " + vertices[i].z + " ");
    ps.println(reflectivity.getRed() + " " + reflectivity.getGreen() + " " + reflectivity.getBlue());
  }

  public static void main(String[] args) {
    RenderPipeline.main(new String[] {});
  }

}
