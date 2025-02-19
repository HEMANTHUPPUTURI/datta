package com.ibsplc.ops.afkl.day.mapper;

import com.ibsplc.si.event.schema.cdm.CDMFlightInfoType;
import com.ibsplc.si.event.schema.cdm.CodeContextType;
import com.ibsplc.si.event.schema.cdm.FlightIDType;
import com.ibsplc.si.event.schema.sofi.Airport;
import com.ibsplc.si.event.schema.sofi.Arrival;
import com.ibsplc.si.event.schema.sofi.Departure;
import com.ibsplc.si.event.schema.sofi.FlightLeg;
import com.ibsplc.si.event.schema.sofi.OperationalFlight;
import com.ibsplc.si.event.schema.sofi.SendOperationalFlightInternalEvent;
import com.ibsplc.si.event.schema.sofi.Times;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.AIBT;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.ALDT;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.AMS;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.AOBT;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.ATOT;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.CDM;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.DATE;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.DATES;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.DATE_TIME_PATTERN;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.DATE_TIME_PATTERN_CDM;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.DATE_VALUE;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.EIBT;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.EOBT;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.ETDT;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.IOBT;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.NAMESPACE_URI;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.SIBT;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.SOBT;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.SOFI;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.UPDATE;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.THREE_DIGIT_FORMAT;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CDMByKLMapper {

    CDMByKLMapper INSTANCE = Mappers.getMapper(CDMByKLMapper.class);

    /**
     * Maps the SendOperationalFlightInternalEvent and a context result to a CDMFlightInfoType object.
     * - Maps the timestamp with a custom date formatting method.
     * - Sets the source field to a constant value.
     * - Maps nested flight details and additional fields.
     *
     * @param sendOperationalFlightInternalEvent the source event object.
     * @param result                             the context result.
     * @return the mapped CDMFlightInfoType object.
     */
    @Mapping(target = "timeStamp", source = "messageTimeStamp", qualifiedByName = "formatDateAndTime")
    @Mapping(target = "source", constant = SOFI)
    @Mapping(target = "flightID.airportSlot", source = "sendOperationalFlightInternalEvent",
            qualifiedByName = "mapAirportSlot")
    @Mapping(target = "dates", expression = "java(mapToDates(sendOperationalFlightInternalEvent,result))")
    @Mapping(target = "action", constant = UPDATE)
    @Mapping(target = "sequenceNmbr", expression = "java(mapSequenceNmbr())")
    CDMFlightInfoType mapToCDMFlightInfoType(
            SendOperationalFlightInternalEvent sendOperationalFlightInternalEvent, @Context String result
    );

    default BigInteger mapSequenceNmbr() {
        Random random = new Random();
        int number = 1000000 + random.nextInt(9000000);
        return new BigInteger(String.valueOf(number));
    }

    /**
     * Converts an XMLGregorianCalendar to a formatted string using a specified pattern.
     *
     * @param date the XMLGregorianCalendar object.
     * @return the formatted date string.
     */
    @Named("formatDateAndTime")
    default String mapTimeStamp(XMLGregorianCalendar date) {
        LocalDateTime localDateTime = date.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
        return localDateTime.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    }

    /**
     * Populates the Dates object in CDMFlightInfoType by mapping flight leg and result data.
     *
     * @param flightLeg the flight leg details.
     * @param result    the context result.
     * @return the populated Dates object.
     */
    @Mapping(target = "cdmDate", expression = "java(mapToCdmDate(flightLeg,result))")
    CDMFlightInfoType.Dates populateDates(FlightLeg flightLeg, String result);

    /**
     * Maps an XMLGregorianCalendar to a JAXBElement containing a DateValue object.
     *
     * @param date the XMLGregorianCalendar object.
     * @return the JAXBElement containing the DateValue object.
     */
    @Named("mapDateValue")
    default JAXBElement<CDMFlightInfoType.Dates.CdmDate.DateValue> mapDateValue(XMLGregorianCalendar date) {
        String dateValue = formatDateAndTime(date);
        return new JAXBElement<>(new QName(NAMESPACE_URI, DATE_VALUE),
                CDMFlightInfoType.Dates.CdmDate.DateValue.class, populateDateValue(dateValue));
    }

    /**
     * Creates a DateValue object with the specified date string.
     *
     * @param dateValue the date string.
     * @return the populated DateValue object.
     */
    @Mapping(target = "value", source = "dateValue")
    CDMFlightInfoType.Dates.CdmDate.DateValue populateDateValue(String dateValue);

    /**
     * Converts an XMLGregorianCalendar to a formatted string using a specified pattern.
     *
     * @param date the XMLGregorianCalendar object.
     * @return the formatted date string.
     */
    @Named("formatDateAndTimes")
    default String formatDateAndTime(XMLGregorianCalendar date) {
        LocalDateTime localDateTime = date.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
        return localDateTime.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN_CDM));
    }

    /**
     * Populates a CdmDate object with specified attributes such as result, date, name, and type.
     *
     * @param result   the context result.
     * @param date     the XMLGregorianCalendar date.
     * @param dateName the name of the date.
     * @param dateType the type of the date.
     * @return the populated CdmDate object.
     */
    @Mapping(target = "result", source = "result")
    @Mapping(target = "source", constant = CDM)
    @Mapping(target = "message", constant = CDM)
    @Mapping(target = "dateType", source = "dateType")
    @Mapping(target = "dateName", source = "dateName")
    @Mapping(target = "dateValue", source = "date", qualifiedByName = "mapDateValue")
    CDMFlightInfoType.Dates.CdmDate populateCdmDate(
            String result, XMLGregorianCalendar date, String dateName, String dateType
    );

    /**
     * Maps the flight leg to a list of CdmDate objects by extracting relevant date attributes.
     *
     * @param flightLeg the flight leg details.
     * @param result    the context result.
     * @return a list of CdmDate objects.
     */
    default List<CDMFlightInfoType.Dates.CdmDate> mapToCdmDate(FlightLeg flightLeg, String result) {
        List<CDMFlightInfoType.Dates.CdmDate> cdmDateList = new ArrayList<>();

        XMLGregorianCalendar aldt = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getDeparture)
                .map(Departure::getTimes)
                .map(Times::getActualTouchDownTime)
                .orElse(null);

        XMLGregorianCalendar eldt = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getDeparture)
                .map(Departure::getTimes)
                .map(Times::getEstimatedTouchDownTime)
                .orElse(null);

        XMLGregorianCalendar sobt = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getDeparture)
                .map(Departure::getTimes)
                .map(Times::getScheduledDateTime)
                .orElse(null);

        XMLGregorianCalendar aobt = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getDeparture)
                .map(Departure::getTimes)
                .map(Times::getActualDateTime)
                .orElse(null);

        XMLGregorianCalendar atot = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getDeparture)
                .map(Departure::getTimes)
                .map(Times::getActualTakeOffTime)
                .orElse(null);

        XMLGregorianCalendar eobt = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getDeparture)
                .map(Departure::getTimes)
                .map(Times::getEstimatedDateTimeInternal)
                .orElse(null);

        XMLGregorianCalendar sibt = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getArrival)
                .map(Arrival::getTimes)
                .map(Times::getScheduledDateTime)
                .orElse(null);

        XMLGregorianCalendar eibt = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getArrival)
                .map(Arrival::getTimes)
                .map(Times::getEstimatedDateTime)
                .orElse(null);

        XMLGregorianCalendar aibt = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getArrival)
                .map(Arrival::getTimes)
                .map(Times::getActualDateTime)
                .orElse(null);

        String arrivalAirportCode = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getArrival)
                .map(Arrival::getAirport)
                .map(Airport::getCode)
                .orElse(null);

        String departureAirportCode = Optional.ofNullable(flightLeg)
                .map(FlightLeg::getDeparture)
                .map(Departure::getAirport)
                .map(Airport::getCode)
                .orElse(null);

        boolean isArrivalAMS = arrivalAirportCode != null && !arrivalAirportCode.equals(AMS);
        boolean isDepartureAMS = departureAirportCode != null && !departureAirportCode.equals(AMS);

        if (sibt != null && isArrivalAMS) {
            cdmDateList.add(populateCdmDate(result, sibt, SIBT, DATE));
        }
        if (eibt != null && isArrivalAMS) {
            cdmDateList.add(populateCdmDate(result, eibt, EIBT, DATE));
        }
        if (aibt != null && isArrivalAMS) {
            cdmDateList.add(populateCdmDate(result, aibt, AIBT, DATE));
        }
        if (aldt != null && isDepartureAMS) {
            cdmDateList.add(populateCdmDate(result, aldt, ALDT, DATE));
        }
        if (eldt != null && isDepartureAMS) {
            cdmDateList.add(populateCdmDate(result, eldt, ETDT, DATE));
        }
        if (sobt != null && isDepartureAMS) {
            cdmDateList.add(populateCdmDate(result, sobt, SOBT, DATE));
        }
        if (aobt != null && isDepartureAMS) {
            cdmDateList.add(populateCdmDate(result, aobt, AOBT, DATE));
        }
        if (atot != null && isDepartureAMS) {
            cdmDateList.add(populateCdmDate(result, atot, ATOT, DATE));
        }
        if (eobt != null && isDepartureAMS) {
            cdmDateList.add(populateCdmDate(result, eobt, EOBT, DATE));
        }
        return cdmDateList;
    }

    /**
     * Maps operational flight and arrival airport details to a Dates object if conditions are met.
     *
     * @param event  the operational flight event.
     * @param result the context result.
     * @return the mapped JAXBElement of Dates or null if no mapping occurs.
     */
    default JAXBElement<CDMFlightInfoType.Dates> mapToDates(
            SendOperationalFlightInternalEvent event, String result) {
        CDMFlightInfoType.Dates dates;
        String arrivalAirportCode = Optional.ofNullable(event)
                .map(SendOperationalFlightInternalEvent::getOperationalFlight)
                .map(OperationalFlight::getFlightleg)
                .map(flightLegList -> flightLegList.get(0))
                .map(FlightLeg::getArrival)
                .map(Arrival::getAirport)
                .map(Airport::getCode)
                .orElse(null);
        String departureAirportCode = Optional.ofNullable(event)
                .map(SendOperationalFlightInternalEvent::getOperationalFlight)
                .map(OperationalFlight::getFlightleg)
                .map(flightLegList -> flightLegList.get(0))
                .map(FlightLeg::getDeparture)
                .map(Departure::getAirport)
                .map(Airport::getCode)
                .orElse(null);
        if (arrivalAirportCode != null || departureAirportCode != null) {
            dates = populateDates(event.getOperationalFlight().getFlightleg().get(0), result);
        } else {
            dates = null;
        }
        return Optional.ofNullable(dates)
                .map(CDMFlightInfoType.Dates::getCdmDate)
                .filter(cdmDates -> !cdmDates.isEmpty())
                .map(cdmDates -> new JAXBElement<>(new QName(NAMESPACE_URI, DATES), CDMFlightInfoType.Dates.class, dates))
                .orElse(null);
    }

    /**
     * Maps the operational flight and flight leg details to a FlightIDType object.
     *
     * @param sendOperationalFlightInternalEvent the operational flight event.
     * @param flightLeg                          the flight leg details.
     * @return the mapped FlightIDType object.
     */
    @Mapping(target = "airline.value",
            source = "sendOperationalFlightInternalEvent.operationalFlight.flightIdentifier.airlineCode")
    @Mapping(target = "airline.codeContext",
            source = "sendOperationalFlightInternalEvent.operationalFlight.flightIdentifier.airlineCode",
            qualifiedByName = "mapCodeContext")
    @Mapping(target = "flightNumber",
            source = "sendOperationalFlightInternalEvent.operationalFlight.flightIdentifier.flightNumber",
            qualifiedByName = "normalizeFlightNumber")
    @Mapping(target = "flightDate.value",
            source = "sendOperationalFlightInternalEvent.operationalFlight.flightIdentifier.scheduledDate",
            qualifiedByName = "formatDateAndTimes")
    @Mapping(target = "flightDate.dateID", constant = IOBT)
    @Mapping(target = "arrivalAirport.value", source = "flightLeg.arrival.airport.code")
    @Mapping(target = "arrivalAirport.codeContext", source = "flightLeg.arrival.airport.code",
            qualifiedByName = "mapCodeContext")
    @Mapping(target = "departureAirport.value", source = "flightLeg.departure.airport.code")
    @Mapping(target = "departureAirport.codeContext", source = "flightLeg.departure.airport.code",
            qualifiedByName = "mapCodeContext")
    @Mapping(target = "departureAirport.messageIssued", expression = "java(mapMessageIssued())")
    FlightIDType mapToFlightIDType(
            SendOperationalFlightInternalEvent sendOperationalFlightInternalEvent, FlightLeg flightLeg
    );

    default boolean mapMessageIssued() {
        return true;
    }

    @Named("mapCodeContext")
    default CodeContextType mapCodeContext(String airportCode) {
        return airportCode != null ? CodeContextType.IATA : null;
    }

    /**
     * Maps the operational flight event to a FlightIDType object by using the first flight leg.
     *
     * @param sendOperationalFlightInternalEvent the operational flight event.
     * @return the mapped FlightIDType object.
     */
    @Named("mapAirportSlot")
    default FlightIDType mapAirportSlot(SendOperationalFlightInternalEvent sendOperationalFlightInternalEvent) {
        return mapToFlightIDType(sendOperationalFlightInternalEvent,
                sendOperationalFlightInternalEvent.getOperationalFlight().getFlightleg().get(0));
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
