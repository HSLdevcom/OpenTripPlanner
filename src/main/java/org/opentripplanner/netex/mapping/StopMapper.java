package org.opentripplanner.netex.mapping;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.FareZone;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalVersionMapById;
import org.opentripplanner.netex.issues.QuayWithoutCoordinates;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;

class StopMapper {

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  private final ReadOnlyHierarchicalVersionMapById<StopPlace> stopPlaceIndex;

  StopMapper(
          FeedScopedIdFactory idFactory,
          DataImportIssueStore issueStore,
          ReadOnlyHierarchicalVersionMapById<StopPlace> stopPlaceIndex
  ) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
    this.stopPlaceIndex = stopPlaceIndex;
  }

  /**
   * Map Netex Quay to OTP Stop
   */
  @Nullable
  Stop mapQuayToStop(
          Quay quay,
          Station parentStation,
          Collection<FareZone> fareZones,
          T2<TransitMode, String> transitMode
  ) {
    WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(quay.getCentroid());

    if (coordinate == null) {
      issueStore.add(new QuayWithoutCoordinates(quay.getId()));
      return null;
    }

    var wheelchairBoarding = wheelChairBoardingFromQuay(quay, parentStation);

    Stop stop = new Stop(
            idFactory.createId(quay.getId()),
            parentStation.getName(),
            quay.getPublicCode(),
            quay.getDescription() != null ? quay.getDescription().getValue() : null,
            WgsCoordinateMapper.mapToDomain(quay.getCentroid()),
            wheelchairBoarding,
            null,
            null,
            fareZones,
            null,
            null,
            transitMode.first,
            transitMode.second
    );

    stop.setParentStation(parentStation);

    return stop;
  }

  /**
   * Get WheelChairBoarding from Quay and parent Station.
   *
   * @param quay          NeTEx quay could contain information about accessability
   * @param parentStation Used to get a default WheelChairBoarding if not found from Quay.
   * @return not null value with default NO_INFORMATION if nothing defined in quay or
   * parentStation.
   */
  private WheelChairBoarding wheelChairBoardingFromQuay(Quay quay, Station parentStation) {

    var defaultWheelChairBoarding = WheelChairBoarding.NO_INFORMATION;
    var stopPlaceId = parentStation.getId();
    var stopPlace = stopPlaceIndex.lookupLastVersionById(stopPlaceId.getId());

    if (stopPlace != null) {
      defaultWheelChairBoarding = WheelChairMapper.wheelChairBoarding(
              stopPlace.getAccessibilityAssessment(),
              WheelChairBoarding.NO_INFORMATION
      );
    }

    return WheelChairMapper.wheelChairBoarding(
            quay.getAccessibilityAssessment(), defaultWheelChairBoarding);
  }

}
