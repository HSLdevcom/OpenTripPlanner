package org.opentripplanner.service.shape.model;

/**
 * Where the geometry of a hop, or of a whole trip, came from.
 * <p>
 * This is deliberately part of the read API rather than an internal detail: several of the sources
 * below are <em>synthesized</em> rather than real, published geometry, and consumers — API clients,
 * data-quality reports, debugging tools — need to be able to tell the difference. Today that
 * distinction is invisible: a pattern built without shape data returns straight lines that are
 * indistinguishable from a real shape that happens to be straight.
 * <p>
 * PROVISIONAL: this type is part of the initial {@code ShapeService} API design and may change
 * before the service is wired up.
 */
public enum GeometrySource {
  /**
   * The geometry of the trip pattern itself, used because the trip has no geometry of its own.
   * <p>
   * For NeTEx this is the only possible source of real geometry, because ServiceLink projections
   * are attached to the JourneyPattern and shared by every ServiceJourney on it.
   */
  PATTERN_DEFAULT,

  /**
   * A per-trip geometry that came from the scheduled data, i.e. a GTFS trip whose {@code shape_id}
   * differs from the shape chosen as the pattern default.
   * <p>
   * GTFS-only. NeTEx has no per-ServiceJourney equivalent.
   */
  STATIC_TRIP_SHAPE,

  /** A per-trip geometry supplied or derived by a real-time update. */
  REAL_TIME,

  /**
   * No shape data was available, so the geometry was synthesized as straight lines between
   * consecutive stops. Distances for such hops are measured with the haversine formula rather than
   * summed along a shape.
   */
  STRAIGHT_LINE,
}
