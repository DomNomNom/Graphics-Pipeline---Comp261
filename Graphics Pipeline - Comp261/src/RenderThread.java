import java.util.*;
import java.awt.Rectangle;
import java.util.concurrent.ArrayBlockingQueue;


public class RenderThread extends Thread {
  private static final int chunkSize = 4; // how many polygons we work on before we ask for more
  
  ZBuffer zBuffer;
  Light light;
  Rectangle screenBounds;
  ArrayBlockingQueue<Polygon> polyQueue;
  
  public RenderThread(ZBuffer zBuffer, Light light, Rectangle screenBounds, ArrayBlockingQueue<Polygon> polyQueue) {
    this.zBuffer = zBuffer;
    this.light = light;
    this.screenBounds = screenBounds;
    this.polyQueue = polyQueue;
  }
  
  @Override
  public void run() {
    // poll from the queue until it's empty
    List<Polygon> workChunk = new ArrayList<Polygon>(chunkSize);
    polyQueue.drainTo(workChunk, chunkSize);
    while (workChunk.size() > 0) {
      for (Polygon p : workChunk)
        renderPoly(p);
      workChunk.clear();
      polyQueue.drainTo(workChunk, chunkSize);
    }
  }
  
  public void renderPoly(Polygon p) {
    if (p.getNormal().z > 0) return; // don't draw polys that are facing away
    Rectangle polyBounds = p.bounds();
    if (! screenBounds.intersects(polyBounds)) return; // don't even bother with things that aren't in our view
    int y = polyBounds.y;
    EdgeList[] lists = p.computeEdgeLists();
    
    for (EdgeList list : lists) {
      if (list == null) continue;
      //if (! screenBounds.contains(0, y)) continue; // don't bother with lines that aren't in the screen 
      zBuffer.lockLine(y);
      
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
        if (x<0 || x>=screenBounds.width) continue; // don't draw of the side of the screen (these would wrap)
        normal.normalize(normalizedNormal);
        zBuffer.add(p.computeShade_phong(light, normalizedNormal), x, y, z);
      }
      
      zBuffer.releaseLine(y);
      ++y; // next line
    }

  }
}
