package org.opentripplanner.street.geometry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

/**
 * Slices a full-trip shape {@link LineString} into one {@link LineString} per hop (the segment
 * between two consecutive stops), by snapping each stop onto the shape.
 * <p>
 * This is used both by the graph builder (GTFS {@code shapes.txt} without
 * {@code shape_dist_traveled}) and by real-time updaters resolving a GTFS-RT {@code Shape}
 * FeedEntity for a trip whose stop sequence may not match static GTFS (e.g. {@code REPLACEMENT}
 * or {@code NEW} trips).
 */
public class ShapeHopGeometrySlicer {

  private static final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();

  private ShapeHopGeometrySlicer() {}

  /**
   * Slice {@code shape} into {@code stopCoordinates.size() - 1} per-hop geometries, one for each
   * consecutive pair of stops.
   *
   * @param shape                    the full-trip shape to slice
   * @param stopCoordinates          ordered stop coordinates, one per stop in the trip
   * @param maxStopToShapeSnapDistance the maximum distance (in meters) a stop may be from the
   *                                 shape and still be snapped onto it; stops farther away fall
   *                                 back to being matched to the single nearest point on the shape
   * @param relaxedSnapping         if {@code true}, ignore {@code maxStopToShapeSnapDistance} and
   *                                 always snap every stop onto the shape (used for flex trips,
   *                                 which may have stops that are areas far from a corridor shape)
   * @return one {@link LineString} per hop, or {@code null} if the stops could not be consistently
   *     matched to the shape (e.g. a stop is far from the shape and out of order) — callers should
   *     fall back to straight-line hop geometries in that case
   */
  public static List<LineString> sliceIntoHopGeometries(
    LineString shape,
    List<Coordinate> stopCoordinates,
    double maxStopToShapeSnapDistance,
    boolean relaxedSnapping
  ) {
    List<LinearLocation> locations = getLinearLocations(
      shape,
      stopCoordinates,
      maxStopToShapeSnapDistance,
      relaxedSnapping
    );
    if (locations == null) {
      return null;
    }
    return getGeometriesByShape(shape, locations);
  }

  private static List<LineString> getGeometriesByShape(
    LineString shape,
    List<LinearLocation> locations
  ) {
    List<LineString> geoms = new ArrayList<>(locations.size() - 1);
    Iterator<LinearLocation> locationIt = locations.iterator();
    LinearLocation endLocation = locationIt.next();
    for (int i = 0; i < locations.size() - 1; ++i) {
      LinearLocation startLocation = endLocation;
      endLocation = locationIt.next();

      LocationIndexedLine locationIndexed = new LocationIndexedLine(shape);
      LineString geometry = (LineString) locationIndexed.extractLine(startLocation, endLocation);

      // Pack the resulting line string
      CoordinateSequence sequence = new PackedCoordinateSequence.Double(
        geometry.getCoordinates(),
        2
      );
      geoms.add(GEOMETRY_FACTORY.createLineString(sequence));
    }
    return geoms;
  }

  private static List<LinearLocation> getLinearLocations(
    LineString shape,
    List<Coordinate> stopCoordinates,
    double maxStopToShapeSnapDistance,
    boolean relaxedSnapping
  ) {
    // This trip does not have shape_dist in stop_times, but does have an associated shape.
    ArrayList<IndexedLineSegment> segments = new ArrayList<>();
    for (int i = 0; i < shape.getNumPoints() - 1; ++i) {
      segments.add(new IndexedLineSegment(i, shape.getCoordinateN(i), shape.getCoordinateN(i + 1)));
    }
    // Find possible segment matches for each stop.
    List<List<IndexedLineSegment>> possibleSegmentsForStop = new ArrayList<>();
    int minSegmentIndex = 0;
    for (Coordinate coord : stopCoordinates) {
      List<IndexedLineSegment> stopSegments = new ArrayList<>();
      double bestDistance = Double.MAX_VALUE;
      IndexedLineSegment bestSegment = null;
      int maxSegmentIndex = -1;
      int index = -1;
      int minSegmentIndexForThisStop = -1;
      for (IndexedLineSegment segment : segments) {
        index++;
        if (segment.index < minSegmentIndex) {
          continue;
        }
        double distance = segment.distance(coord);
        if (distance < maxStopToShapeSnapDistance || relaxedSnapping) {
          stopSegments.add(segment);
          maxSegmentIndex = index;
          if (minSegmentIndexForThisStop == -1) {
            minSegmentIndexForThisStop = index;
          }
        } else if (distance < bestDistance) {
          bestDistance = distance;
          bestSegment = segment;
          if (maxSegmentIndex != -1) {
            maxSegmentIndex = index;
          }
        }
      }
      if (stopSegments.size() == 0 && bestSegment != null) {
        //no segments within maxStopToShapeSnapDistance
        //fall back to nearest segment
        stopSegments.add(bestSegment);
        minSegmentIndex = bestSegment.index;
      } else {
        minSegmentIndex = minSegmentIndexForThisStop;
      }

      for (int j = possibleSegmentsForStop.size() - 1; j >= 0; j--) {
        for (
          Iterator<IndexedLineSegment> it = possibleSegmentsForStop.get(j).iterator();
          it.hasNext();

        ) {
          IndexedLineSegment segment = it.next();
          if (segment.index > maxSegmentIndex) {
            it.remove();
          }
        }
      }
      possibleSegmentsForStop.add(stopSegments);
    }

    return getStopLocations(possibleSegmentsForStop, stopCoordinates);
  }

