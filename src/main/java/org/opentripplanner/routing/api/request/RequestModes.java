package org.opentripplanner.routing.api.request;

import static org.opentripplanner.routing.api.request.StreetMode.NOT_SET;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.tostring.ToStringBuilder;

public class RequestModes {

  /**
   * The RequestModes is mutable, so we need to keep a private default set of modes.
   */
  private static final RequestModes DEFAULTS = new RequestModes(
    StreetMode.WALK,
    StreetMode.WALK,
    StreetMode.WALK,
    StreetMode.WALK
  );

  @Nonnull
  public final List<StreetMode> accessModes;

  @Nonnull
  public final List<StreetMode> egressModes;

  @Nonnull
  public final List<StreetMode> directModes;

  @Nonnull
  public final List<StreetMode> transferModes;

  private RequestModes(
    StreetMode accessMode,
    StreetMode egressMode,
    StreetMode directMode,
    StreetMode transferMode
  ) {
    this.accessModes =
      List.of((accessMode != null && accessMode.accessAllowed()) ? accessMode : NOT_SET);
    this.egressModes =
      List.of((egressMode != null && egressMode.egressAllowed()) ? egressMode : NOT_SET);
    this.directModes = List.of(directMode != null ? directMode : NOT_SET);
    this.transferModes =
      List.of((transferMode != null && transferMode.transferAllowed()) ? transferMode : NOT_SET);
  }

  private RequestModes(
    List<StreetMode> accessModes,
    List<StreetMode> egressModes,
    List<StreetMode> directModes,
    List<StreetMode> transferModes
  ) {
    this.accessModes =
      accessModes != null && accessModes.stream().anyMatch(access -> access.accessAllowed())
        ? accessModes
        : List.of(NOT_SET);
    this.egressModes =
      egressModes != null && egressModes.stream().anyMatch(egress -> egress.egressAllowed())
        ? egressModes
        : List.of(NOT_SET);
    this.directModes = directModes != null ? directModes : List.of(NOT_SET);
    this.transferModes =
      transferModes != null &&
        transferModes.stream().anyMatch(transfer -> transfer.transferAllowed())
        ? transferModes
        : List.of(NOT_SET);
  }

  public RequestModes(RequestModesBuilder builder) {
    this(
      builder.accessModes(),
      builder.egressModes(),
      builder.directModes(),
      builder.transferModes()
    );
  }

  /**
   * Return a mode builder with the defaults set.
   */
  public static RequestModesBuilder of() {
    return DEFAULTS.copyOf();
  }

  public RequestModesBuilder copyOf() {
    return new RequestModesBuilder(this);
  }

  /**
   * Return the default set of modes with WALK for all street modes and all transit modes set.
   * Tip: Use the {@link #of()} to change the defaults.
   */
  public static RequestModes defaultRequestModes() {
    return DEFAULTS;
  }

  public boolean contains(StreetMode streetMode) {
    return (
      accessModes.contains(streetMode) ||
      egressModes.contains(streetMode) ||
      directModes.contains(streetMode)
    );
  }

  public StreetMode accessSearchMode() {
    return searchMode(accessModes);
  }

  public StreetMode directSearchMode() {
    return searchMode(directModes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestModes that = (RequestModes) o;

    if (!accessModes.equals(that.accessModes)) {
      return false;
    }
    if (!egressModes.equals(that.egressModes)) {
      return false;
    }
    if (!directModes.equals(that.directModes)) {
      return false;
    }

    return transferModes.equals(that.transferModes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessModes, egressModes, directModes, transferModes);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(RequestModes.class)
      .addCol("accessMode", accessModes)
      .addCol("egressMode", egressModes)
      .addCol("directMode", directModes)
      .addCol("transferMode", transferModes)
      .toString();
  }

  private StreetMode searchMode(List<StreetMode> modes) {
    if (modes.size() == 1) {
      return modes.getFirst();
    }
    // TODO support for more than two modes within a search might be implemented here
    return modes.stream().filter(mode -> mode != StreetMode.WALK).findFirst().get();
  }
}
