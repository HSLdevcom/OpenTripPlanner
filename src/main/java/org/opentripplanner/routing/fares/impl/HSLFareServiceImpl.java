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

package org.opentripplanner.routing.fares.impl;

import java.io.Serial;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opentripplanner.model.FareAttribute;

import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareRuleSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This fare service module handles single feed HSL ticket pricing logic.
 */

public class HSLFareServiceImpl extends DefaultFareServiceImpl {
  @Serial
  private static final long serialVersionUID = 20131259L;
  private static final Logger LOG = LoggerFactory.getLogger(HSLFareServiceImpl.class);

  @Override
  protected FareAndId getBestFareAndId(FareType fareType, List<Ride> rides, Collection<FareRuleSet> fareRules) {
    Set<String> zones = new HashSet<String>();
    ZonedDateTime startTime = rides.get(0).startTime;
    ZonedDateTime lastRideStartTime = startTime;

    float specialRouteFare = Float.POSITIVE_INFINITY;
    FareAttribute specialFareAttribute = null;

    for (Ride ride : rides) {
      lastRideStartTime = ride.startTime;

            /* HSL specific logic: all exception routes start and end from the defined zone set,
               but visit temporarily (maybe 1 stop only) an 'external' zone */
      float bestSpecialFare = Float.POSITIVE_INFINITY;
      Set<String> ruleZones = null;
      for (FareRuleSet ruleSet : fareRules) {
        String routeOriginDestination = ride.route + ", " + ride.startZone + ", " + ride.endZone;
        boolean isSpecialRoute = false;
        if(!ruleSet.getRouteOriginDestinations().isEmpty() && ruleSet.getRouteOriginDestinations().toString().indexOf(routeOriginDestination) != -1) {
          isSpecialRoute = true;
        }
        if(isSpecialRoute || (ruleSet.getRoutes().contains(ride.route) &&
          ruleSet.getContains().contains(ride.startZone) &&
          ruleSet.getContains().contains(ride.endZone))) {
          // check validity of this special rule and that it is the cheapest applicable one
          FareAttribute attribute = ruleSet.getFareAttribute();
          if (!attribute.isTransferDurationSet() ||
            Duration.between(lastRideStartTime, startTime).getSeconds() < attribute.getTransferDuration()) {
            float newFare = getFarePrice(attribute, fareType);
            if (newFare < bestSpecialFare) {
              bestSpecialFare = newFare;
              ruleZones = ruleSet.getContains();
              if(isSpecialRoute) {
                specialRouteFare = bestSpecialFare;
                specialFareAttribute = attribute;
              }
            }
          }
        }
      }
      if (ruleZones != null) { // the special case
        // evaluate boolean ride.zones AND rule.zones
        Set<String> zoneIntersection = new HashSet<String>(ride.zones);
        zoneIntersection.retainAll(ruleZones); // don't add temporarily visited zones
        zones.addAll(zoneIntersection);
      } else {
        zones.addAll(ride.zones);
      }
    }

    FareAttribute bestAttribute = null;
    float bestFare = Float.POSITIVE_INFINITY;
    long tripTime = Duration.between(lastRideStartTime, startTime).getSeconds();

    if(zones.size() > 0) {
      // find the best fare that matches this set of rides
      for (FareRuleSet ruleSet : fareRules) {
                /* another HSL specific change: We do not set rules for every possible zone combination,
                but for the largest zone set allowed for a certain ticket type.
                This way we need only a few rules instead of hundreds of rules. Good for speed!
                */
        if (ruleSet.getContains().containsAll(zones)) { // contains, not equals !!
          FareAttribute attribute = ruleSet.getFareAttribute();
          // transfers are evaluated at boarding time
          if (attribute.isTransferDurationSet()) {
            if(tripTime > attribute.getTransferDuration()) {
              LOG.debug("transfer time exceeded; {} > {} in fare {}", tripTime, attribute.getTransferDuration(), attribute.getId());
              continue;
            } else {
              LOG.debug("transfer time OK; {} < {} in fare {}", tripTime, attribute.getTransferDuration(), attribute.getId());
            }
          }
          float newFare = getFarePrice(attribute, fareType);
          if (newFare < bestFare) {
            bestAttribute = attribute;
            bestFare = newFare;
          }
        }
      }
    } else if (specialRouteFare != Float.POSITIVE_INFINITY && specialFareAttribute != null) {
      bestFare = specialRouteFare;
      bestAttribute = specialFareAttribute;
    }
    LOG.debug("HSL {} best for {}", bestAttribute, rides);
    return new FareAndId(bestFare, bestAttribute == null ? null : bestAttribute.getId());
  }


}

