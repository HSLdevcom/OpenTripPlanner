package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitService;

class TransfersMapper {

  /**
   * Copy pre-calculated transfers from the original graph
   * @return a list where each element is a list of transfers for the corresponding stop index
   */
  static List<Map<StreetMode, List<Transfer>>> mapTransfers(
    SiteRepository siteRepository,
    TransitService transitService
  ) {
    List<Map<StreetMode, List<Transfer>>> transfersForModeByStopIndex = new ArrayList<>();

    for (int i = 0; i < siteRepository.stopIndexSize(); ++i) {
      var stop = siteRepository.stopByIndex(i);

      if (stop == null) {
        continue;
      }

      // This contains transfers for a certain stop mapped to the relevant mode.
      Map<StreetMode, List<Transfer>> stopTransfersForMode = new HashMap<>();

      for (Map.Entry<StreetMode, Multimap<StopLocation, PathTransfer>> entry : transitService
        .getTransfersByStopForMode()
        .entrySet()) {
        ArrayList<Transfer> list = new ArrayList<>();
        StreetMode mode = entry.getKey();
        Collection<PathTransfer> transfersByStop = entry.getValue().get(stop);
        for (PathTransfer pathTransfer : transfersByStop) {
          if (pathTransfer.to instanceof RegularStop) {
            int toStopIndex = pathTransfer.to.getIndex();
            Transfer newTransfer;
            if (pathTransfer.getEdges() != null) {
              newTransfer = new Transfer(toStopIndex, pathTransfer.getEdges());
            } else {
              newTransfer =
                new Transfer(toStopIndex, (int) Math.ceil(pathTransfer.getDistanceMeters()));
            }

            list.add(newTransfer);
          }
        }
        // Create a copy to compact and make the inner lists immutable
        stopTransfersForMode.put(mode, List.copyOf(list));
      }
      // Make map immutable
      transfersForModeByStopIndex.add(Map.copyOf(stopTransfersForMode));
    }

    // Return an immutable copy
    return List.copyOf(transfersForModeByStopIndex);
  }
}
