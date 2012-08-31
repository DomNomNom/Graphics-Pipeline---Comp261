import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class ZBuffer {
  private float[] depths;
  public int[] colours;
  private Lock[] lineLocks; // one lock per line

  int wd;  // width
  int ht; // height
  
  public ZBuffer(int width, int height) {
    wd = width;
    ht = height;
    depths = new float[wd * ht];
    colours = new int[wd * ht];
    lineLocks = new Lock[ht];
    for (int y=0; y<ht; ++y)
      lineLocks[y] = new ReentrantLock();
    clear();
  }

  void lockLine(int y) {
    if (y<0 || y>=ht) return;
    lineLocks[y].lock();
  }
  void releaseLine(int y) {
    if (y<0 || y>=ht) return;
    lineLocks[y].unlock();
  }
  
  void add(int colour, int x, int y, float z) {
    if (x<0 || y<0 || x>=wd || y>=ht) return; // don't accept invalid coordinates
    int i = x + y*ht; // our current index  TODO: optimize?
    if (z < depths[i]) {
      depths[i] = z;
      colours[i] = colour;
    }
  }
  
  
  
  
  public void clear() {
    Random rand = new Random();
    rand.setSeed(15);
    for (int i=0; i<wd*ht; ++i) {
      int shade = rand.nextInt(40);
      colours[i] = shade<<0 | shade<<8 | shade<<16; // background colour
      depths[i] = Float.POSITIVE_INFINITY; // initialize the depth so it will always me overwritten
    }
    add(255 << 8, wd-3, ht-1, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-1, ht-1, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-4, ht-4, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-2, ht-1, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-4, ht-1, Float.NEGATIVE_INFINITY);
    add(000 << 8, wd-3, ht-2, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-3, ht-3, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-3, ht-4, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-4, ht-3, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-4, ht-2, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-1, ht-4, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-2, ht-2, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-2, ht-4, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-1, ht-3, Float.NEGATIVE_INFINITY);
    add(000 << 8, wd-2, ht-3, Float.NEGATIVE_INFINITY);
    add(255 << 8, wd-1, ht-2, Float.NEGATIVE_INFINITY);
  }
  
}
