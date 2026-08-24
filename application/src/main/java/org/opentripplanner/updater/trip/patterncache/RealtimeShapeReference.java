package org.opentripplanner.updater.trip.patterncache;

import org.locationtech.jts.geom.LineString;

/**
 * A GTFS-RT {@code Shape} resolved for a specific trip update, pairing the {@code shape_id} (used
 * to key the {@link TripPatternCache} so that trips sharing a stop pattern but not a shape don't
 * collide) with the decoded shape geometry (used to slice per-hop geometries).
 */
public record RealtimeShapeReference(String shapeId, LineString geometry) {}
