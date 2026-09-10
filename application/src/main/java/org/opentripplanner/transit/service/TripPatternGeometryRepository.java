package org.opentripplanner.transit.service;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.geometry.CompactLineStringSequence;

/**
 * Stores the compacted stop-to-stop geometry ({@link CompactLineStringSequence}) associated with
 * trip patterns and, optionally, individual trips.
 * <p>
 * Two independent maps are kept, reflecting their different lifetimes:
 * <ul>
 *   <li><b>Pattern-default geometry</b> - one entry per {@code TripPattern} id. This is built once
 *   at graph build time (from GTFS shapes.txt or NeTEx ServiceLinks, or synthesized as straight
 *   lines between stops when no shape data is available) and is expected to have the same
 *   lifetime/serialization story as the rest of the transit model.</li>
 *   <li><b>Per-trip override geometry</b> - one entry per {@code Trip} id, populated <em>only</em>
 *   when a trip's own geometry differs from its pattern's default (e.g. GTFS trips that share a
 *   stop pattern/route/direction/mode but reference a different {@code shape_id}). This keeps
 *   memory use low, since the common case (all trips in a pattern share one shape) needs no
 *   per-trip storage at all.
 *   <p>
 *   This map is intentionally kept mutable and separate from the (largely immutable, serialized)
 *   pattern-default map so that a future real-time updater can add, replace, or remove a single
 *   trip's override at runtime - for example to support GTFS-RT {@code TripUpdate}s that redefine
 *   the shape for a trip whose stop sequence was replaced in real time (see
 *   <a href="https://github.com/google/transit/issues/653">google/transit#653</a>). No such
 *   updater is wired in yet; this class only provides the storage shape for it.</li>
 * </ul>
 */
public class TripPatternGeometryRepository implements Serializable {

  private final Map<FeedScopedId, CompactLineStringSequence> patternGeometries =
    new ConcurrentHashMap<>();

  private final Map<FeedScopedId, CompactLineStringSequence> tripGeometryOverrides =
    new ConcurrentHashMap<>();

  /**
   * Register (or replace) the default geometry for the pattern with the given id.
   */
  public void setPatternGeometry(FeedScopedId patternId, CompactLineStringSequence geometry) {
    patternGeometries.put(patternId, geometry);
  }

  /**
   * The default geometry registered for the pattern with the given id, or {@code null} if none
   * has been registered.
   */
  @Nullable
  public CompactLineStringSequence getPatternGeometry(FeedScopedId patternId) {
    return patternGeometries.get(patternId);
  }

  /**
   * Register (or replace) a per-trip override geometry. Callers should only call this when the
   * trip's geometry actually differs from its pattern's default, to avoid unnecessary duplicate
   * storage.
   */
  public void setTripGeometryOverride(FeedScopedId tripId, CompactLineStringSequence geometry) {
    tripGeometryOverrides.put(tripId, geometry);
  }

  /**
   * The per-trip override geometry registered for the trip with the given id, or {@code null} if
   * the trip has no override (i.e. it uses its pattern's default geometry).
   */
  @Nullable
  public CompactLineStringSequence getTripGeometryOverride(FeedScopedId tripId) {
    return tripGeometryOverrides.get(tripId);
  }

  /**
   * Whether a per-trip override geometry is registered for the given trip id.
   */
  public boolean hasTripGeometryOverride(FeedScopedId tripId) {
    return tripGeometryOverrides.containsKey(tripId);
  }

  /**
   * Remove the per-trip override geometry for the given trip id, if any. This is provided for a
   * future real-time updater that may need to clear a previously set override (e.g. when a trip
   * is cancelled or reverts to its scheduled shape).
   */
  public void removeTripGeometryOverride(FeedScopedId tripId) {
    tripGeometryOverrides.remove(tripId);
  }
}
