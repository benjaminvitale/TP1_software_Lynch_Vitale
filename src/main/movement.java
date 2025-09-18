package gc.domain;
import java.math.BigDecimal;
import java.time.Instant;

public final class Movement {
  private final Instant when;
  private final String merchantId;
  private final BigDecimal amount; // positivo como magnitud del débito
  private final String description;

  public static Movement debit(Instant when, String merchantId, BigDecimal amount, String description) {
    return new Movement(when, merchantId, amount, description);
  }
  private Movement(Instant when, String merchantId, BigDecimal amount, String description) {
    this.when = when; this.merchantId = merchantId; this.amount = amount; this.description = description;
  }
  // getters...
}
