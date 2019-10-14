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

package org.opentripplanner.routing.impl;

import org.opentripplanner.annotation.Component;
import org.opentripplanner.annotation.ServiceType;
import org.opentripplanner.routing.core.StandardFareType;
import org.opentripplanner.routing.services.FareService;

@Component(key = "HSL",type = ServiceType.ServiceFactory)
public class HSLFareServiceFactory extends DefaultFareServiceFactory {

    public FareService makeFareService() {
        HSLFareServiceImpl fareService = new HSLFareServiceImpl();
        fareService.addFareRules(StandardFareType.regular, regularFareRules.values());
        return fareService;
    }
}
