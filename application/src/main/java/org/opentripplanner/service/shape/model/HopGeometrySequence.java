package org.opentripplanner.service.shape.model;

import org.locationtech.jts.geom.LineString;

/**
 * An ordered, end-to-end sequence of {@link HopGeometry} covering every hop of one stop pattern,
 * together with a cumulative arc-length table that makes distance queries constant-time.
 * <p>
 * This is the direct successor to the {@code CompactLineStringSequence} field that
 * {@code TripPattern} owns today. The difference is ownership and sharing: a sequence no longer
 * belongs to one pattern, it is a value that a pattern default and any number of per-trip overrides
 * can point at, and its members are interned {@link HopGeometry} instances shared with unrelated
 * sequences.
 * <p>
 * Positions follow the same "vertex position" convention as the stop pattern, so they can be indexed
 * directly by a stop position in pattern:
 * <ul>
 *   <li>position {@code 0} is the first stop;</li>
 *   <li>position {@code i} for {@code 0 < i < size()} is the stop between hop {@code i-1} and hop
 *       {@code i};</li>
 *   <li>position {@code size()} is the last stop.</li>
 * </ul>
 * A sequence for a degenerate pattern of one stop or fewer has {@code size() == 0}.
 * <p>
 * Implementations must be immutable and safe to publish across threads without synchronization: a
 * single instance is read concurrently by every request that touches a pattern using it.
 * <p>
 * PROVISIONAL: this type is part of the initial {@code ShapeService} API design and may change
 * before the service is wired up.
 */
public interface HopGeometrySequence {
  /** The number of hops, which is one less than the number of stops. */
  int size();

  /** The geometry of the hop leaving the stop at {@code stopPosInPattern}. */
  HopGeometry hop(int stopPosInPattern);

  /**
   * Distance in meters between two stop positions, read from the cumulative table in constant time.
   * <p>
   * This is the accessor for the routing-result hot path — every transit leg needs its distance —
   * so it returns a primitive. Callers that want the domain type use
   * {@link org.opentripplanner.service.shape.ShapeService#distanceBetween}.
   * <p>
   * Distances are accumulated as floating point and rounded only when each cumulative entry is
   * stored, which bounds the error of any single query to one meter regardless of how long the leg
   * is.
   */
  int distanceMetersBetween(int boardingStopPosition, int alightingStopPosition);

  /**
   * Decode and concatenate the hops between two stop positions into one {@link LineString}, emitting
   * the coordinate shared by two consecutive hops only once.
   * <p>
   * Returns an empty line string for an empty or degenerate range. Not cached: each call decodes
   * again, which is why the compacted form is kept in the first place.
   */
  LineString concatenate(int boardingStopPosition, int alightingStopPosition);

  /** Decode and concatenate every hop, i.e. {@code concatenate(0, size())}. */
  LineString concatenate();
}
