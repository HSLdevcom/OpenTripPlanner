package org.opentripplanner.updater.trip.gtfs.model;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.PolylineEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves standalone GTFS-RT {@code Shape} FeedEntities (self-contained shapes carried in the
 * same real-time message, identified by an encoded polyline) into a lookup by {@code shape_id}.
 * <p>
 * Per the GTFS-RT reference, a {@code Shape}'s {@code shape_id} is keyed against the entity's
 * {@code shape_id} field, <em>not</em> the enclosing {@code FeedEntity.id}.
 * <p>
 * Resolving {@code shape_id} against shapes already defined in static GTFS {@code shapes.txt} is
 * not supported.
 */
public class RealtimeShapes {

  private static final Logger LOG = LoggerFactory.getLogger(RealtimeShapes.class);

  private RealtimeShapes() {}

  public static Map<String, LineString> fromFeedEntities(List<FeedEntity> feedEntityList) {
    Map<String, LineString> shapesById = new HashMap<>();
    for (FeedEntity entity : feedEntityList) {
      if (!entity.hasShape()) {
        continue;
      }
      var shape = entity.getShape();
      if (!shape.hasShapeId() || !shape.hasEncodedPolyline()) {
        continue;
      }
      try {
        var coordinates = PolylineEncoder.decode(shape.getEncodedPolyline());
        if (coordinates.size() < 2) {
          LOG.warn(
            "Ignoring GTFS-RT Shape '{}' with fewer than 2 points in encoded_polyline",
            shape.getShapeId()
          );
          continue;
        }
        shapesById.put(shape.getShapeId(), GeometryUtils.makeLineString(coordinates));
      } catch (IllegalArgumentException e) {
        LOG.warn(
          "Ignoring GTFS-RT Shape '{}' with malformed encoded_polyline: {}",
          shape.getShapeId(),
          e.getMessage()
        );
      }
    }
    return shapesById;
  }
}
