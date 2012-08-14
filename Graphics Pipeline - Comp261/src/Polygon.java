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

  private Vector3D[] vertices = new Vector3D[3];
  private Color reflectivity;
  private Vector3D normal;

  // state: computed during rendering.
  private boolean hidden = false;
  private Color shade;
  private Rectangle bounds = null;

  /** 	 */
  public Polygon(Color r, Vector3D v1, Vector3D v2, Vector3D v3) {
    this.reflectivity = r;
    vertices[0] = v1;
    vertices[1] = v2;
    vertices[2] = v3;
    normal = ((v2.minus(v1)).crossProduct(v3.minus(v2))).unitVector();
  }

  /*
   * Assumes that the string has 12 numbers: 3 vertices and a
   * colour/reflectivity
   */
  public Polygon(String s) {
    // System.out.println("s=" + s);
    Scanner sc = new Scanner(s);
    for (int v = 0; v < 3; v++) {
      vertices[v] = new Vector3D(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
    }
    reflectivity = new Color(sc.nextInt(), sc.nextInt(), sc.nextInt());
    normal = ((vertices[1].minus(vertices[0])).crossProduct(vertices[2].minus(vertices[1]))).unitVector();
  }

  public void apply(Transform t) {
    for (int v = 0; v < 3; v++) {
      vertices[v] = t.multiply(vertices[v]);
    }
    normal = ((vertices[1].minus(vertices[0])).crossProduct(vertices[2].minus(vertices[1]))).unitVector();
    bounds = null;
  }

  public Vector3D getNormal() {
    return normal;
  }

  public void hide() {
    hidden = true;
  }

  public boolean hidden() {
    return hidden;
  }

  public void computeShade(Vector3D lightSource, float ambient) {
    float cosAngle = normal.cosTheta(lightSource);
    float reflect = ambient + ((cosAngle > 0) ? cosAngle : 0);
    // System.out.println("shade: ambient="+ambient+ " normal="+normal+
    // " lightSource="+ lightSource+ " cos="+cosAngle+ "reflect=" + reflect);
    int red = Math.max(0, Math.min(255, (int) (reflectivity.getRed() * reflect)));
    int green = Math.max(0, Math.min(255, (int) (reflectivity.getGreen() * reflect)));
    int blue = Math.max(0, Math.min(255, (int) (reflectivity.getBlue() * reflect)));
    // System.out.println("color:" +
    // reflectivity.getRed()+","+reflectivity.getGreen()+","+reflectivity.getBlue()+
    // " -> shade:" + red+","+green+","+blue);
    shade = new Color(red, green, blue);
  }

  public Color getShade() {
    return shade;
  }

  public void reset() {
    hidden = false;
    shade = null;
  }

  /**
   * Returns the (integer) rectangle bounding box of the polygon in the x-y
   * plane.
   */
  public Rectangle bounds() {
    if (bounds == null) {
      int minX = Math.round(Math.min(Math.min(vertices[0].x, vertices[1].x), vertices[2].x));
      int minY = Math.round(Math.min(Math.min(vertices[0].y, vertices[1].y), vertices[2].y));
      int maxX = Math.round(Math.max(Math.max(vertices[0].x, vertices[1].x), vertices[2].x));
      int maxY = Math.round(Math.max(Math.max(vertices[0].y, vertices[1].y), vertices[2].y));
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
    for (int edge = 0; edge < 3; edge++) {
      Vector3D v1 = vertices[edge];
      Vector3D v2 = vertices[(edge + 1) % 3];
      if (v1.y > v2.y) {
        v1 = v2;
        v2 = vertices[edge];
      }
      float x = v1.x;
      float z = v1.z;
      float mx = (v2.x - v1.x) / (v2.y - v1.y);
      float mz = (v2.z - v1.z) / (v2.y - v1.y);

      int scanLine = Math.round(v1.y - bounds.y);
      double maxScanLine = (v2.y - bounds.y);

      // System.out.println("edgelist from: ("+v1.x+","+v1.y+","+v1.z+")-("+v2.x+","+v2.y+","+v2.z+") @ scanline "+scanLine+
      // " of "+ans.length + " starting at bounds.y =" + bounds.y + "mx="+mx);
      for (; scanLine <= maxScanLine; scanLine++, x += mx, z += mz) {
        // System.out.println(" x=" + x + " z=" + z + "@scanLine=" + scanLine);
        if (ans[scanLine] == null)
          ans[scanLine] = new EdgeList(x, z);
        else
          ans[scanLine].add(x, z);
      }
      // for (int i=0; i<ans.length; i++){EdgeList e = ans[i];
      // System.out.println(" ["+ i+"]: "+((e!=null)?e.leftX:"*")+
      // " - "+((e!=null)?e.rightX:"*"));}
    }
    return ans;
  }

  public EdgeList[] computeEdgeLists2() {
    bounds();
    // System.out.println("bounds: " +
    // bounds.x+","+bounds.y+","+bounds.width+","+bounds.height);
    EdgeList[] ans = new EdgeList[bounds.height + 1];
    for (int edge = 0; edge < 3; edge++) {
      Vector3D v1 = vertices[edge];
      Vector3D v2 = vertices[(edge + 1) % 3];
      if (v1.y > v2.y) {
        v1 = v2;
        v2 = vertices[edge];
      }
      float x = v1.x;
      float z = v1.z;
      float mx = (v2.x - v1.x) / (v2.y - v1.y);
      float mz = (v2.z - v1.z) / (v2.y - v1.y);

      int scanLine = Math.round(v1.y - bounds.y);
      int maxScanLine = Math.round(v2.y - bounds.y);

      // System.out.println("edgelist from: ("+v1.x+","+v1.y+","+v1.z+")-("+v2.x+","+v2.y+","+v2.z+") @ scanline "+scanLine+
      // " of "+ans.length + " starting at bounds.y =" + bounds.y + "mx="+mx);
      for (; scanLine <= maxScanLine; scanLine++, x += mx, z += mz) {
        // System.out.println(" x=" + x + " z=" + z + "@scanLine=" + scanLine);
        if (ans[scanLine] == null)
          ans[scanLine] = new EdgeList(x, z);
        else
          ans[scanLine].add(x, z);
      }
      // for (int i=0; i<ans.length; i++){EdgeList e = ans[i];
      // System.out.println(" ["+ i+"]: "+((e!=null)?e.leftX:"*")+
      // " - "+((e!=null)?e.rightX:"*"));}
    }
    return ans;
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
    if (shade != null) {
      f.format("s:(%3d-%3d-%3d)", shade.getRed(), shade.getGreen(), shade.getBlue());
    }
    return ans.toString();
  }

  public void printToFile(PrintStream ps) {
    for (int i = 0; i < 3; i++) {
      ps.print(vertices[i].x + " " + vertices[i].y + " " + vertices[i].z + " ");
    }
    ps.println(reflectivity.getRed() + " " + reflectivity.getGreen() + " " + reflectivity.getBlue());
  }

  public static void main(String[] args) {
    RenderPipeline.main(new String[] {});
  }

}
