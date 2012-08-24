
public class ZBuffer {
  private float[] depths;
  public int[] colours;

  int wd;  // width
  int ht; // height
  
  public ZBuffer(int width, int height) {
    wd = width;
    ht = height;
    depths = new float[wd * ht];
    colours = new int[wd * ht];
    clear();
  }
  
  public void clear() {
    for (int i=0; i<wd*ht; ++i) {
      colours[i] = 0; // background colour
      depths[i] = Float.POSITIVE_INFINITY; // initialize the depth so it will always me overwritten
    }
  }
  
  void lockLine(int y) {
    // TODO: make it threadsafe (one line at the time)
  }
  void releaseLine(int y) {/*TODO*/}
  
  void add(int colour, int x, int y, float z) {
    if (x<0 || y<0 || x>=wd || y>=ht) return; // don't accept invalid coordinates
    int i = x + y*ht; // our current index  TODO: optimize?
    if (z < depths[i]) {
      depths[i] = z;
      colours[i] = colour;
    }
  }
}
