package org.opentripplanner.updater.vehicle_parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ParkApiUpdaterTest {

    @Test
    void parseCars() {

        var cwd = System.getProperty("user.dir");
        var url = "file://" + cwd + "/src/test/resources/vehicle_parking/parkapi-reutlingen.json";

        var updater = new CarParkAPIUpdater(url, "park-api", List.of());

        assertTrue(updater.update());
        var parkingLots = updater.getUpdates();

        assertEquals(30, parkingLots.size());

        var first = parkingLots.get(0);
        assertEquals("Parkplatz Alenberghalle", first.getName().toString());
        assertTrue(first.hasAnyCarPlaces());
        assertNull(first.getCapacity());


        var last = parkingLots.get(29);
        assertEquals("Zehntscheuer Kegelgraben", last.getName().toString());
        assertTrue(last.hasAnyCarPlaces());
        assertTrue(last.hasWheelchairAccessibleCarPlaces());
        assertEquals(1, last.getCapacity().getWheelchairAccessibleCarSpaces());
    }

}
