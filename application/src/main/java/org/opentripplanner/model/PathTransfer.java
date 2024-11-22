package org.opentripplanner.model;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Represents a transfer between stops with the street network path attatched to it.
 * <p>
 * Do not confuse this with {@link ConstrainedTransfer}.
 *
 * <p>
 * TODO these should really have a set of valid modes in case bike vs. walk transfers are different
 * TODO Should we just store the NearbyStop as a field here, or even switch to using it instead
 *      where this class is used
 */
public class PathTransfer implements Serializable {

  public final StopLocation from;

  public final StopLocation to;

  private final double distanceMeters;

  private final List<Edge> edges;

  public PathTransfer(StopLocation from, StopLocation to, double distanceMeters, List<Edge> edges) {
    this.from = from;
    this.to = to;
    this.distanceMeters = distanceMeters;
    this.edges = edges;
  }

  public String getName() {
    return from + " => " + to;
  }

  public double getDistanceMeters() {
    return distanceMeters;
  }

  public List<Edge> getEdges() {
    return this.edges;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(getClass())
      .addObj("from", from)
      .addObj("to", to)
      .addNum("distance", distanceMeters)
      .addColSize("edges", edges)
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final PathTransfer that = (PathTransfer) o;
    return (
      from.equals(that.from) &&
      to.equals(that.to) &&
      Objects.equals(edges, that.edges) &&
      Double.compare(distanceMeters, that.distanceMeters) == 0
    );
  }

  @Override
  public final int hashCode() {
    return Objects.hash(from, to, edges, distanceMeters);
  }
}
