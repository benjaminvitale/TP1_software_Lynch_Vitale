// Token con vencimiento fijo (5')
package gc.domain;
import java.time.Duration;
import java.time.Instant;

public final class Token {
  public static final String Expired = "Token expired";
  private final String value;
  private final String userId;
  private final Instant issuedAt;

  private static final Duration TTL = Duration.ofMinutes(5);

  public static Token issuedFor(String userId, String value, Instant issuedAt) {
    return new Token(userId, value, issuedAt);
  }
  private Token(String userId, String value, Instant issuedAt) {
    this.value = value; this.userId = userId; this.issuedAt = issuedAt;
  }

  public void assertValidAt(Instant now) {
    if (issuedAt.plus(TTL).isBefore(now)) throw new RuntimeException(Expired);
  }

  public String value() { return value; }
  public String userId() { return userId; }
}
