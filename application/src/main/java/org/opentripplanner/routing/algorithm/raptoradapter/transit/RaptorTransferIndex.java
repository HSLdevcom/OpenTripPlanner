package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

public class RaptorTransferIndex {

  private final List<RaptorTransfer>[] forwardTransfers;

  private final List<RaptorTransfer>[] reversedTransfers;

  public RaptorTransferIndex(
    List<List<RaptorTransfer>> forwardTransfers,
    List<List<RaptorTransfer>> reversedTransfers
  ) {
    // Create immutable copies of the lists for each stop to make them immutable and faster to iterate
    this.forwardTransfers = forwardTransfers.stream().map(List::copyOf).toArray(List[]::new);
    this.reversedTransfers = reversedTransfers.stream().map(List::copyOf).toArray(List[]::new);
  }

  public static RaptorTransferIndex create(
    List<Map<StreetMode, List<Transfer>>> transfersForModeByStopIndex,
    StreetSearchRequest request
  ) {
    var forwardTransfers = new ArrayList<List<RaptorTransfer>>(transfersForModeByStopIndex.size());
    var reversedTransfers = new ArrayList<List<RaptorTransfer>>(transfersForModeByStopIndex.size());

    StreetMode mode = request.mode();

    for (int i = 0; i < transfersForModeByStopIndex.size(); i++) {
      forwardTransfers.add(new ArrayList<>());
      reversedTransfers.add(new ArrayList<>());
    }

    for (int fromStop = 0; fromStop < transfersForModeByStopIndex.size(); fromStop++) {
      // The transfers are filtered so that there is only one possible directional transfer
      // for a stop pair.
      var transfers = transfersForModeByStopIndex
        .get(fromStop)
        // this is getOrDefault because of tests failing, should the tests instead be changed to allow for get?
        .getOrDefault(mode, List.of())
        .stream()
        .flatMap(s -> s.asRaptorTransfer(request).stream())
        .collect(
          toMap(RaptorTransfer::stop, Function.identity(), (a, b) -> a.c1() < b.c1() ? a : b)
        )
        .values();

      forwardTransfers.get(fromStop).addAll(transfers);

      for (RaptorTransfer forwardTransfer : transfers) {
        reversedTransfers
          .get(forwardTransfer.stop())
          .add(DefaultRaptorTransfer.reverseOf(fromStop, forwardTransfer));
      }
    }

    return new RaptorTransferIndex(forwardTransfers, reversedTransfers);
  }

  public List<RaptorTransfer> getForwardTransfers(int stopIndex) {
    return forwardTransfers[stopIndex];
  }

  public List<RaptorTransfer> getReversedTransfers(int stopIndex) {
    return reversedTransfers[stopIndex];
  }
}
