# Digitransit OTP Release Summary

## Changelog _20260922_ vs _upstream/dev-2.x_

### Added PRs
- Allow very large NeTEx versions [#7955](https://github.com/opentripplanner/OpenTripPlanner/pull/7955)
- Expose vehicle id through GTFS API [#7923](https://github.com/opentripplanner/OpenTripPlanner/pull/7923)
- Add vehicleRentalGeofencing sandbox loading GBFS geofencing zones during the graph build [#7887](https://github.com/opentripplanner/OpenTripPlanner/pull/7887)
- Add snowAndIce to the Transmodel TransportMode enum [#7980](https://github.com/opentripplanner/OpenTripPlanner/pull/7980)
- Limit growth of NeTEx stop time index [#7963](https://github.com/opentripplanner/OpenTripPlanner/pull/7963)
- Remove .idea files from version control [#7992](https://github.com/opentripplanner/OpenTripPlanner/pull/7992)
- Deprecate serviceId in GTFS API and stop populating it [#7991](https://github.com/opentripplanner/OpenTripPlanner/pull/7991)
- Decode OSM PBF data in parallel [#8008](https://github.com/opentripplanner/OpenTripPlanner/pull/8008)
- Verify who labeled the PRs merged by the custom release script [#8018](https://github.com/opentripplanner/OpenTripPlanner/pull/8018)
- Use custom collections to speed up island pruning [#7990](https://github.com/opentripplanner/OpenTripPlanner/pull/7990)
- Treat negative `stair_count` as stairs in `PathwayEdge` [#8027](https://github.com/opentripplanner/OpenTripPlanner/pull/8027)
- Improvements in routing and graph build performance [#7440](https://github.com/opentripplanner/OpenTripPlanner/pull/7440)
- Implement on-board access in Transmodel trip API [#7429](https://github.com/opentripplanner/OpenTripPlanner/pull/7429)
- Make TransitGroupPriority an official feature [#7738](https://github.com/opentripplanner/OpenTripPlanner/pull/7738)
- Cache TransferIndexGenerator results to skip redundant regeneration [#7355](https://github.com/opentripplanner/OpenTripPlanner/pull/7355)
- De-duplicate boarding locations on areas [#7508](https://github.com/opentripplanner/OpenTripPlanner/pull/7508)
- Add application warmup feature to run routing queries during startup [#7509](https://github.com/opentripplanner/OpenTripPlanner/pull/7509) [#7576](https://github.com/opentripplanner/OpenTripPlanner/pull/7576)
- Set default fares to `gtfs` [#7545](https://github.com/opentripplanner/OpenTripPlanner/pull/7545)
- Improve secret redaction for config file logging [#7560](https://github.com/opentripplanner/OpenTripPlanner/pull/7560)
- Add transactional repository and snapshot framework [#7745](https://github.com/opentripplanner/OpenTripPlanner/pull/7745) [#7689](https://github.com/opentripplanner/OpenTripPlanner/pull/7689) [#7894](https://github.com/opentripplanner/OpenTripPlanner/pull/7894)
- Move per-network GBFS configuration into a shared GBFS section of otp-config.json [#7886](https://github.com/opentripplanner/OpenTripPlanner/pull/7886)
- Allow nanosecond resolution in NeTEx date times [#7983](https://github.com/opentripplanner/OpenTripPlanner/pull/7983)
- Support GML DirectPosition in NeTEx  [#7954](https://github.com/opentripplanner/OpenTripPlanner/pull/7954)
