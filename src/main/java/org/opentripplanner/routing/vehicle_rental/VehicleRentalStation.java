package org.opentripplanner.routing.vehicle_rental;

import static java.util.Locale.ROOT;

import java.io.Serializable;
import java.util.Set;
import org.opentripplanner.util.I18NString;

public class VehicleRentalStation implements Serializable {
    private static final long serialVersionUID = 8311460609708089384L;

    public String id;
    public I18NString name;
    public double longitude;
    public double latitude;
    public int vehiclesAvailable = Integer.MAX_VALUE;
    public int spacesAvailable = Integer.MAX_VALUE;
    public boolean allowDropoff = true;
    public boolean isFloatingBike = false;
    public boolean isCarStation = false;
    public boolean isKeepingVehicleRentalAtDestinationAllowed = false;

    /**
     * List of compatible network names. Null (default) to be compatible with all.
     */
    public Set<String> networks = null;
    
    /**
     * Whether this station is static (usually coming from OSM data) or a real-time source. If no real-time data, users should take
     * bikesAvailable/spacesAvailable with a pinch of salt, as they are always the total capacity divided by two. Only the total is meaningful.
     */
    public boolean realTimeData = true;

    public VehicleRentalStationUris rentalUris;
    
    public String toString () {
        return String.format(ROOT, "Vehicle rental station %s at %.6f, %.6f", name, latitude, longitude);
    }
}
