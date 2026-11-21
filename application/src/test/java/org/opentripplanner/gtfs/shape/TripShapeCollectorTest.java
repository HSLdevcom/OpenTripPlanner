package org.opentripplanner.gtfs.shape;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.transit.model._data.TransitRepositoryForTest.trip;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.geometry.GeometryProcessor;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;

class TripShapeCollectorTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final RegularStop STOP_A = TEST_MODEL.stop("A", 60.0, 10.0).build();
  private static final RegularStop STOP_B = TEST_MODEL.stop("B", 60.01, 10.0).build();
  private static final StopPattern STOP_PATTERN = TransitRepositoryForTest.stopPattern(
    STOP_A,
    STOP_B
  );
  private static final TripPattern PATTERN = TransitRepositoryForTest.tripPattern(
    "pattern",
    TransitRepositoryForTest.route("route").build()
  )
    .withStopPattern(STOP_PATTERN)
    .build();

  /** The number of points of a straight line between the two stops. */
  private static final int STRAIGHT_LINE = 2;
  /** The number of points of the detour shape between the two stops. */
  private static final int DETOUR = 3;
  private static final FeedScopedId DETOUR_SHAPE_ID = id("detour");

  private final TransitDataImportBuilder dataImport = new TransitDataImportBuilder(
    TEST_MODEL.siteRepositoryBuilder().withRegularStop(STOP_A).withRegularStop(STOP_B).build(),
    DataImportIssueStore.NOOP
  );
  private final TripShapeCollector subject = new TripShapeCollector(
    new GeometryProcessor(dataImport, 100, 0.0, DataImportIssueStore.NOOP),
    DeduplicatorService.NOOP
  );

  @BeforeEach
  void setup() {
    dataImport
      .getShapePoints()
      .put(
        DETOUR_SHAPE_ID,
        List.of(
          new ShapePoint(1, 60.0, 10.0, null),
          new ShapePoint(2, 60.005, 10.001, null),
          new ShapePoint(3, 60.01, 10.0, null)
        )
      );
  }

  @Test
  void shapeUsedByMostTripsBecomesTheTripPatternShape() {
    addTrip(trip("straightLine1").build());
    addTrip(trip("straightLine2").build());
    addTrip(trip("detour").withShapeId(DETOUR_SHAPE_ID).build());

    subject.addShapesToDataImport(PATTERN, dataImport);

    var tripPatternShape = dataImport.getShapesByTripPattern().get(PATTERN.getId());
    assertEquals(STRAIGHT_LINE, tripPatternShape.get(0).getNumPoints());

    var shapesByTrip = dataImport.getShapesByTripByTripPattern().get(PATTERN.getId());
    assertThat(shapesByTrip.keySet()).containsExactly(id("detour"));
    assertEquals(DETOUR, shapesByTrip.get(id("detour")).get(0).getNumPoints());
    assertEquals(1, subject.numberOfTripsWithDifferentShape());
    assertEquals(1, subject.numberOfTripPatternsWithDifferentTripShapes());
    // the two straight-line trips share one computed shape
    assertEquals(2, subject.numberOfComputedTripShapes());
  }

  @Test
  void tieIsBrokenByTheShapeOfTheLowestTripId() {
    addTrip(trip("b").build());
    addTrip(trip("a").withShapeId(DETOUR_SHAPE_ID).build());

    subject.addShapesToDataImport(PATTERN, dataImport);

    var tripPatternShape = dataImport.getShapesByTripPattern().get(PATTERN.getId());
    assertEquals(DETOUR, tripPatternShape.get(0).getNumPoints());

    var shapesByTrip = dataImport.getShapesByTripByTripPattern().get(PATTERN.getId());
    assertThat(shapesByTrip.keySet()).containsExactly(id("b"));
  }

  @Test
  void tripsWithTheSameShapeGetNoShapeOfTheirOwn() {
    addTrip(trip("detour1").withShapeId(DETOUR_SHAPE_ID).build());
    addTrip(trip("detour2").withShapeId(DETOUR_SHAPE_ID).build());

    subject.addShapesToDataImport(PATTERN, dataImport);

    var tripPatternShape = dataImport.getShapesByTripPattern().get(PATTERN.getId());
    assertEquals(DETOUR, tripPatternShape.get(0).getNumPoints());

    assertThat(dataImport.getShapesByTripByTripPattern()).isEmpty();
    assertEquals(0, subject.numberOfTripsWithDifferentShape());
    assertEquals(0, subject.numberOfTripPatternsWithDifferentTripShapes());
  }

  private void addTrip(Trip trip) {
    List<StopTime> stopTimes = List.of(
      TEST_MODEL.stopTime(trip, 0, STOP_A),
      TEST_MODEL.stopTime(trip, 1, STOP_B)
    );
    dataImport.getStopTimesSortedByTrip().put(trip, stopTimes);
    subject.addTrip(PATTERN.getId(), trip, STOP_PATTERN, stopTimes);
  }
}
