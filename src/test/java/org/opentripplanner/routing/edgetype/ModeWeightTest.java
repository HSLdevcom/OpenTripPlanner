/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import org.opentripplanner.GtfsTest;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;

public class ModeWeightTest extends GtfsTest {
    private HashMap<TraverseMode, Double> currentWeights = new HashMap<>();

    @Override
    public boolean isLongDistance() {
        return true;
    }

    @Override
    public String getFeedName() {
        return "gtfs/modeweight";
    }

    protected RoutingRequest getRoutingRequest() {
        return new RoutingRequest() {
            @Override
            public double getModeWeight(TraverseMode traverseMode) {
                Double weight = currentWeights.get(traverseMode);
                if (weight == null) {
                    weight = 1d;
                }
                return weight;
            }
        };

    }

    private void assertRoute(final String expectedRoute) {
        Calendar calendar = new GregorianCalendar(2014, Calendar.JANUARY, 01, 00, 05, 00);
        calendar.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        long time = calendar.getTime().getTime() / 1000;
        // We should arrive at the destination using the fastest route
        Leg[] legs = plan(time, "stop0", "stop3", null, false, false, null, null, null, 1);
        assertEquals(1, legs.length);
        assertEquals(expectedRoute, legs[0].routeId.getId());
        assertTrue(itinerary.transfers == 0);
    }
    
    public void testDefaultWeights() {
        currentWeights.put(TraverseMode.BUS, 1d);
        assertRoute("route1");
    }

    public void testModifiedWeights() {
        currentWeights.put(TraverseMode.BUS, 1.05d);
        assertRoute("route0");
    }

}
