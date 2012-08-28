import java.awt.Color;
import java.util.*;


public class Vertex extends PVector {
  private static final long serialVersionUID = -5596774444508887096L;

  private static final float maximumEqualDistance = 0f;
  
  private List<Polygon> polys = new ArrayList<Polygon>(); // the polygons we're attached to
  private PVector normal = null; // the average of the polygon normals
  private Color reflectivity = null;
  
  private Vertex(PVector pos) {
    x = pos.x;
    y = pos.y;
    z = pos.z;
  }
  
  
  public void addPolygon(Polygon p) {
    if (normal != null) throw new Error("can't add polygons after normal has been given out >:[");
    polys.add(p);
  }
  
  public PVector normal() {
    if (normal != null) return normal; // use the cached one if possible
    
    normal = new PVector(0,0,0);
    for (Polygon p : polys)
      normal.add(p.getNormal());
    normal.normalize();
    return normal;
  }
  
  public Color reflectivity() {
    if (reflectivity != null) return reflectivity; // use the cached one if possible
    
    // calculate the average colour
    int r=0, g=0, b=0;
    for (Polygon p : polys) {
      r += p.reflectivity().getRed();
      g += p.reflectivity().getGreen();
      b += p.reflectivity().getBlue();
    }
    r /= polys.size();
    g /= polys.size();
    b /= polys.size();
    reflectivity = new Color(r, g, b);
    return reflectivity;
  }
  
  
  /**
   * A Factory for Vertices 
   * If you want to have a vertex that is at the same place as another,
   * you may only use the original vertex.
   * If it is at a new position, a new vertex gets created.
   * 
   * please only read from allVerticies.
   */
  public static List<Vertex> allVerticies = new ArrayList<Vertex>();
  public static Vertex vertex(PVector pos) {
    // check whether this vertex already exists
    for (Vertex v : allVerticies)
      if (v.equals(pos))
      //if (PVector.sub(v, pos).magSq() <= maximumEqualDistance)
        return v; // use the existing vertex
    
    // create a new one
    Vertex newVertex = new Vertex(pos);
    allVerticies.add(newVertex);
    return newVertex;
  }
  
  /** 
   * Returns a new Vertex with the given transformation applied to it.
   * This vertex will not be a element of allVerticies
   */
  public Vertex applyTransformCopy(Transform t) {
    Vertex newVertex = new Vertex(t.multiply(this));
    newVertex.normal = null;
    newVertex.polys = this.polys;
    return newVertex;
  }
  
}
