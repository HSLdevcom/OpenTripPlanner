package org.opentripplanner.service.shape.model;

import org.locationtech.jts.geom.LineString;

/**
 * The geometry of a single hop: the path a vehicle takes between two consecutive stops.
 * <p>
 * <b>This is the unit of deduplication.</b> OTP stores transit geometry as per-hop segments that are
 * concatenated on demand, and hops — not whole shapes — are what different trips and patterns
 * actually have in common:
 * <ul>
 *   <li>a per-trip override that diverges from the pattern default in one hop shares every other
 *       hop with it;</li>
 *   <li>two patterns on the same corridor (for example a full run and a short-turn variant) share
 *       the hops they have in common, even though neither whole shape matches;</li>
 *   <li>two trips on the same shape but with different stop patterns still share every hop whose
 *       endpoints coincide.</li>
 * </ul>
 * Interning whole shapes would capture only the last of these, and only when the stop patterns match
 * exactly.
 * <p>
 * Implementations are therefore expected to be immutable, to store a compacted form of the geometry
 * rather than a decoded {@link LineString}, and to implement {@code equals}/{@code hashCode} over
 * that compacted form so instances can be interned. Interning itself is a storage invariant of the
 * repository, not something callers participate in — see
 * {@link org.opentripplanner.service.shape.ShapeRepository}.
 * <p>
 * The compacted storage form is deliberately absent from this interface. It is an implementation
 * detail, and keeping it out means the model does not depend on the internals of the {@code street}
 * module's geometry packing.
 * <p>
 * PROVISIONAL: this type is part of the initial {@code ShapeService} API design and may change
 * before the service is wired up.
 */
public interface HopGeometry {
  /**
   * Decode and return the path between the two stops.
   * <p>
   * Implementations are expected to decode on each call rather than cache: the compacted form exists
   * precisely so that the decoded geometry does not have to be retained, and most hops are never
   * decoded at all.
   */
  LineString geometry();

  /**
   * Length of this hop along the geometry, in meters, rounded to the nearest meter.
   * <p>
   * Stored rather than derived, so that the cumulative distance table of a
   * {@link HopGeometrySequence} can be assembled from interned hops without decoding any of them.
   */
  int lengthMeters();

  /**
   * How this hop's geometry was produced. {@link GeometrySource#STRAIGHT_LINE} means it was
   * synthesized from the two stop coordinates because no shape data covered it.
   */
  GeometrySource source();
}
