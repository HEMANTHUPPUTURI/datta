package com.ibsplc.ops.afkl.day.processor;

import com.ibsplc.ops.afkl.day.publisher.SOFIByAFKLPublisher;
import com.ibsplc.ops.afkl.day.service.CDMByKLServiceImpl;
import com.ibsplc.ops.afkl.day.service.SOFIByKLServiceImpl;
import com.ibsplc.ops.afkl.day.util.XmlGenerationUtil;
import com.ibsplc.ops.afkl.day.util.XmlValidationUtil;
import com.ibsplc.si.event.schema.cdm.CDMFlightInfoType;
import com.ibsplc.si.event.schema.flightleg.notification.IATAAIDXFlightLegNotifRQ;
import com.ibsplc.si.event.schema.sofi.SendOperationalFlightInternalEvent;
import com.ibsplc.si.event.schema.sofi.SoapHeader;
import com.ibsplc.si.framework.util.LoggerUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.CDM_FLIGHT_INFO;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.CDM_FLIGHT_INFO_XSD;
import static com.ibsplc.ops.afkl.day.constants.CDMByKLConstants.NAMESPACE_URI;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.IATA_AIDX_FLIGHTLEGNOTIFRQ_OUTPUT_XSD;

@Slf4j
@ApplicationScoped
@SuppressWarnings("unused")
public class SOFIByKLProcessor {

    @ConfigProperty(name = "sofi.aidx.kl.output.sqs.url")
    private String sofiAIDXOutputSqsURL;

    @ConfigProperty(name = "sofi.cdm.kl.output.sqs.url")
    private String sofiCDMOutputSqsURL;

    @Inject
    private SOFIByKLServiceImpl sofiByKLService;

    @Inject
    private SOFIByAFKLPublisher sofiByAFKLPublisher;

    @Inject
    private CDMByKLServiceImpl cdmByKLServiceImpl;

    /**
     * Process the received SendOperationalFlightInternalEvent and produces IATAAIDXFlightLegNotifRQ
     * and CDMFlightInfoType object
     *
     * @param sendOperationalFlightInternalEvent event
     * @throws Exception if an error occurs while creating JAXBContext
     */
    public void processSendOperationalFlightInternalEvent
    (SendOperationalFlightInternalEvent sendOperationalFlightInternalEvent, SoapHeader soapHeader) throws JAXBException {
        log.info(" Transforming the received KL SOFI message to AIDX & CDM message");
        IATAAIDXFlightLegNotifRQ iataAidxFlightLegNotifRQ =
                sofiByKLService.mapToIATAAIDXFlightLegNotifRQ(sendOperationalFlightInternalEvent, soapHeader);
        CDMFlightInfoType cdmFlightInfoType =
                cdmByKLServiceImpl.mapToCDMFlightInfoType(sendOperationalFlightInternalEvent);
        if (iataAidxFlightLegNotifRQ != null) {
            String flightLegNotificationRQXml =
                    XmlGenerationUtil.generateXmlFromObj(iataAidxFlightLegNotifRQ, IATAAIDXFlightLegNotifRQ.class);
            validateXmlAndPublishToSqs(flightLegNotificationRQXml,
                    IATA_AIDX_FLIGHTLEGNOTIFRQ_OUTPUT_XSD, "KL AIDX", sofiAIDXOutputSqsURL
            );
        }
        if (cdmFlightInfoType != null) {
            QName cdmFlightInfoTypeQName = new QName(NAMESPACE_URI, CDM_FLIGHT_INFO);
            JAXBElement<CDMFlightInfoType> jaxbElement =
                    new JAXBElement<>(cdmFlightInfoTypeQName, CDMFlightInfoType.class, cdmFlightInfoType);
            String cdmFlightInfoTypeXml = XmlGenerationUtil.generateXmlFromObj(jaxbElement, CDMFlightInfoType.class);
            validateXmlAndPublishToSqs(cdmFlightInfoTypeXml, CDM_FLIGHT_INFO_XSD, "KL CDM",
                    sofiCDMOutputSqsURL);
        }
    }

    /**
     * Validates the received message and publishes it to SQS
     *
     * @param message     output xml
     * @param xsd         output XSD
     * @param messageType messageType
     * @param sqsUrl      outputSQSURl
     */
    private void validateXmlAndPublishToSqs(String message, String xsd, String messageType, String sqsUrl) {

        boolean isOutputXmlValid = XmlValidationUtil.validateXMLAgainstXSD(message, xsd);
        if (isOutputXmlValid) {
            sofiByAFKLPublisher.publish(message, sqsUrl, messageType);
        } else {
            log.error(" XSD validation error in the {} output message. Hence, the following message will not" +
                    " be published to the outbound SQS: {}", messageType, LoggerUtility.sanitizeMessage(message));
        }

    }

}
