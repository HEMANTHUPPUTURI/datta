package com.ibsplc.ops.afkl.day.mapper;

import com.ibsplc.si.event.schema.flightleg.notification.DepartureArrivalType;
import com.ibsplc.si.event.schema.flightleg.notification.FlightLegType;
import com.ibsplc.si.event.schema.flightleg.notification.IATAAIDXFlightLegNotifRQ;
import com.ibsplc.si.event.schema.flightleg.notification.UsageType;
import com.ibsplc.si.event.schema.sofi.Arrival;
import com.ibsplc.si.event.schema.sofi.Boarding;
import com.ibsplc.si.event.schema.sofi.Departure;
import com.ibsplc.si.event.schema.sofi.Disembarking;
import com.ibsplc.si.event.schema.sofi.FlightLeg;
import com.ibsplc.si.event.schema.sofi.Location;
import com.ibsplc.si.event.schema.sofi.ParkingPosition;
import com.ibsplc.si.event.schema.sofi.SendOperationalFlightInternalEvent;
import com.ibsplc.si.event.schema.sofi.SoapHeader;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.AIRCRAFT_PARKING_POSITION;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.AIRCRAFT_SUB_TYPE;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.CODE_CONTEXT;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.COLON;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.DEPARTURE;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.NAMESPACE_URI;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.PUBLIC;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.REGISTRATION;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.REPEAT_INDEX;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.SOFI;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.THREE_DIGIT_FORMAT;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.VERSION;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SOFIByAFMapper {

    SOFIByAFMapper INSTANCE = Mappers.getMapper(SOFIByAFMapper.class);

    /**
     * Maps a SendOperationalFlightInternalEvent to an IATAAIDXFlightLegNotifRQ object, including flight leg details,
     * timestamp, and originator.
     *
     * @param sendOperationalFlightInternalEvent source Object
     * @return IATAAIDXFlightLegNotifRQ target Object
     */
    @Mapping(target = "version", expression = "java(mapVersion())")
    @Mapping(target = "flightLeg",
            expression = "java(populateFlightLegs(sendOperationalFlightInternalEvent))")
    @Mapping(target = "timeStamp", source = "sendOperationalFlightInternalEvent.messageTimeStamp")
    @Mapping(target = "originator.companyShortName", constant = SOFI)
    @Mapping(target = "transactionIdentifier", source = "soapHeader.messageID", qualifiedByName = "determineMessageId")
    IATAAIDXFlightLegNotifRQ mapToIATAAIDXFlightLegNotifRQ(
            SendOperationalFlightInternalEvent sendOperationalFlightInternalEvent, SoapHeader soapHeader
    );

    /**
     * This method is used to determine the MessageId
     *
     * @param messageId
     * @return
     */
    @Named("determineMessageId")
    default String determineMessageId(String messageId) {
        return Optional.ofNullable(messageId)
                .map(id -> id.substring(messageId.indexOf(COLON) + 1, messageId.length())
                        .replaceAll("-", "").trim())
                .orElse(null);
    }

    /**
     * Returns a constant version number as a BigDecimal.
     */
    default BigDecimal mapVersion() {
        return new BigDecimal(VERSION);
    }

    /**
     * Maps a SendOperationalFlightInternalEvent & flightLeg to an FlightLegType object, including legIdentifier,
     * legData.
     *
     * @param sendOperationalFlightInternalEvent source Object
     * @param flightLeg
     * @return FlightLegType target Object
     */
    @Mapping(target = "legIdentifier.airline.value",
            source = "sendOperationalFlightInternalEvent.operationalFlight.flightIdentifier.airlineCode")
    @Mapping(target = "legIdentifier.airline.codeContext", constant = CODE_CONTEXT)
    @Mapping(target = "legIdentifier.flightNumber", source = "sendOperationalFlightInternalEvent.operationalFlight" +
            ".flightIdentifier.flightNumber", qualifiedByName = "normalizeFlightNumber")
    @Mapping(target = "legIdentifier.operationalSuffix", source = "sendOperationalFlightInternalEvent.operationalFlight" +
            ".flightIdentifier.operationalSuffix")
    @Mapping(target = "legIdentifier.departureAirport.value", source = "flightLeg.departure.airport.code")
    @Mapping(target = "legIdentifier.departureAirport.codeContext", constant = CODE_CONTEXT)
    @Mapping(target = "legIdentifier.arrivalAirport.value", source = "flightLeg.arrival.airport.code")
    @Mapping(target = "legIdentifier.arrivalAirport.codeContext", constant = CODE_CONTEXT)
    @Mapping(target = "legIdentifier.originDate",
            source = "sendOperationalFlightInternalEvent.operationalFlight.flightIdentifier.scheduledDate")
    @Mapping(target = "legData.airportResources", source = "flightLeg", qualifiedByName = "mapAirportResources")
    FlightLegType populateFlightLeg(
            SendOperationalFlightInternalEvent sendOperationalFlightInternalEvent, FlightLeg flightLeg
    );
    
    /**
     * Maps a flightLeg to an AirportResources object
     *
     * @param flightLeg source Object
     * @return AirportResources target Object
     */
    @Mapping(target = "resource", source = "flightLeg", qualifiedByName = "mapResource")
    @Mapping(target = "usage", expression = "java(mapUsage())")
    FlightLegType.LegData.AirportResources populateAirportResources(FlightLeg flightLeg);

    /**
     * Returns an enum UsageType.
     */
    default UsageType mapUsage() {
        return UsageType.ACTUAL;
    }

    /**
     * Maps a flightLeg & departureOrArrival to a Resource object
     *
     * @param flightLeg source Object
     * @return Resource target Object
     */
    @Mapping(target = "passengerGate", expression = "java(mapPassengerGate(flightLeg,departureOrArrival))")
    @Mapping(target = "aircraftParkingPosition", expression = "java(mapAircraftParkingPosition(flightLeg,departureOrArrival))")
    @Mapping(target = "departureOrArrival", source = "departureOrArrival", qualifiedByName = "mapDepartureOrArrival")
    FlightLegType.LegData.AirportResources.Resource populateResource(FlightLeg flightLeg, String departureOrArrival);

    /**
     * Maps a code to an PassengerGate object
     *
     * @param code source Object
     * @return PassengerGate target Object
     */
    @Mapping(target = "value", source = "code")
    @Mapping(target = "repeatIndex", constant = REPEAT_INDEX)
    FlightLegType.LegData.AirportResources.Resource.PassengerGate populatePassengerGate(String code);

    /**
     * Maps a flightLeg to a list of PassengerGate
     *
     * @param flightLeg & departureOrArrival source Object
     * @return list of PassengerGate target Object
     */
    default List<FlightLegType.LegData.AirportResources.Resource.PassengerGate> mapPassengerGate(
            FlightLeg flightLeg, String departureOrArrival) {
        List<FlightLegType.LegData.AirportResources.Resource.PassengerGate> passengerGateList = new ArrayList<>();
        String gateCode;
        if (departureOrArrival.equals(DEPARTURE)) {
            gateCode = Optional.ofNullable(flightLeg)
                    .map(FlightLeg::getDeparture)
                    .map(Departure::getBoarding)
                    .map(Boarding::getLocation)
                    .map(Location::getGateCode)
                    .filter(gateCodes -> !gateCodes.isEmpty())
                    .map(gateCodes -> gateCodes.get(0))
                    .orElse(null);
        } else {
            gateCode = Optional.ofNullable(flightLeg)
                    .map(FlightLeg::getArrival)
                    .map(Arrival::getDisembarking)
                    .map(Disembarking::getLocation)
                    .map(Location::getGateCode)
                    .filter(gateCodes -> !gateCodes.isEmpty())
                    .map(gateCodes -> gateCodes.get(0))
                    .orElse(null);
        }
        if (gateCode != null && !gateCode.isEmpty()) {
            passengerGateList.add(populatePassengerGate(gateCode));
        }
        return passengerGateList;
    }

    /**
     * Maps a flightLeg to a AircraftParkingPosition.
     *
     * @param departureOrArrival source Object
     * @return AircraftParkingPosition target Object
     */
    @Mapping(target = "qualifier", constant = PUBLIC)
    @Mapping(target = "value", source = "departureOrArrival")
    FlightLegType.LegData.AirportResources.Resource.AircraftParkingPosition populateAircraftParkingPosition(
            String departureOrArrival);

    /**
     * This method is used to get the AircraftParkingPositionValue(
     *
     * @param flightLeg & departureOrArrival source Object
     * @return String
     */
    default String getAircraftParkingPositionValue(String departureOrArrival, FlightLeg flightLeg) {
        return departureOrArrival.equals(DEPARTURE) ?
                Optional.ofNullable(flightLeg)
                        .map(FlightLeg::getDeparture)
                        .map(Departure::getParkingPosition)
                        .map(ParkingPosition::getCode)
                        .orElse(null) :
                Optional.ofNullable(flightLeg)
                        .map(FlightLeg::getArrival)
                        .map(Arrival::getParkingPosition)
                        .map(ParkingPosition::getCode)
                        .orElse(null);
    }

    /**
     * Maps a flightLeg to a list of AircraftParkingPosition.
     *
     * @param flightLeg & departureOrArrival source Object
     * @return JAXBElement of AircraftParkingPosition
     */
    default JAXBElement<FlightLegType.LegData.AirportResources.Resource.AircraftParkingPosition> mapAircraftParkingPosition(
            FlightLeg flightLeg, String departureOrArrival) {
        String aircraftParkingPositionValue = getAircraftParkingPositionValue(departureOrArrival, flightLeg);
        if (aircraftParkingPositionValue != null) {
            return Optional.ofNullable(flightLeg)
                    .map(qualifier -> new JAXBElement<>(
                            new QName(NAMESPACE_URI, AIRCRAFT_PARKING_POSITION),
                            FlightLegType.LegData.AirportResources.Resource.AircraftParkingPosition.class,
                            populateAircraftParkingPosition(aircraftParkingPositionValue))
                    )
                    .orElse(null);
        }
        return null;
    }

    /**
     * Maps a departureOrArrival to a DepartureArrivalType.
     *
     * @param departureOrArrival
     * @return DepartureArrivalType
     */
    @Named("mapDepartureOrArrival")
    default DepartureArrivalType mapDepartureOrArrival(String departureOrArrival) {
        return departureOrArrival.equals("Departure") ? DepartureArrivalType.DEPARTURE : DepartureArrivalType.ARRIVAL;
    }

    /**
     * Maps a flightLeg to a list of Resource.
     *
     * @param flightLeg
     * @return list of Resource
     */
    @Named("mapResource")
    default List<FlightLegType.LegData.AirportResources.Resource> mapResource(FlightLeg flightLeg) {
        List<FlightLegType.LegData.AirportResources.Resource> resourceList = new ArrayList<>();
        FlightLegType.LegData.AirportResources.Resource departureResource = populateResource(
                flightLeg, DepartureArrivalType.DEPARTURE.value());
        String aircraftParkingPosition = getAircraftParkingPosition(departureResource);
        String passengerGate = getPassengerGate(departureResource);
        String runWay = getRunWay(departureResource);
        if (aircraftParkingPosition != null || passengerGate != null || runWay != null) {
            resourceList.add(departureResource);
        }
        FlightLegType.LegData.AirportResources.Resource arrivalResource = populateResource(
                flightLeg, DepartureArrivalType.ARRIVAL.value());
        String aircraftParkingPositionArrival = getAircraftParkingPosition(arrivalResource);
        String passengerGateArrival = getPassengerGate(arrivalResource);
        String runWayArrival = getRunWay(arrivalResource);
        if (aircraftParkingPositionArrival != null || passengerGateArrival != null || runWayArrival != null) {
            resourceList.add(arrivalResource);
        }
        return resourceList;
    }

    /**
     * This method is used to get the runway value
     *
     * @param resource
     * @return String value of runway
     */
    private static String getRunWay(FlightLegType.LegData.AirportResources.Resource resource) {
        return Optional.ofNullable(resource)
                .map(FlightLegType.LegData.AirportResources.Resource::getRunway)
                .map(JAXBElement::getValue)
                .orElse(null);
    }

    /**
     * This method is used to get the passengerGate value
     *
     * @param resource
     * @return String value of passengerGate
     */
    private static String getPassengerGate(FlightLegType.LegData.AirportResources.Resource resource) {
        return Optional.ofNullable(resource)
                .map(FlightLegType.LegData.AirportResources.Resource::getPassengerGate)
                .filter(passengerGates -> !passengerGates.isEmpty())
                .map(passengerGates -> passengerGates.get(0))
                .map(FlightLegType.LegData.AirportResources.Resource.PassengerGate::getValue)
                .orElse(null);
    }

    /**
     * This method is used to get the runway aircraftParkingPosition
     *
     * @param resource
     * @return String value of aircraftParkingPosition
     */
    private static String getAircraftParkingPosition(FlightLegType.LegData.AirportResources.Resource resource) {
        return Optional.ofNullable(resource)
                .map(FlightLegType.LegData.AirportResources.Resource::getAircraftParkingPosition)
                .map(JAXBElement::getValue)
                .map(FlightLegType.LegData.AirportResources.Resource.AircraftParkingPosition::getValue)
                .orElse(null);
    }

    /**
     * Maps a flightLeg to a list of Resource.
     *
     * @param flightLeg
     * @return list of Resource
     */
    @Named("mapAirportResources")
    default List<FlightLegType.LegData.AirportResources> mapAirportResources(FlightLeg flightLeg) {
        List<FlightLegType.LegData.AirportResources> airportResourcesList = new ArrayList<>();
        FlightLegType.LegData.AirportResources airportResources = populateAirportResources(flightLeg);
        if (airportResources != null && airportResources.getResource() != null && !airportResources.getResource().isEmpty()) {
            airportResourcesList.add(airportResources);
        }
        return airportResourcesList;
    }

    /**
     * Maps a flightLeg to a list of flightLegType.
     *
     * @param sendOperationalFlightInternalEvent
     * @return list of flightLegType
     */
    default List<FlightLegType> populateFlightLegs(SendOperationalFlightInternalEvent sendOperationalFlightInternalEvent) {
        return List.of(populateFlightLeg(sendOperationalFlightInternalEvent,
                sendOperationalFlightInternalEvent.getOperationalFlight().getFlightleg().get(0))
        );
    }

    /**
     * Normalizes a flight number to a standard format.
     *
     * <p>If the given flight number has a length of 4 or fewer characters, it is formatted
     * as a zero-padded three-digit string. For example:
     * - Input: "25" -> Output: "025"
     * - Input: "123" -> Output: "123"
     * - Input: "12345" -> Output: "12345" (unchanged, as it exceeds 4 characters)
     *
     * <p>This ensures consistent formatting for flight numbers, particularly those that
     * require three-digit padding.
     *
     * @param flightNumber the flight number to normalize, typically a string representation of a number
     * @return the normalized flight number as a string
     * @throws NumberFormatException if the input flight number cannot be parsed as an integer
     */
    @Named("normalizeFlightNumber")
    default String normalizeFlightNumber(String flightNumber) {
        return (flightNumber.length() <= 4) ? String.format(THREE_DIGIT_FORMAT,
                Integer.parseInt(flightNumber)) : flightNumber;
    }

}