  /**
   * Find a consistent, increasing list of LinearLocations along a shape for a set of stops.
   * Handles loop routes.
   */
  private static List<LinearLocation> getStopLocations(
    List<List<IndexedLineSegment>> possibleSegmentsForStop,
    List<Coordinate> stopCoordinates
  ) {
    IndexedLineSegment prevSegment = null;
    var prevSegmentFraction = 0.0;
    List<LinearLocation> locations = new ArrayList<>(stopCoordinates.size());
    for (
      var stopPositionInPattern = 0;
      stopPositionInPattern < stopCoordinates.size();
      ++stopPositionInPattern
    ) {
      Coordinate stopCoord = stopCoordinates.get(stopPositionInPattern);

      // Arrange segments into list of continuous segments
      // we assume that the first time a shape passes through within maxStopToShapeSnapDistance of
      // the stop, it will match the stop. Therefore, we choose the best segment within the first
      // list of continuous segments, rather than trying from the best segment globally.
      // This is to avoid exponential complexity for routes with multiple double-backs with
      // multiple stops within the double-backs.
      //
      // An exception is that, if the discontinuity appears before the minimum possible segment
      // for the next stop, the discontinuity is joined together. This is avoid a simple edge case
      // of a bus first passing within maxStopToShapeSnapDistance of a stop, exit that radius to a
      // turning circle, then call at the stop at the opposite side of the road.
      List<List<IndexedLineSegment>> continuousSegments = new LinkedList<>();
      for (IndexedLineSegment segment : possibleSegmentsForStop.get(stopPositionInPattern)) {
        //can't go backwards along line
        if (prevSegment == null || segment.index >= prevSegment.index) {
          // can't go backwards in the same segment
          if (prevSegment != null && segment.index == prevSegment.index) {
            var splitX = segment.start.x + (segment.end.x - segment.start.x) * prevSegmentFraction;
            var splitY = segment.start.y + (segment.end.y - segment.start.y) * prevSegmentFraction;
            var splitZ = segment.start.z + (segment.end.z - segment.start.z) * prevSegmentFraction;
            segment = new IndexedLineSegment(
              segment.index,
              new Coordinate(splitX, splitY, splitZ),
              segment.end
            );
          }
          boolean shouldStartNewSegment;
          if (continuousSegments.isEmpty()) {
            shouldStartNewSegment = true;
          } else if (stopPositionInPattern + 1 == stopCoordinates.size()) {
            shouldStartNewSegment = false;
          } else {
            var lastSegment = continuousSegments.getLast().getLast();
            var segmentsForNextStop = possibleSegmentsForStop.get(stopPositionInPattern + 1);
            var s = segment;
            shouldStartNewSegment = segmentsForNextStop
              .stream()
              .anyMatch(item -> item.index > lastSegment.index && item.index < s.index);
          }
          if (shouldStartNewSegment) {
            // start a new continuous segment
            continuousSegments.add(new LinkedList<>());
          }
          continuousSegments.getLast().add(segment);
        }
      }
      // choose the best match from the first list
      if (continuousSegments.isEmpty()) {
        return null;
      }
      List<IndexedLineSegment> firstContinuousSegments = continuousSegments.getFirst();
      var bestMatch = firstContinuousSegments.getFirst();
      for (var segment : firstContinuousSegments) {
        if (segment.distance(stopCoord) < bestMatch.distance(stopCoord)) {
          bestMatch = segment;
        }
      }
      // we found one!
      // best match may be the split segment with the previous stop, in this case we need to load
      // the full segment
      IndexedLineSegment matchedSegment = prevSegment != null &&
        bestMatch.index == prevSegment.index
        ? prevSegment
        : bestMatch;
      prevSegmentFraction = matchedSegment.fraction(stopCoord);
      LinearLocation location = new LinearLocation(0, bestMatch.index, prevSegmentFraction);
      locations.add(location);
      prevSegment = matchedSegment;
    }
    return locations;
  }
}
