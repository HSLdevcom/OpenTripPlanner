package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiPlace;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.ApiPlace;
import org.opentripplanner.api.model.ApiVehicleParkingWithEntrance;
import org.opentripplanner.api.model.ApiVehicleParkingWithEntrance.ApiVehicleParkingPlaces;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.VehicleParkingWithEntrance;
import org.opentripplanner.routing.vehicle_parking.VehicleParking.VehiclePlaces;

public class PlaceMapper {

    public static List<ApiPlace> mapStopArrivals(Collection<StopArrival> domain) {
        if(domain == null) { return null; }

        return domain.stream().map(PlaceMapper::mapStopArrival).collect(Collectors.toList());
    }

    public static ApiPlace mapStopArrival(StopArrival domain) {
        return mapPlace(domain.place, domain.arrival, domain.departure);
    }

    public static ApiPlace mapPlace(Place domain, Calendar arrival, Calendar departure) {
        if(domain == null) { return null; }

        ApiPlace api = new ApiPlace();

        api.name = domain.name;

        if (domain.stop != null) {
            api.stopId = FeedScopedIdMapper.mapToApi(domain.stop.getId());
            api.stopCode = domain.stop.getCode();
            api.platformCode = domain.stop instanceof Stop ? ((Stop) domain.stop).getPlatformCode() : null;
            api.zoneId = domain.stop instanceof Stop ? ((Stop) domain.stop).getFirstZoneAsString() : null;
        }

        if(domain.coordinate != null) {
            api.lon = domain.coordinate.longitude();
            api.lat = domain.coordinate.latitude();
        }

        api.vertexType = VertexTypeMapper.mapVertexType(domain.getVertexType());

        api.arrival = arrival;
        api.departure = departure;

        switch (domain.getVertexType()) {
            case NORMAL:
                break;
            case BIKESHARE:
                api.bikeShareId = domain.getBikeRentalStation().id.getId();
                break;
            case VEHICLEPARKING:
                api.vehicleParking = mapVehicleParking(domain.getVehicleParkingWithEntrance());
                break;
            case TRANSIT:
                api.stopId = FeedScopedIdMapper.mapToApi(domain.getStop().getId());
                api.stopCode = domain.getStop().getCode();
                api.platformCode = domain.getStop().getPlatformCode();
                api.zoneId = domain.getStop().getFirstZoneAsString();
                api.stopIndex = domain.getStopIndex();
                api.stopSequence = domain.getStopSequence();
                break;
        }

        return api;
    }

    private static ApiVehicleParkingWithEntrance mapVehicleParking(VehicleParkingWithEntrance vehicleParkingWithEntrance) {
        var vp = vehicleParkingWithEntrance.getVehicleParking();
        var e = vehicleParkingWithEntrance.getEntrance();

        return ApiVehicleParkingWithEntrance.builder()
                .id(FeedScopedIdMapper.mapToApi(vp.getId()))
                .name(vp.getName().toString())
                .entranceId(FeedScopedIdMapper.mapToApi(e.getEntranceId()))
                .entranceName(vp.getName().toString())
                .detailsUrl(vp.getDetailsUrl())
                .imageUrl(vp.getImageUrl())
                .note(vp.getNote() != null ? vp.getNote().toString() : null)
                .tags(new ArrayList<>(vp.getTags()))
                .hasBicyclePlaces(vp.hasBicyclePlaces())
                .hasAnyCarPlaces(vp.hasAnyCarPlaces())
                .hasCarPlaces(vp.hasCarPlaces())
                .hasWheelchairAccessibleCarPlaces(vp.hasWheelchairAccessibleCarPlaces())
                .realtime(vehicleParkingWithEntrance.isRealtime())
                .availability(mapVehicleParkingPlaces(vp.getAvailability()))
                .capacity(mapVehicleParkingPlaces(vp.getCapacity()))
                .closesSoon(vehicleParkingWithEntrance.isClosesSoon())
                .build();
    }

    private static ApiVehicleParkingPlaces mapVehicleParkingPlaces(VehiclePlaces availability) {
        if (availability == null) {
            return null;
        }

        return ApiVehicleParkingPlaces.builder()
                .bicycleSpaces(availability.getBicycleSpaces())
                .carSpaces(availability.getCarSpaces())
                .wheelchairAccessibleCarSpaces(availability.getWheelchairAccessibleCarSpaces())
                .build();
    }
}
