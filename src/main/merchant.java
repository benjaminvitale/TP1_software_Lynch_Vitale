package gc.domain;
public final class Merchant {
  private final String id;
  private final String key;
  public static Merchant identified(String id, String key) { return new Merchant(id, key); }
  private Merchant(String id, String key) { this.id=id; this.key=key; }
  public String id() { return id; }
  public String key() { return key; }
}
