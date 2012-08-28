
public class Light extends PVector {
  final PVector original;
  
  public Light(PVector original) {
    this.original = original;
    set(original);
  }
  
  public void apply(Transform t) {
    set(t.multiply(original));
  }
  
}
