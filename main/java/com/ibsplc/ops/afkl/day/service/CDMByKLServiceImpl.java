package com.ibsplc.ops.afkl.day.service;

import com.ibsplc.ops.afkl.day.mapper.CDMByKLMapper;
import com.ibsplc.si.event.schema.cdm.CDMFlightInfoType;
import com.ibsplc.si.event.schema.sofi.SendOperationalFlightInternalEvent;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.xml.bind.JAXBElement;
import java.util.Optional;

/**
 * The type Cdm by kl service.
 */
@Slf4j
@ApplicationScoped
@SuppressWarnings("unused")
public class CDMByKLServiceImpl {

    @ConfigProperty(name = "cdmflightinfotype.dates.cdmdates.result")
    private String result;

    /**
     * Maps the SendOperationalFlightInternalEvent to CDMFlightInfoType Object
     *
     * @param sendOperationalFlightInternalEvent source object
     * @return CDMFlightInfoType cdm flight info type
     */
    public CDMFlightInfoType mapToCDMFlightInfoType(SendOperationalFlightInternalEvent sendOperationalFlightInternalEvent) {
        CDMFlightInfoType cdmFlightInfoType = CDMByKLMapper.INSTANCE.mapToCDMFlightInfoType(sendOperationalFlightInternalEvent, result);
        boolean present = Optional.ofNullable(cdmFlightInfoType).map(CDMFlightInfoType::getDates).map(JAXBElement::getValue).map(CDMFlightInfoType.Dates::getCdmDate).filter(list -> !list.isEmpty()).isPresent();
        if (present) {
            return cdmFlightInfoType;
        } else {
            log.info(" CDM message cannot be published to iFlight since no CDMDates were found.");
            return null;
        }
    }
}

//Code Refactored to Skip publishing