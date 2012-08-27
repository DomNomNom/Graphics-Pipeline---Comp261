
public class Light extends PVector {
  final PVector original;
  
  public Light(PVector original) {
    this.original = original;
    copyFrom(original);
  }
  
  public void apply(Transform t) {
    copyFrom(t.multiply(original));
  }
  
  private void copyFrom(PVector p) {
    x = p.x;
    y = p.y;
    z = p.z;
  }
}
