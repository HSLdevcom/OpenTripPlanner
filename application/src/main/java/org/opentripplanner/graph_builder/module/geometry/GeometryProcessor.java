package org.opentripplanner.graph_builder.module.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.BogusShapeDistanceTraveled;
import org.opentripplanner.graph_builder.issues.BogusShapeGeometry;
import org.opentripplanner.graph_builder.issues.BogusShapeGeometryCaught;
import org.opentripplanner.graph_builder.issues.ShapeGeometryTooFar;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.ShapeHopGeometrySlicer;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This module creates hop geometries from GTFS shapes.
 *
 * <p>
 * THREAD SAFETY The computation runs in parallel so be careful about thread safety when modifying
 * the logic here.
 */
public class GeometryProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(GeometryProcessor.class);
  private static final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();
  private final TransitDataImportBuilder builder;
  // this is a thread-safe implementation
  private final Map<ShapeSegmentKey, LineString> geometriesByShapeSegmentKey =
    new ConcurrentHashMap<>();
  // this is a thread-safe implementation
  private final Map<FeedScopedId, LineString> geometriesByShapeId = new ConcurrentHashMap<>();
  // this is a thread-safe implementation
  private final Map<FeedScopedId, double[]> distancesByShapeId = new ConcurrentHashMap<>();
  private final double maxStopToShapeSnapDistance;
  private final DataImportIssueStore issueStore;

  public GeometryProcessor(
    TransitDataImportBuilder builder,
    double maxStopToShapeSnapDistance,
    DataImportIssueStore issueStore
  ) {
    this.builder = builder;
    this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance > 0
      ? maxStopToShapeSnapDistance
      : 150;
    this.issueStore = issueStore;
  }

  /**
   * Generate the per-hop geometry for the trip. The returned list is never null and always
   * contains one entry per hop ({@code stopTimes.size() - 1}). When the trip has no usable
   * shape data, the hops fall back to straight lines between consecutive stops, mirroring the
   * NeTEx {@link org.opentripplanner.netex.mapping.ServiceLinkMapper} behaviour. Assumes that
   * there are already vertices in the graph for the stops.
   * <p>
   * THREAD SAFETY The geometries for the trip patterns are computed in parallel. The collections
   * needed for this are concurrent implementations and therefore threadsafe but the issue store,
   * the graph, the TransitDataImport and others are not.
   */
  public List<LineString> createHopGeometries(Trip trip) {
    List<StopTime> stopTimes = builder.getStopTimesSortedByTrip().get(trip);
    if (
      trip.getShapeId() == null ||
      trip.getShapeId().getId() == null ||
      trip.getShapeId().getId().isEmpty()
    ) {
      return Arrays.asList(createStraightLineHopGeometries(stopTimes));
    }

    return Arrays.asList(createGeometry(trip.getShapeId(), stopTimes));
  }

  private static boolean equals(LinearLocation startIndex, LinearLocation endIndex) {
    return (
      startIndex.getSegmentIndex() == endIndex.getSegmentIndex() &&
      startIndex.getSegmentFraction() == endIndex.getSegmentFraction() &&
      startIndex.getComponentIndex() == endIndex.getComponentIndex()
    );
  }

  /**
   * Creates a set of geometries for a single trip, considering the GTFS shapes.txt, The geometry is
   * broken down into one geometry per inter-stop segment ("hop"). We also need a shape for the
   * entire trip and tripPattern, but given the complexity of the existing code for generating hop
   * geometries, we will create the full-trip geometry by simply concatenating the hop geometries.
   * <p>
   * This geometry will in fact be used for an entire set of trips in a trip pattern. Technically
   * one of the trips with exactly the same sequence of stops could follow a different route on the
   * streets, but that's very uncommon.
   */
  private LineString[] createGeometry(FeedScopedId shapeId, List<StopTime> stopTimes) {
    if (hasShapeDist(shapeId, stopTimes)) {
      // this trip has shape_dist in stop_times
      LineString[] geometries = getHopGeometriesViaShapeDistTravelled(stopTimes, shapeId);
      if (geometries != null) {
        return geometries;
      }
      // else proceed to method below which uses shape without distance information
    }

    LineString shapeLineString = getLineStringForShapeId(shapeId);
    if (shapeLineString == null) {
      // this trip has a shape_id, but no such shape exists, and no shape_dist in stop_times
      // create straight line segments between stops for each hop
      issueStore.add(
        "InvalidShapeReference",
        "Trip '%s' refers to unknown shape geometry '%s'",
        stopTimes.get(0).getTrip().getId(),
        shapeId
      );
      return createStraightLineHopGeometries(stopTimes);
    }

    var isFlexTrip = FlexTrip.containsFlexStops(stopTimes);
    List<Coordinate> stopCoordinates = stopTimes
      .stream()
      .map(st -> st.getStop().getCoordinate().asJtsCoordinate())
      .toList();
    List<LineString> hopGeometries = ShapeHopGeometrySlicer.sliceIntoHopGeometries(
      shapeLineString,
      stopCoordinates,
      maxStopToShapeSnapDistance,
      isFlexTrip
    );
    if (hopGeometries == null) {
      // this only happens on shape which have points very far from
      // their stop sequence. So we'll fall back to trivial stop-to-stop
      // linking, even though theoretically we could do better.
      issueStore.add(new ShapeGeometryTooFar(stopTimes.get(0).getTrip().getId(), shapeId));
      return createStraightLineHopGeometries(stopTimes);
    }

    return hopGeometries.toArray(new LineString[0]);
  }

  private boolean hasShapeDist(FeedScopedId shapeId, List<StopTime> stopTimes) {
    StopTime st0 = stopTimes.get(0);
    return st0.isShapeDistTraveledSet() && getDistanceForShapeId(shapeId) != null;
  }

  private LineString[] createStraightLineHopGeometries(List<StopTime> stopTimes) {
    LineString[] geoms = new LineString[stopTimes.size() - 1];
    StopTime st0;
    for (int i = 0; i < stopTimes.size() - 1; ++i) {
      st0 = stopTimes.get(i);
      StopTime st1 = stopTimes.get(i + 1);
      LineString geometry = createSimpleGeometry(st0.getStop(), st1.getStop());
      geoms[i] = geometry;
    }
    return geoms;
  }

  private LineString[] getHopGeometriesViaShapeDistTravelled(
    List<StopTime> stopTimes,
    FeedScopedId shapeId
  ) {
    LineString[] geoms = new LineString[stopTimes.size() - 1];
    StopTime st0;
    for (int i = 0; i < stopTimes.size() - 1; ++i) {
      st0 = stopTimes.get(i);
      StopTime st1 = stopTimes.get(i + 1);
      geoms[i] = getHopGeometryViaShapeDistTraveled(shapeId, st0, st1);
      if (geoms[i] == null) {
        return null;
      }
    }
    return geoms;
  }

  private LineString getHopGeometryViaShapeDistTraveled(
    FeedScopedId shapeId,
    StopTime st0,
    StopTime st1
  ) {
    double startDistance = st0.getShapeDistTraveled();
    double endDistance = st1.getShapeDistTraveled();

    ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);
    LineString geometry = geometriesByShapeSegmentKey.get(key);
    if (geometry != null) {
      return geometry;
    }

    double[] distances = getDistanceForShapeId(shapeId);

    if (distances == null) {
      issueStore.add(new BogusShapeGeometry(shapeId));
      return null;
    } else {
      LinearLocation startIndex = getSegmentFraction(distances, startDistance);
      LinearLocation endIndex = getSegmentFraction(distances, endDistance);

      if (equals(startIndex, endIndex)) {
        //bogus shape_dist_traveled
        issueStore.add(new BogusShapeDistanceTraveled(st1));
        // return null to indicate failure. Another approach which does not need shape_dist_traveled will be used.
        return null;
      }
      LineString line = getLineStringForShapeId(shapeId);
      LocationIndexedLine lol = new LocationIndexedLine(line);

      geometry = getSegmentGeometry(
        shapeId,
        lol,
        startIndex,
        endIndex,
        startDistance,
        endDistance,
        st0,
        st1
      );

      return geometry;
    }
  }

  /** create a 2-point linestring (a straight line segment) between the two stops */
  private LineString createSimpleGeometry(StopLocation s0, StopLocation s1) {
    Coordinate[] coordinates = new Coordinate[] {
      s0.getCoordinate().asJtsCoordinate(),
      s1.getCoordinate().asJtsCoordinate(),
    };
    CoordinateSequence sequence = new PackedCoordinateSequence.Double(coordinates, 2);

    return GEOMETRY_FACTORY.createLineString(sequence);
  }

  private boolean isValid(Geometry geometry, StopLocation s0, StopLocation s1) {
    Coordinate[] coordinates = geometry.getCoordinates();
    if (coordinates.length < 2) {
      return false;
    }
    if (geometry.getLength() == 0) {
      return false;
    }
    for (Coordinate coordinate : coordinates) {
      if (Double.isNaN(coordinate.x) || Double.isNaN(coordinate.y)) {
        return false;
      }
    }
    Coordinate geometryStartCoord = coordinates[0];
    Coordinate geometryEndCoord = coordinates[coordinates.length - 1];

    Coordinate startCoord = s0.getCoordinate().asJtsCoordinate();
    Coordinate endCoord = s1.getCoordinate().asJtsCoordinate();
    if (
      SphericalDistanceLibrary.fastDistance(startCoord, geometryStartCoord) >
      maxStopToShapeSnapDistance
    ) {
      return false;
    } else if (
      SphericalDistanceLibrary.fastDistance(endCoord, geometryEndCoord) > maxStopToShapeSnapDistance
    ) {
      return false;
    }
    return true;
  }

  private LineString getSegmentGeometry(
    FeedScopedId shapeId,
    LocationIndexedLine locationIndexedLine,
    LinearLocation startIndex,
    LinearLocation endIndex,
    double startDistance,
    double endDistance,
    StopTime st0,
    StopTime st1
  ) {
    ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);

    LineString geometry = geometriesByShapeSegmentKey.get(key);
    if (geometry == null) {
      geometry = (LineString) locationIndexedLine.extractLine(startIndex, endIndex);

      // Pack the resulting line string
      CoordinateSequence sequence = new PackedCoordinateSequence.Double(
        geometry.getCoordinates(),
        2
      );
      geometry = GEOMETRY_FACTORY.createLineString(sequence);

      if (!isValid(geometry, st0.getStop(), st1.getStop())) {
        issueStore.add(new BogusShapeGeometryCaught(shapeId, st0, st1));
        return null;
      }
      geometriesByShapeSegmentKey.put(key, geometry);
    }

    return geometry;
  }

  /**
   * If a shape appears in more than one feed, the shape points will be loaded several times, and
   * there will be duplicates in the DAO. Filter out duplicates and repeated coordinates because 1)
   * they are unnecessary, and 2) they define 0-length line segments which cause JTS location
   * indexed line to return a segment location of NaN, which we do not want.
   */
  private Collection<ShapePoint> getUniqueShapePointsForShapeId(FeedScopedId shapeId) {
    var points = builder.getShapePoints().getOrDefault(shapeId, List.of());
    ArrayList<ShapePoint> filtered = new ArrayList<>();
    ShapePoint last = null;
    int currentSeq = Integer.MIN_VALUE;
    for (ShapePoint sp : points) {
      if (sp.sequence() < currentSeq) {
        // this should never happen, because the GTFS import should make sure they are already in order.
        // therefore this just a safety check to detect a programmer error.
        throw new IllegalStateException(
          "Shape %s is not sorted in order of sequence. This indicates a bug in OTP.".formatted(
            shapeId
          )
        );
      }
      if (last == null || last.sequence() != sp.sequence()) {
        if (last != null && last.sameCoordinates(sp)) {
          LOG.trace("pair of identical shape points (skipping): {} {}", last, sp);
        } else {
          filtered.add(sp);
        }
      }
      last = sp;
      currentSeq = sp.sequence();
    }
    return filtered;
  }

  private LineString getLineStringForShapeId(FeedScopedId shapeId) {
    LineString geometry = geometriesByShapeId.get(shapeId);

    if (geometry != null) {
      return geometry;
    }

    var points = getUniqueShapePointsForShapeId(shapeId);
    if (points.size() < 2) {
      return null;
    }
    Coordinate[] coordinates = new Coordinate[points.size()];
    double[] distances = new double[points.size()];

    boolean hasAllDistances = true;

    int i = 0;
    for (ShapePoint point : points) {
      coordinates[i] = point.coordinate();
      distances[i] = point.distTraveled();
      if (!point.isDistTraveledSet()) {
        hasAllDistances = false;
      }
      i++;
    }

    CoordinateSequence sequence = new PackedCoordinateSequence.Double(coordinates, 2);
    geometry = GEOMETRY_FACTORY.createLineString(sequence);
    geometriesByShapeId.put(shapeId, geometry);

    // If we don't have distances here, we can't calculate them ourselves because we can't
    // assume the units will match
    if (hasAllDistances) {
      distancesByShapeId.put(shapeId, distances);
    }

    return geometry;
  }

  private double[] getDistanceForShapeId(FeedScopedId shapeId) {
    getLineStringForShapeId(shapeId);
    return distancesByShapeId.get(shapeId);
  }

  private LinearLocation getSegmentFraction(double[] distances, double distance) {
    int index = Arrays.binarySearch(distances, distance);
    if (index < 0) {
      index = -(index + 1);
    }
    if (index == 0) {
      return new LinearLocation(0, 0.0);
    }
    if (index == distances.length) {
      return new LinearLocation(distances.length, 0.0);
    }

    double prevDistance = distances[index - 1];
    if (prevDistance == distances[index]) {
      return new LinearLocation(index - 1, 1.0);
    }
    double indexPart = (distance - distances[index - 1]) / (distances[index] - prevDistance);
    return new LinearLocation(index - 1, indexPart);
  }
}
