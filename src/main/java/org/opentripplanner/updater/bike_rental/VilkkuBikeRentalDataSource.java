package org.opentripplanner.updater.bike_rental;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import com.google.common.base.Strings;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;

/**
 * Vilkku (Kuopio, Finland) bike rental data source.
 * url: https://kaupunkipyorat.kuopio.fi/tkhs-export-map.html?format=xml
 */
public class VilkkuBikeRentalDataSource extends GenericXmlBikeRentalDataSource {
    
    private String networkName;
    
    public VilkkuBikeRentalDataSource(String networkName) {
        super("//station");
        this.networkName = Strings.isNullOrEmpty(networkName) ? "vilkku" : networkName;
    }
    
    public BikeRentalStation makeStation(Map<String, String> attributes) {
        BikeRentalStation station = new BikeRentalStation();
        station.networks = new HashSet<>(Collections.singleton(this.networkName));
        station.id = attributes.get("name");
        station.x = getCoordinate(attributes.get("longitude"));
        station.y = getCoordinate(attributes.get("latitude"));
        station.name = new NonLocalizedString(attributes.get("name"));
        station.bikesAvailable  = Integer.parseInt(attributes.get("externallyLockedBikes"));
        station.state = "Station on";
        return station;
    }

    private double getCoordinate(String coordinate) {
        // for some reason the API returns coordinates with ',' as decimal separator
        if (coordinate.contains(",")) {
            return Double.parseDouble(coordinate.replace(",", "."));
        }
        return Double.parseDouble(coordinate);
    }
}
