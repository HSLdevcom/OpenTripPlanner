package org.opentripplanner.updater.trip.gtfs.moduletests.shape;

import static com.google.common.truth.Truth.assertThat;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.DUPLICATED;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.NEW;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.REPLACEMENT;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.gtfs.GtfsRtTestHelper;

/**
 * Tests resolution of GTFS-RT {@code TripProperties.shape_id} against a standalone {@code Shape}
 * FeedEntity carried in the same message, per google/transit#653: the referenced shape may
 * describe a new vehicle path even when the trip's stop sequence differs from static GTFS (e.g.
 * for {@code REPLACEMENT}/{@code NEW} trips).
 */
class ShapeResolutionTest implements RealtimeTestConstants {

  private static final String SHAPE_ID = "detour-shape";

  private static LineString shapeGeometry(RegularStop... stops) {
    var coordinates = new Coordinate[stops.length];
    for (int i = 0; i < stops.length; i++) {
      coordinates[i] = stops[i].getCoordinate().asJtsCoordinate();
    }
    return GeometryUtils.makeLineString(coordinates);
  }

  @Test
  void replacementTripUsesResolvedShape() {
    var builder = TransitTestEnvironment.of();
    var stopA = builder.stop(STOP_A_ID);
    var stopB = builder.stop(STOP_B_ID);
    var stopC = builder.stop(STOP_C_ID);
    var env = builder
      .addTrip(
        TripInput.of(TRIP_1_ID)
          .addStop(stopA, "8:30:00", "8:30:00")
          .addStop(stopB, "8:40:00", "8:40:00")
      )
      .build();
    var rt = GtfsRtTestHelper.of(env);

    var geometry = shapeGeometry(stopA, stopB, stopC);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, REPLACEMENT)
      .withShapeId(SHAPE_ID)
      .addStopTime(STOP_A_ID, "00:30")
      .addStopTime(STOP_B_ID, "00:45")
      .addStopTime(STOP_C_ID, "01:00")
      .build();

    var result = rt.applyTripUpdates(
      List.of(tripUpdate),
      UpdateIncrementality.FULL_DATASET,
      Map.of(SHAPE_ID, geometry)
    );
    assertSuccess(result);

    var tripPattern = env.tripData(TRIP_1_ID).tripPattern();
    assertThat(tripPattern.getGeometry()).isNotNull();
    assertThat(tripPattern.getHopGeometry(0).getNumPoints()).isAtLeast(2);
  }

  @Test
  void newTripUsesResolvedShape() {
    var builder = TransitTestEnvironment.of();
    var stopA = builder.stop(STOP_A_ID);
    var stopB = builder.stop(STOP_B_ID);
    var env = builder
      .addTrip(
        TripInput.of(TRIP_1_ID)
          .withServiceDates(
            builder.defaultServiceDate().minusDays(1),
            builder.defaultServiceDate().plusDays(1)
          )
          .addStop(stopA, "12:00", "12:00")
          .addStop(stopB, "12:10", "12:10")
      )
      .build();
    var rt = GtfsRtTestHelper.of(env);

    var geometry = shapeGeometry(stopA, stopB);

    var tripUpdate = rt
      .tripUpdate(ADDED_TRIP_ID, NEW)
      .withShapeId(SHAPE_ID)
      .addStopTime(STOP_A_ID, "00:30")
      .addStopTime(STOP_B_ID, "00:45")
      .build();

    var result = rt.applyTripUpdates(
      List.of(tripUpdate),
      UpdateIncrementality.FULL_DATASET,
      Map.of(SHAPE_ID, geometry)
    );
    assertSuccess(result);

    var tripPattern = env.tripData(ADDED_TRIP_ID).tripPattern();
    assertThat(tripPattern.getGeometry()).isNotNull();
  }

  @Test
  void unresolvedShapeIdFallsBackToDefaultGeometry() {
    var builder = TransitTestEnvironment.of();
    var stopA = builder.stop(STOP_A_ID);
    var stopB = builder.stop(STOP_B_ID);
    var env = builder
      .addTrip(
        TripInput.of(TRIP_1_ID)
          .withServiceDates(
            builder.defaultServiceDate().minusDays(1),
            builder.defaultServiceDate().plusDays(1)
          )
          .addStop(stopA, "12:00", "12:00")
          .addStop(stopB, "12:10", "12:10")
      )
      .build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdate(ADDED_TRIP_ID, NEW)
      .withShapeId("unknown-shape-id")
      .addStopTime(STOP_A_ID, "00:30")
      .addStopTime(STOP_B_ID, "00:45")
      .build();

    // no shapes provided -- shape_id does not resolve, should not fail, falls back
    var result = rt.applyTripUpdate(tripUpdate);
    assertSuccess(result);

    var tripPattern = env.tripData(ADDED_TRIP_ID).tripPattern();
    assertThat(tripPattern.getGeometry()).isNotNull();
  }

  @Test
  void duplicatedTripUsesResolvedShape() {
    var envBuilder = TransitTestEnvironment.of();
    var stopA = envBuilder.stop(STOP_A_ID);
    var stopB = envBuilder.stop(STOP_B_ID);
    var stopC = envBuilder.stop(STOP_C_ID);
    var serviceDate = LocalDate.of(2026, 6, 22);
    var env = envBuilder
      .addTrip(
        TripInput.of(TRIP_1_ID)
          .withServiceDates(serviceDate, serviceDate)
          .addStop(stopA, "12:00")
          .addStop(stopB, "12:10")
          .addStop(stopC, "12:20")
      )
      .build();
    var rt = GtfsRtTestHelper.of(env);

    var geometry = shapeGeometry(stopA, stopB, stopC);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, DUPLICATED)
      .withStartDate(serviceDate)
      .withStartTime(LocalTime.of(13, 30))
      .withShapeId(SHAPE_ID)
      .build();

    var result = rt.applyTripUpdates(
      List.of(tripUpdate),
      UpdateIncrementality.FULL_DATASET,
      Map.of(SHAPE_ID, geometry)
    );
    assertSuccess(result);

    var duplicatedId = TRIP_1_ID + ":duplicated:" + serviceDate + "T13:30";
    var tripPattern = env.tripData(duplicatedId, serviceDate).tripPattern();
    assertThat(tripPattern.getGeometry()).isNotNull();

    var originalPattern = env
      .transitService()
      .findPattern(env.transitService().getTrip(FeedScopedIdForTestFactory.id(TRIP_1_ID)));
    assertThat(originalPattern.getStopPattern()).isEqualTo(tripPattern.getStopPattern());
  }
}
