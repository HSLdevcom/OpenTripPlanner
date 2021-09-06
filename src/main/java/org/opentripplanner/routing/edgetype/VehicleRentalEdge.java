package org.opentripplanner.routing.edgetype;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;

/**
 * Renting or dropping off a rented vehicle edge.
 * 
 * @author laurent
 * 
 */
public class VehicleRentalEdge extends Edge {

    private static final long serialVersionUID = 1L;

    public VehicleRentalEdge(VehicleRentalStationVertex vertex) {
        super(vertex, vertex);
    }

    public State traverse(State s0) {
        if (!s0.getOptions().bikeRental) { return null; }

        StateEditor s1 = s0.edit(this);
        RoutingRequest options = s0.getOptions();

        VehicleRentalStationVertex stationVertex = (VehicleRentalStationVertex) tov;
        var networks = allowedNetworkBasedOnOptions(options, stationVertex.getStation().networks);
        if (networks.isEmpty()) {
            return null;
        }

        boolean pickedUp;
        if (options.arriveBy) {
            switch (s0.getVehicleRentalState()) {
                case BEFORE_RENTING:
                    return null;
                case HAVE_RENTED:
                    if (options.useVehicleRentalAvailabilityInformation && stationVertex.getSpacesAvailable() == 0) {
                        return null;
                    }
                    s1.dropOffRentedVehicleAtStation(stationVertex.getVehicleMode(), networks, true);
                    pickedUp = false;
                    break;
                case RENTING_FLOATING:
                    if (stationVertex.getStation().isFloatingBike) {
                        s1.beginFloatingVehicleRenting(stationVertex.getVehicleMode(), networks, true);
                        pickedUp = true;
                    } else {
                        return null;
                    }
                    break;
                case RENTING_FROM_STATION:
                    if (options.useVehicleRentalAvailabilityInformation && stationVertex.getVehiclesAvailable() == 0) {
                        return null;
                    }
                    // For arriveBy searches mayKeepRentedVehicleAtDestination is only set in State#getInitialStates(),
                    // and so here it is checked if this bicycle could have been kept at the destination
                    if (s0.mayKeepRentedVehicleAtDestination() && !stationVertex.getStation().isKeepingVehicleRentalAtDestinationAllowed) {
                        return null;
                    }
                    if (!hasCompatibleNetworks(networks, s0.getVehicleRentalNetworks())) { return null; }
                    s1.beginVehicleRentingAtStation(stationVertex.getVehicleMode(), networks, false, true);
                    pickedUp = true;
                    break;
                default:
                    throw new IllegalStateException();
            }
        } else {
            switch (s0.getVehicleRentalState()) {
                case BEFORE_RENTING:
                    if (options.useVehicleRentalAvailabilityInformation && stationVertex.getVehiclesAvailable() == 0) {
                        return null;
                    }
                    if (stationVertex.getStation().isFloatingBike) {
                        s1.beginFloatingVehicleRenting(stationVertex.getVehicleMode(), networks,
                                false);
                    } else {
                        var mayKeep =
                                stationVertex.getStation().isKeepingVehicleRentalAtDestinationAllowed
                                        && options.allowKeepingRentedVehicleAtDestination;
                        s1.beginVehicleRentingAtStation(stationVertex.getVehicleMode(), networks,
                                mayKeep, false);
                    }
                    pickedUp = true;
                    break;
                case HAVE_RENTED:
                    return null;
                case RENTING_FLOATING:
                case RENTING_FROM_STATION:
                    if (!hasCompatibleNetworks(networks, s0.getVehicleRentalNetworks())) { return null; }
                    if (options.useVehicleRentalAvailabilityInformation && stationVertex.getSpacesAvailable() == 0) {
                        return null;
                    }
                    s1.dropOffRentedVehicleAtStation(stationVertex.getVehicleMode(), networks, false);
                    pickedUp = false;
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        s1.incrementWeight(pickedUp ? options.bikeRentalPickupCost : options.bikeRentalDropoffCost);
        s1.incrementTimeInSeconds(pickedUp ? options.bikeRentalPickupTime : options.bikeRentalDropoffTime);
        s1.setBackMode(null);
        return s1.makeState();
    }

    @Override
    public double getDistanceMeters() {
        return 0;
    }

    @Override
    public LineString getGeometry() {
        return null;
    }

    @Override
    public String getName() {
        return getToVertex().getName();
    }

    @Override
    public String getName(Locale locale) {
        return getToVertex().getName(locale);
    }

    @Override
    public boolean hasBogusName() {
        return false;
    }

    private Set<String> allowedNetworkBasedOnOptions(RoutingRequest options, Set<String> networks) {
        if (options.allowedBikeRentalNetworks.isEmpty()
                && options.bannedBikeRentalNetworks.isEmpty()
        ) {
            return networks;
        }

        var result = new HashSet<>(networks);
        result.removeAll(options.bannedBikeRentalNetworks);
        if (!options.allowedBikeRentalNetworks.isEmpty()) {
            result.retainAll(options.allowedBikeRentalNetworks);
        }
        return result;
    }

    /**
     * @param stationNetworks The station where we want to drop the bike off.
     * @param rentedNetworks The set of networks of the station we rented the bike from.
     * @return true if the bike can be dropped off here, false if not.
     */
    private boolean hasCompatibleNetworks(Set<String> stationNetworks, Set<String> rentedNetworks) {
        /*
         * Two stations are compatible if they share at least one network. Special case for "null"
         * networks ("catch-all" network defined).
         */
        if (stationNetworks == null || rentedNetworks == null)
            return true; // Always a match
        return !Sets.intersection(stationNetworks, rentedNetworks).isEmpty();
    }
}
