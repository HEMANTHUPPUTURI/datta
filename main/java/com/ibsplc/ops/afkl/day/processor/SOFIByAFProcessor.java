package com.ibsplc.ops.afkl.day.processor;

import com.ibsplc.ops.afkl.day.publisher.SOFIByAFKLPublisher;
import com.ibsplc.ops.afkl.day.service.SOFIByAFServiceImpl;
import com.ibsplc.ops.afkl.day.util.XmlGenerationUtil;
import com.ibsplc.ops.afkl.day.util.XmlValidationUtil;
import com.ibsplc.si.event.schema.flightleg.notification.IATAAIDXFlightLegNotifRQ;
import com.ibsplc.si.event.schema.sofi.SendOperationalFlightInternalEvent;
import com.ibsplc.si.event.schema.sofi.SoapHeader;
import com.ibsplc.si.framework.util.LoggerUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.xml.bind.JAXBException;

import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.IATA_AIDX_FLIGHTLEGNOTIFRQ_OUTPUT_XSD;

/**
 * this class is for processing the received AF message.
 */
@Slf4j
@ApplicationScoped
@SuppressWarnings("unused")
public class SOFIByAFProcessor {

    @ConfigProperty(name = "sofi.aidx.af.output.sqs.url")
    private String afOutputSqsUrl;

    @Inject
    private SOFIByAFServiceImpl sofiByAFKLService;

    @Inject
    private SOFIByAFKLPublisher sofiByAFKLPublisher;

    /**
     * Process the received SendOperationalFlightInternalEvent and produces IATAAIDXFlightLegNotifRQ object
     *
     * @param sendOperationalFlightInternalEvent event
     * @throws JAXBException if an error occurs while creating JAXBContext
     */
    public void processSendOperationalFlightInternalEvent
    (SendOperationalFlightInternalEvent sendOperationalFlightInternalEvent, SoapHeader soapHeader) throws JAXBException {
        log.info(" Transforming the received AF SOFI message to AIDX message");
        IATAAIDXFlightLegNotifRQ iataAidxFlightLegNotifRQ = sofiByAFKLService.mapToIATAAIDXFlightLegNotifRQ(
                sendOperationalFlightInternalEvent, soapHeader);
        if (iataAidxFlightLegNotifRQ != null) {
            String flightLegNotificationRQXml = XmlGenerationUtil.generateXmlFromObj(
                    iataAidxFlightLegNotifRQ, IATAAIDXFlightLegNotifRQ.class
            );
            log.info(" Successfully Transformed the received AF SOFI message to AIDX message");
            boolean isOutputXmlValid = XmlValidationUtil.validateXMLAgainstXSD(flightLegNotificationRQXml,
                    IATA_AIDX_FLIGHTLEGNOTIFRQ_OUTPUT_XSD);
            if (isOutputXmlValid) {
                sofiByAFKLPublisher.publish(flightLegNotificationRQXml, afOutputSqsUrl, "AF AIDX");
            } else {
                log.error(" XSD validation error in the AF AIDX output message. Hence, the following message will not" +
                        " be published to the outbound SQS: {}", LoggerUtility.sanitizeMessage(flightLegNotificationRQXml));
            }
        }
    }
}
