package org.opentripplanner.updater.bike_park;

import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.VehicleParkingEdge;
import org.opentripplanner.routing.edgetype.StreetVehicleParkingLink;
import org.opentripplanner.graph_builder.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vertextype.VehicleParkingVertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Graph updater that dynamically sets availability information on bike parking lots.
 * This updater fetches data from a single BikeParkDataSource.
 *
 * Bike park-and-ride and "OV-fiets mode" development has been funded by GoAbout
 * (https://goabout.com/).
 * 
 * @author laurent
 * @author GoAbout
 */
public class BikeParkUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(BikeParkUpdater.class);

    private GraphUpdaterManager updaterManager;

    private final Map<VehicleParking, VehicleParkingVertex> verticesByPark = new HashMap<>();

    private final Map<VehicleParking, DisposableEdgeCollection> tempEdgesByPark = new HashMap<>();

    private final BikeParkDataSource source;

    private VertexLinker linker;

    private VehicleParkingService vehicleParkingService;

    public BikeParkUpdater(BikeParkUpdaterParameters parameters) {
        super(parameters);
        // Set source from preferences
        source = new KmlBikeParkDataSource(parameters.sourceParameters());

        LOG.info("Creating bike-park updater running every {} seconds : {}", pollingPeriodSeconds, source);
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) {
        // Creation of network linker library will not modify the graph
        linker = graph.getLinker();
        // Adding a bike park station service needs a graph writer runnable
        vehicleParkingService = graph.getService(VehicleParkingService.class, true);
    }

    @Override
    protected void runPolling() throws Exception {
        LOG.debug("Updating bike parks from " + source);
        if (!source.update()) {
            LOG.debug("No updates");
            return;
        }
        List<VehicleParking> bikeParks = source.getBikeParks();

        // Create graph writer runnable to apply these stations to the graph
        BikeParkGraphWriterRunnable graphWriterRunnable = new BikeParkGraphWriterRunnable(bikeParks);
        updaterManager.execute(graphWriterRunnable);
    }

    @Override
    public void teardown() {
    }

    private class BikeParkGraphWriterRunnable implements GraphWriterRunnable {

        private final List<VehicleParking> bikeParks;

        private BikeParkGraphWriterRunnable(List<VehicleParking> bikeParks) {
            this.bikeParks = bikeParks;
        }

        @Override
        public void run(Graph graph) {
            // Apply stations to graph
            Set<VehicleParking> bikeParkSet = new HashSet<>();
            /* Add any new park and update space available for existing parks */
            for (VehicleParking bikePark : bikeParks) {
                vehicleParkingService.addVehicleParking(bikePark);
                bikeParkSet.add(bikePark);
                VehicleParkingVertex bikeParkVertex = verticesByPark.get(bikePark);
                if (bikeParkVertex == null) {
                    bikeParkVertex = new VehicleParkingVertex(graph, bikePark);
                    DisposableEdgeCollection tempEdges = linker.linkVertexForRealTime(
                        bikeParkVertex,
                        new TraverseModeSet(TraverseMode.WALK),
                        LinkingDirection.BOTH_WAYS,
                        (vertex, streetVertex) -> List.of(
                            new StreetVehicleParkingLink((VehicleParkingVertex) vertex, streetVertex),
                            new StreetVehicleParkingLink(streetVertex, (VehicleParkingVertex) vertex)
                        )
                    );

                    if (bikeParkVertex.getOutgoing().isEmpty()) {
                        // the toString includes the text "Bike park"
                        LOG.info("Bike park {} unlinked", bikeParkVertex);
                    }

                    new VehicleParkingEdge(bikeParkVertex);
                    verticesByPark.put(bikePark, bikeParkVertex);
                    tempEdgesByPark.put(bikePark, tempEdges);
                } else {
                    bikeParkVertex.getVehicleParking().updateVehiclePlaces(bikePark.getAvailability());
                }
            }
            /* Remove existing parks that were not present in the update */
            List<VehicleParking> toRemove = new ArrayList<>();
            for (Entry<VehicleParking, VehicleParkingVertex> entry : verticesByPark.entrySet()) {
                VehicleParking bikePark = entry.getKey();
                if (bikeParkSet.contains(bikePark))
                    continue;
                toRemove.add(bikePark);
                vehicleParkingService.removeVehicleParking(bikePark);
            }
            for (VehicleParking bikePark : toRemove) {
                // post-iteration removal to avoid concurrent modification
                verticesByPark.remove(bikePark);
                tempEdgesByPark.get(bikePark).disposeEdges();
                tempEdgesByPark.remove(bikePark);
            }
        }
    }
}
