package org.opentripplanner.updater.trip.patterncache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.shape.TripShapeFactory;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.updater.trip.shape.RealTimeTripShapeFactory;

class TripPatternCacheTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final RegularStop STOP_A = TEST_MODEL.stop("A").withCoordinate(60.0, 24.0).build();
  private static final RegularStop STOP_X = TEST_MODEL.stop("X")
    .withCoordinate(60.05, 24.1)
    .build();
  private static final RegularStop STOP_B = TEST_MODEL.stop("B").withCoordinate(60.1, 24.0).build();
  private static final RegularStop STOP_C = TEST_MODEL.stop("C").withCoordinate(60.2, 24.0).build();

  private static final Trip TRIP = TransitRepositoryForTest.trip("T1").build();

  private static final TripPattern ORIGINAL = TripPattern.of(id("original"))
    .withRoute(TRIP.getRoute())
    .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
    .withShape(
      TripShapeFactory.fromLineStrings(
        List.of(
          makeLineString(STOP_A.getCoordinate(), STOP_X.getCoordinate(), STOP_B.getCoordinate()),
          makeLineString(STOP_B.getCoordinate(), STOP_C.getCoordinate())
        ),
        DeduplicatorService.NOOP
      )
    )
    .build();

  private final TripPatternCache subject = new TripPatternCache(
    new TripPatternIdGenerator(),
    new RealTimeTripShapeFactory(DeduplicatorService.NOOP)
  );

  @Test
  void newPatternGetsShapeDerivedFromTheOriginal() {
    // Skip stop C: the first hop is unchanged and keeps the shape of the original
    var stopPattern = TransitRepositoryForTest.stopPattern(STOP_A, STOP_B);

    var pattern = subject.getOrCreateTripPattern(stopPattern, TRIP, ORIGINAL);

    assertSame(ORIGINAL, pattern.getOriginalTripPattern());
    assertEquals(1, pattern.getShape().size());
    assertEquals(ORIGINAL.getShape().get(0), pattern.getShape().get(0));
  }

  @Test
  void newPatternWithoutOriginalGetsStraightLines() {
    var stopPattern = TransitRepositoryForTest.stopPattern(STOP_A, STOP_X, STOP_C);

    var pattern = subject.getOrCreateTripPattern(stopPattern, TRIP, null);

    assertEquals(2, pattern.getShape().size());
    assertEquals(
      makeLineString(STOP_A.getCoordinate(), STOP_X.getCoordinate()),
      pattern.getShape().get(0)
    );
    assertEquals(
      makeLineString(STOP_X.getCoordinate(), STOP_C.getCoordinate()),
      pattern.getShape().get(1)
    );
  }
}
