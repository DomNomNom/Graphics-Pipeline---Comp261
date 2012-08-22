import java.util.*;


public class Vertex extends PVector {
  private static final long serialVersionUID = -5596774444508887096L;

  //private PVector pos; // position
  private PVector normal = null; // the average of the polygon normals
  private List<Polygon> polys = new ArrayList<Polygon>(); // the polygons we're attached to
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
    for (Polygon pp : polys)
      normal.add(pp.getNormal());
    return normal;
  }
  
  /**
   * A Factory for Vertices 
   * If you want to have a vertex that is at the same place as another,
   * you may only use the original vertex.
   * If it is at a new position, a new vertex gets created.
   */
  private static List<Vertex> allVerticies = new ArrayList<Vertex>();
  public static Vertex vertex(PVector pos) {
    // check whether this vertex already exists
    for (Vertex v : allVerticies)
      if (v.equals(pos))
        return v; // use the existing vertex
    
    // create a new one
    Vertex newVertex = new Vertex(pos);
    allVerticies.add(newVertex);
    return newVertex;
  }
  
}
