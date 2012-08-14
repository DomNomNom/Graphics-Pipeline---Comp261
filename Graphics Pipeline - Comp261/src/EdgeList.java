
public class EdgeList {
  
  // x/z pairs for left and right edges
  float l_x, l_z;
  float r_x, r_z;
  
  public EdgeList(float x, float z) {
    // copy into both left and right since we don't know which one we are yet
    l_x = r_x = x;
    l_z = r_z = z;
  }

  public void add(float x, float z) {
    // copy into the appropriate left/right
    if (x > l_x) { r_x=x; r_z=z; }
    else         { l_x=x; l_z=z; }
  }
  
  // TODO: some kind of getter?
}
