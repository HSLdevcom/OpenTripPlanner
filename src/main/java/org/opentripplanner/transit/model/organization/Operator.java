package org.opentripplanner.transit.model.organization;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitEntity;

/**
 * A company which is responsible for operating public transport services. The operator will often
 * operate under contract with an Authority (Agency).
 * <p/>
 * Netex ONLY. Operators are available only if the data source is Netex, not GTFS.
 *
 * @see Agency
 */
public class Operator extends TransitEntity {

  private final String name;

  private final String url;

  private final String phone;

  Operator(OperatorBuilder builder) {
    super(builder.getId());
    this.name = builder.getName();
    this.url = builder.getUrl();
    this.phone = builder.getPhone();

    // name is required
    Objects.requireNonNull(this.name);
  }

  public static OperatorBuilder of(FeedScopedId id) {
    return new OperatorBuilder(id);
  }

  public OperatorBuilder mutate() {
    return new OperatorBuilder(this);
  }

  @NotNull
  public String getName() {
    return name;
  }

  @Nullable
  public String getUrl() {
    return url;
  }

  @Nullable
  public String getPhone() {
    return phone;
  }

  public String toString() {
    return "<Operator " + getId() + ">";
  }
}
