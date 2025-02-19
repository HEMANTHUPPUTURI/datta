package com.ibsplc.ops.afkl.day.mapper;

import com.ibsplc.si.event.schema.flightleg.notification.DepartureArrivalType;
import com.ibsplc.si.event.schema.flightleg.notification.FlightLegType;
import com.ibsplc.si.event.schema.flightleg.notification.IATAAIDXFlightLegNotifRQ;
import com.ibsplc.si.event.schema.flightleg.notification.UsageType;
import com.ibsplc.si.event.schema.sofi.Airport;
import com.ibsplc.si.event.schema.sofi.Arrival;
import com.ibsplc.si.event.schema.sofi.Boarding;
import com.ibsplc.si.event.schema.sofi.Departure;
import com.ibsplc.si.event.schema.sofi.Disembarking;
import com.ibsplc.si.event.schema.sofi.FlightHandlingTimes;
import com.ibsplc.si.event.schema.sofi.FlightLeg;
import com.ibsplc.si.event.schema.sofi.Location;
import com.ibsplc.si.event.schema.sofi.ParkingPosition;
import com.ibsplc.si.event.schema.sofi.SendOperationalFlightInternalEvent;
import com.ibsplc.si.event.schema.sofi.SoapHeader;
import com.ibsplc.si.event.schema.sofi.Times;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.ACT;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.AIRCRAFT_PARKING_POSITION;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.AMS;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.CODE_CONTEXT;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.COLON;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.DEPARTURE;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.NAMESPACE_URI;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.OFB;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.OPERATION_TIME_CODE_CONTEXT;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.OPERATION_TIME_DATE_TIME_FORMAT;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.PUBLIC;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.REPEAT_INDEX;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.RUNWAY;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.SAT;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.SOFI;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.SRT;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.TAR;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.THREE_DIGIT_FORMAT;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.TKO;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.VERSION;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SOFIByKLMapper {

    SOFIByKLMapper INSTANCE = Mappers.getMapper(SOFIByKLMapper.class);

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
    @Mapping(target = "legData.operationTime", source = "flightLeg", qualifiedByName = "mapOperationTime")
    FlightLegType populateFlightLeg(
            SendOperationalFlightInternalEvent sendOperationalFlightInternalEvent, FlightLeg flightLeg
    );

    /**
     * This method is used to convert the XMLGregorianCalendar date to string
     *
     * @param date
     * @return String
     */
    @Named("mapOperationTimeValue")
    default String mapOperationTimeValue(XMLGregorianCalendar date) {
        return date.toGregorianCalendar().toZonedDateTime().toLocalDateTime()
                .format(DateTimeFormatter.ofPattern(OPERATION_TIME_DATE_TIME_FORMAT));
    }

    /**
     * Populates the OperationTime object
     *
     * @return OperationTime target Object
     */
    @Mapping(target = "value", source = "date", qualifiedByName = "mapOperationTimeValue")
    @Mapping(target = "operationQualifier", source = "operationQualifier")
    @Mapping(target = "codeContext", constant = OPERATION_TIME_CODE_CONTEXT)
    @Mapping(target = "repeatIndex", source = "repeatIndex")
    @Mapping(target = "timeType", source = "timeType")
    FlightLegType.LegData.OperationTime populateOperationTime(
            XMLGregorianCalendar date, String operationQualifier, int repeatIndex, String timeType
    );

    /**
     * This method creates a list of operationTime
     *
     * @return list of OperationTime
     */
    @Named("mapOperationTime")
    default List<FlightLegType.LegData.OperationTime> mapOperationTime(FlightLeg flightLeg) {
        String departureCode = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getDeparture)
                .map(Departure::getAirport)
                .map(Airport::getCode)
                .orElse(null);
        List<FlightLegType.LegData.OperationTime> operationTimeList = new ArrayList<>();
        if (departureCode != null && !departureCode.equals(AMS)) {
            int[] counter = {1};
            FlightHandlingTimes flightHandlingTimes = Optional.ofNullable(flightLeg)
                    .map(FlightLeg::getDeparture)
                    .map(Departure::getFlightHandlingTimes)
                    .orElse(null);
            Times times = Optional.ofNullable(flightLeg)
                    .map(FlightLeg::getDeparture)
                    .map(Departure::getTimes)
                    .orElse(null);
            if (flightHandlingTimes != null) {
                Optional.of(flightHandlingTimes)
                        .map(FlightHandlingTimes::getTargetTakeOffTime)
                        .ifPresent(targetTakeOffTime -> {
                            operationTimeList.add(populateOperationTime(targetTakeOffTime, TKO, counter[0], TAR));
                            counter[0]++;
                        });
                Optional.of(flightHandlingTimes)
                        .map(FlightHandlingTimes::getActualStartupRequestTime)
                        .ifPresent(actualStartupRequestTime -> {
                            operationTimeList.add(populateOperationTime(actualStartupRequestTime, SRT, counter[0], ACT));
                            counter[0]++;
                        });
            }
            if (times != null) {
                Optional.of(times)
                        .map(Times::getTargetStartupApprovalTime)
                        .ifPresent(targetStartupApprovalTime -> {
                            operationTimeList.add(populateOperationTime(targetStartupApprovalTime, SAT, counter[0], TAR));
                            counter[0]++;
                        });
                Optional.of(times)
                        .map(Times::getTargetOffBlockTime)
                        .ifPresent(targetOffBlockTime -> {
                            operationTimeList.add(populateOperationTime(targetOffBlockTime, OFB, counter[0], TAR));
                            counter[0]++;
                        });
            }
        }
        return operationTimeList;
    }

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
     * Maps a flightLeg to JAXBElement
     *
     * @param flightLeg source Object
     * @return JAXBElement of string
     */
    default JAXBElement<String> mapRunway(FlightLeg flightLeg, String departureOrArrival, boolean isAms) {
        if (isAms) {
            return null;
        }
        if (departureOrArrival.equals(DEPARTURE)) {
            String takeOffRunwayCode = Optional.ofNullable(flightLeg)
                    .map(FlightLeg::getDeparture)
                    .map(Departure::getTakeOffRunwayCode)
                    .orElse(null);
            return takeOffRunwayCode != null
                    ? new JAXBElement<>(new QName(NAMESPACE_URI, RUNWAY), String.class, takeOffRunwayCode) : null;
        } else {
            String landingRunwayCode = Optional.ofNullable(flightLeg)
                    .map(FlightLeg::getArrival)
                    .map(Arrival::getLandingRunwayCode)
                    .orElse(null);
            return landingRunwayCode != null ?
                    new JAXBElement<>(new QName(NAMESPACE_URI, RUNWAY), String.class, landingRunwayCode) : null;
        }
    }

    /**
     * Maps a flightLeg & departureOrArrival to an Resource object
     *
     * @param flightLeg source Object
     * @return Resource target Object
     */
    @Mapping(target = "runway", expression = "java(mapRunway(flightLeg,departureOrArrival,isAms))")
    @Mapping(target = "passengerGate", expression = "java(mapPassengerGate(flightLeg,departureOrArrival))")
    @Mapping(target = "aircraftParkingPosition", expression = "java(mapAircraftParkingPosition(flightLeg,departureOrArrival))")
    @Mapping(target = "departureOrArrival", source = "departureOrArrival", qualifiedByName = "mapDepartureOrArrival")
    FlightLegType.LegData.AirportResources.Resource populateResource(FlightLeg flightLeg, String departureOrArrival, boolean isAms);

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
        return departureOrArrival.equals(DEPARTURE) ? DepartureArrivalType.DEPARTURE : DepartureArrivalType.ARRIVAL;
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
        String departureCode = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getDeparture)
                .map(Departure::getAirport)
                .map(Airport::getCode)
                .orElse(null);

        String arrivalCode = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getArrival)
                .map(Arrival::getAirport)
                .map(Airport::getCode)
                .orElse(null);

        if (departureCode != null && !departureCode.equals(AMS)) {
            FlightLegType.LegData.AirportResources.Resource resource = populateResource(
                    flightLeg, DepartureArrivalType.DEPARTURE.value(), false
            );
            String aircraftParkingPosition = getAircraftParkingPosition(resource);
            String passengerGate = getPassengerGate(resource);
            String runWay = getRunWay(resource);
            if (aircraftParkingPosition != null || passengerGate != null || runWay != null) {
                resourceList.add(resource);
            }
        } else {
            FlightLegType.LegData.AirportResources.Resource resource = populateResource(
                    flightLeg, DepartureArrivalType.DEPARTURE.value(), true
            );
            String aircraftParkingPosition = getAircraftParkingPosition(resource);
            String passengerGate = getPassengerGate(resource);
            String runWay = getRunWay(resource);
            if (aircraftParkingPosition != null || passengerGate != null || runWay != null) {
                resourceList.add(resource);
            }
        }
        if (arrivalCode != null && !arrivalCode.equals(AMS)) {
            FlightLegType.LegData.AirportResources.Resource resource = populateResource(
                    flightLeg, DepartureArrivalType.ARRIVAL.value(), false
            );
            String aircraftParkingPosition = getAircraftParkingPosition(resource);
            String passengerGate = getPassengerGate(resource);
            String runWay = getRunWay(resource);
            if (aircraftParkingPosition != null || passengerGate != null || runWay != null) {
                resourceList.add(resource);
            }
        } else {
            FlightLegType.LegData.AirportResources.Resource resource = populateResource(
                    flightLeg, DepartureArrivalType.ARRIVAL.value(), true
            );
            String aircraftParkingPosition = getAircraftParkingPosition(resource);
            String passengerGate = getPassengerGate(resource);
            if (aircraftParkingPosition != null || passengerGate != null) {
                resourceList.add(resource);
            }
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
