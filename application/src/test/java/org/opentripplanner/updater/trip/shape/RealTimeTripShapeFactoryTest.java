package org.opentripplanner.updater.trip.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.shape.CompactLineStringSequence;
import org.opentripplanner.transit.model.shape.TripShapeFactory;
import org.opentripplanner.transit.model.site.RegularStop;

class RealTimeTripShapeFactoryTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final RegularStop STOP_A = TEST_MODEL.stop("A").build();
  private static final RegularStop STOP_X = TEST_MODEL.stop("X").build();
  private static final RegularStop STOP_B = TEST_MODEL.stop("B").build();
  private static final RegularStop STOP_Y = TEST_MODEL.stop("Y").build();
  private static final RegularStop STOP_C = TEST_MODEL.stop("C").build();

  private static final StopPattern ORIGINAL_STOP_PATTERN = TransitRepositoryForTest.stopPattern(
    STOP_A,
    STOP_B,
    STOP_C
  );

  private static final CompactLineStringSequence ORIGINAL_TRIP_PATTERN_SHAPE =
    TripShapeFactory.fromLineStrings(
      List.of(
        makeLineString(STOP_A.getCoordinate(), STOP_X.getCoordinate(), STOP_B.getCoordinate()),
        makeLineString(STOP_B.getCoordinate(), STOP_Y.getCoordinate(), STOP_C.getCoordinate())
      ),
      DeduplicatorService.NOOP
    );

  private final RealTimeTripShapeFactory subject = new RealTimeTripShapeFactory(
    DeduplicatorService.NOOP
  );

  @Test
  void hopShapeForReplacementPattern() {
    var shape = subject.createTripPatternShape(
      TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_X, STOP_Y),
      ORIGINAL_STOP_PATTERN,
      ORIGINAL_TRIP_PATTERN_SHAPE
    );

    assertEquals(3, shape.size());
    assertEquals(
      makeLineString(STOP_A.getCoordinate(), STOP_X.getCoordinate(), STOP_B.getCoordinate()),
      shape.get(0)
    );
    assertEquals(makeLineString(STOP_B.getCoordinate(), STOP_X.getCoordinate()), shape.get(1));
    assertEquals(makeLineString(STOP_X.getCoordinate(), STOP_Y.getCoordinate()), shape.get(2));
  }

  @Test
  void copyHopsFromTheOriginalWhenStopsAreUnchanged() {
    var shape = subject.createTripPatternShape(
      ORIGINAL_STOP_PATTERN,
      ORIGINAL_STOP_PATTERN,
      ORIGINAL_TRIP_PATTERN_SHAPE
    );

    assertEquals(ORIGINAL_TRIP_PATTERN_SHAPE.distanceBetween(0, 2), shape.distanceBetween(0, 2));
    assertEquals(ORIGINAL_TRIP_PATTERN_SHAPE.get(0), shape.get(0));
    assertEquals(ORIGINAL_TRIP_PATTERN_SHAPE.get(1), shape.get(1));
  }

  /**
   * The original pattern has one hop with an extra coordinate between the stops. The replacement
   * uses an alternative stop in the same station as the first stop, so the old geometry is kept
   * and only the first point is moved to the new stop.
   */
  @Test
  void patchTheEndsOfTheOriginalHopWhenStopsAreInTheSameStation() {
    var stationOrigin = TEST_MODEL.station("S1").withCoordinate(0.0, 0.0).build();
    var stationDestination = TEST_MODEL.station("S2").withCoordinate(1.0, 1.0).build();
    var stopOrigin = TEST_MODEL.stop("A1")
      .withCoordinate(0.1, 0.1)
      .withParentStation(stationOrigin)
      .build();
    var stopNewOrigin = TEST_MODEL.stop("A2")
      .withCoordinate(0.2, 0.2)
      .withParentStation(stationOrigin)
      .build();
    var stopDestination = TEST_MODEL.stop("D")
      .withCoordinate(0.9, 0.9)
      .withParentStation(stationDestination)
      .build();
    var middle = new WgsCoordinate(0.5, 0.5);
    var originalShape = TripShapeFactory.fromLineStrings(
      List.of(makeLineString(stopOrigin.getCoordinate(), middle, stopDestination.getCoordinate())),
      DeduplicatorService.NOOP
    );

    var shape = subject.createTripPatternShape(
      TransitRepositoryForTest.stopPattern(stopNewOrigin, stopDestination),
      TransitRepositoryForTest.stopPattern(stopOrigin, stopDestination),
      originalShape
    );

    assertEquals(
      makeLineString(stopNewOrigin.getCoordinate(), middle, stopDestination.getCoordinate()),
      shape.get(0)
    );
    // The original shape is not modified
    assertEquals(
      makeLineString(stopOrigin.getCoordinate(), middle, stopDestination.getCoordinate()),
      originalShape.get(0)
    );
  }
}
