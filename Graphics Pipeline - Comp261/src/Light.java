
public class Light extends PVector {
  final PVector original;

  // light levels
  public final float ambient  = 0.1f;
  public final float diffuse  = 0.7f;
  public final float specular = 0.8f;
  
  public Light(PVector original) {
    this.original = original;
    set(original);
  }
  
  public void apply(Transform t) {
    set(t.multiply(original));
  }
  
}
