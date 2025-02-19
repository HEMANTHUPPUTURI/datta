package com.ibsplc.ops.afkl.day.service;

import com.ibsplc.ops.afkl.day.mapper.SOFIByKLMapper;
import com.ibsplc.si.event.schema.flightleg.notification.IATAAIDXFlightLegNotifRQ;
import com.ibsplc.si.event.schema.sofi.SendOperationalFlightInternalEvent;
import com.ibsplc.si.event.schema.sofi.SoapHeader;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class SOFIByKLServiceImpl {

    /**
     * Maps the SendOperationalFlightInternalEvent to IATAAIDXFlightLegNotifRQ Object
     *
     * @param sendOperationalFlightInternalEvent source object
     * @return IATAAIDXFlightLegNotifRQ
     */
    public IATAAIDXFlightLegNotifRQ mapToIATAAIDXFlightLegNotifRQ
    (SendOperationalFlightInternalEvent sendOperationalFlightInternalEvent, SoapHeader soapHeader) {
        IATAAIDXFlightLegNotifRQ iataAidxFlightLegNotifRQ =
                SOFIByKLMapper.INSTANCE.mapToIATAAIDXFlightLegNotifRQ(sendOperationalFlightInternalEvent, soapHeader);

        // Check if LegData is empty
        if (!iataAidxFlightLegNotifRQ.getFlightLeg().isEmpty() &&
                iataAidxFlightLegNotifRQ.getFlightLeg().get(0).getLegData() != null &&
                iataAidxFlightLegNotifRQ.getFlightLeg().get(0).getLegData().getAirportResources().isEmpty() &&
                iataAidxFlightLegNotifRQ.getFlightLeg().get(0).getLegData().getOperationTime().isEmpty()) {
            log.error("LegData is empty. AIDX message will not be processed further.");
            return null;
        }
        return iataAidxFlightLegNotifRQ;
    }
}

