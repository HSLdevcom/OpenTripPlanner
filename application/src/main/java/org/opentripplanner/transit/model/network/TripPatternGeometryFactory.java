package org.opentripplanner.transit.model.network;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.CompactLineStringSequence;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Builds the compacted per-hop geometry sequence ({@link CompactLineStringSequence}) used to
 * represent a {@link TripPattern}'s (or an individual trip's) stop-to-stop geometry, together with
 * its cumulative arc-length table.
 * <p>
 * This logic was extracted from {@link TripPatternBuilder} so it can be shared by the
 * {@code TripPatternGeometryService}/{@code TripPatternGeometryRepository} pair (which store both
 * a default geometry per pattern and, optionally, per-trip override geometries) without
 * duplicating the straight-line-fallback and distance-accumulation rules.
 */
public final class TripPatternGeometryFactory {

  private TripPatternGeometryFactory() {}

  /**
   * Build the compacted per-hop geometry sequence for a pattern, along with its cumulative
   * arc-length table. When {@code hopGeometries} is non-null each hop's distance is the planar
   * sum of its {@link LineString} coordinates ({@link GeometryUtils#sumDistances}); when it is
   * {@code null} each hop is synthesized as a straight line between consecutive stops and the
   * distance is measured with {@link SphericalDistanceLibrary} (haversine).
   * <p>
   * Distances are accumulated in {@code double} and rounded to the nearest meter only when
   * writing each entry of the cumulative table, which bounds the rounding error of any
   * {@code distanceBetween(board, alight)} query to at most 1 meter independent of leg length.
   */
  public static CompactLineStringSequence buildHopGeometries(
    StopPattern stopPattern,
    @Nullable List<LineString> hopGeometries
  ) {
    int numberOfStops = stopPattern.getSize();
    int expectedHops = Math.max(numberOfStops - 1, 0);
    if (hopGeometries != null && hopGeometries.size() != expectedHops) {
      throw new IllegalArgumentException(
        "hopGeometries size (%d) does not match the number of hops in the stop pattern (%d)".formatted(
          hopGeometries.size(),
          expectedHops
        )
      );
    }
    // Cumulative table has one entry per vertex position (0 .. expectedHops). For a degenerate
    // pattern with 0 or 1 stops this is just {0}.
    int cumulativeLength = expectedHops + 1;
    double[] cumulativeDouble = new double[cumulativeLength];
    List<LineString> hops = new ArrayList<>(expectedHops);

    if (hopGeometries != null) {
      for (int i = 0; i < hopGeometries.size(); i++) {
        LineString hop = hopGeometries.get(i);
        cumulativeDouble[i + 1] =
          cumulativeDouble[i] + GeometryUtils.sumDistances(hop.getCoordinateSequence());
        hops.add(hop);
      }
    } else {
      for (int i = 0; i < numberOfStops - 1; i++) {
        StopLocation from = stopPattern.getStop(i);
        StopLocation to = stopPattern.getStop(i + 1);
        LineString hop = GeometryUtils.makeLineString(from.getCoordinate(), to.getCoordinate());
        cumulativeDouble[i + 1] =
          cumulativeDouble[i] +
          SphericalDistanceLibrary.distance(from.getLat(), from.getLon(), to.getLat(), to.getLon());
        hops.add(hop);
      }
    }

    int[] cumulativeMeters = new int[cumulativeLength];
    for (int i = 0; i < cumulativeLength; i++) {
      cumulativeMeters[i] = (int) Math.round(cumulativeDouble[i]);
    }
    return CompactLineStringSequence.of(hops, cumulativeMeters);
  }

  /**
   * Derive per-hop geometries for {@code newStopPattern} by reusing as much of
   * {@code originalGeometry} (the already-registered geometry of {@code originalStopPattern}) as
   * possible. This is used when a real-time update replaces a pattern's stop sequence (e.g. a
   * modified or replacement trip) and no shape data is available for the new sequence.
   * <p>
   * For each hop in the new pattern:
   * <ul>
   *   <li>if both endpoints are exactly the same stops as in the original pattern at that hop
   *   index, the original hop geometry is reused unchanged;</li>
   *   <li>otherwise, if both endpoints belong to the same stations as in the original pattern at
   *   that hop index, the original hop geometry is reused but with its first/last coordinate
   *   patched to the new stops' locations;</li>
   *   <li>otherwise (no original hop at that index, or the stops/stations differ), a straight
   *   line between the new hop's stops is used.</li>
   * </ul>
   * The returned list always has exactly {@code newStopPattern.getSize() - 1} entries (one per
   * hop), suitable for passing to {@link #buildHopGeometries(StopPattern, List)}.
   */
  public static List<LineString> deriveHopGeometriesFromOriginal(
    StopPattern newStopPattern,
    StopPattern originalStopPattern,
    CompactLineStringSequence originalGeometry
  ) {
    List<LineString> hopGeometries = new ArrayList<>();
    int originalHopCount = originalGeometry.size();

    for (int i = 0; i < newStopPattern.getSize() - 1; i++) {
      LineString originalHopGeometry = i < originalHopCount ? originalGeometry.get(i) : null;

      if (originalHopGeometry != null && newStopPattern.sameStops(originalStopPattern, i)) {
        // Copy hop geometry from the original pattern unchanged.
        hopGeometries.add(originalHopGeometry);
      } else if (
        originalHopGeometry != null && newStopPattern.sameStations(originalStopPattern, i)
      ) {
        // Use old geometry but patch first and last point with the new stops.
        var newStart = newStopPattern.getStop(i).getCoordinate().asJtsCoordinate();
        var newEnd = newStopPattern.getStop(i + 1).getCoordinate().asJtsCoordinate();

        Coordinate[] coordinates = originalHopGeometry.getCoordinates().clone();
        coordinates[0].setCoordinate(newStart);
        coordinates[coordinates.length - 1].setCoordinate(newEnd);

        hopGeometries.add(GeometryUtils.getGeometryFactory().createLineString(coordinates));
      } else {
        // Create new straight-line geometry for hop.
        hopGeometries.add(
          GeometryUtils.makeLineString(
            newStopPattern.getStop(i).getCoordinate(),
            newStopPattern.getStop(i + 1).getCoordinate()
          )
        );
      }
    }
    return hopGeometries;
  }
}
