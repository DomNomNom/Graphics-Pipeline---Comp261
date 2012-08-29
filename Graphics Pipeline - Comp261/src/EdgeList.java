
public class EdgeList /*implements Iterable<Float>*/ {
  
  // x/z pairs for left and right edges
  float l_x, r_x;
  float l_z, r_z;
  
  PVector l_normal, r_normal;
  
  public EdgeList(float x, float z, PVector normal) {
    // copy into both left and right since we don't know which one we are yet
    l_x = r_x = x;
    l_z = r_z = z;
    l_normal = r_normal = new PVector(normal);
  }

  public void add(float x, float z, PVector normal) {
    // copy into the appropriate left/right
    if     (x < l_x) { l_x=x; l_z=z; l_normal=new PVector(normal); }
    else if(x > r_x) { r_x=x; r_z=z; r_normal=new PVector(normal); }
  }
  
  // TODO: some kind of getter? or iterator?
}
