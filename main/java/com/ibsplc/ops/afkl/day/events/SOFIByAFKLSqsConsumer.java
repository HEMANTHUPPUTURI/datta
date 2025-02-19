package com.ibsplc.ops.afkl.day.events;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.ibsplc.ops.afkl.day.enums.ErrorCodes;
import com.ibsplc.ops.afkl.day.processor.SOFIByAFProcessor;
import com.ibsplc.ops.afkl.day.processor.SOFIByKLProcessor;
import com.ibsplc.ops.afkl.day.util.XmlParserUtil;
import com.ibsplc.ops.afkl.day.util.XmlValidationUtil;
import com.ibsplc.si.event.schema.sofi.SendOperationalFlightInternalEvent;
import com.ibsplc.si.event.schema.sofi.SoapHeader;
import com.ibsplc.si.framework.exception.CustomException;
import com.ibsplc.si.framework.util.LoggerUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import javax.xml.bind.JAXBException;

import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.AF;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.KL;
import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.SOFI_XSD;

/**
 * Handler class for processing messages from SQS and transforming the message and finally publishing them to an AWS SQS queue.
 *
 * <p>This class is designed to be triggered by an AWS SQSEvent, serving as the entry point
 * for the AWS Lambda function. It consumes messages from an SQS queue, processes them, and
 * publishes the processed messages to an AWS SQS queue for downstream consumption.
 *
 * <p>This handler uses the AWS Lambda `RequestHandler` interface and is integrated with the
 * Quarkus framework for dependency injection and logging.
 *
 * @see com.amazonaws.services.lambda.runtime.RequestHandler
 */
@Slf4j
@ApplicationScoped
@Named("SOFIByAFKLSqsConsumer")
@SuppressWarnings("unused")
public class SOFIByAFKLSqsConsumer implements RequestHandler<SQSEvent, Void> {

    @Inject
    private SOFIByAFProcessor sofiByAFProcessor;

    @Inject
    private SOFIByKLProcessor sofiByKLProcessor;

    @Inject
    private XmlParserUtil xmlParserUtil;

    /**
     * Lambda Request Handler for SQS Consumer
     * if we receive a KL message this method will generate one AIDX message & CDM message
     * if we receive a AF message this method will generate only one AF message
     *
     * @param sqsEvent - The event to be passed to this lambda.
     * @param context  - The context variable for this lambda instance
     */
    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {

        sqsEvent.getRecords().forEach(sqsMessage -> {
            try {
                String message = sqsMessage.getBody();
                log.info(" Received message: {}", LoggerUtility.sanitizeMessage(message));
                String messageBody = xmlParserUtil.extractXmlPartsFromSoapMessage(
                        message, Boolean.TRUE
                );
                boolean isInputXmlValid = XmlValidationUtil.validateXMLAgainstXSD(messageBody, SOFI_XSD);
                if (!isInputXmlValid) {
                    log.error(" XSD validation error in the input message. Message will not" +
                            " be processed further: \n{}", LoggerUtility.sanitizeMessage(message));
                    throw new CustomException("", ErrorCodes.E0002.name(), ErrorCodes.E0002.getDescription(),
                            new Exception("XSD validation error in the input message"));
                }
                SendOperationalFlightInternalEvent sendOperationalFlightInternalEvent =
                        xmlParserUtil.parseXmlToObject(
                                SendOperationalFlightInternalEvent.class, messageBody
                        );
                SoapHeader soapHeader = xmlParserUtil.parseSoapHeaderFromXml(
                        xmlParserUtil.extractXmlPartsFromSoapMessage(message, Boolean.FALSE));
                if (ObjectUtils.isNotEmpty(sendOperationalFlightInternalEvent)) {
                    String airlineCode = sendOperationalFlightInternalEvent.getOperationalFlight().getFlightIdentifier()
                            .getAirlineCode();
                    if (airlineCode.equals(AF)) {
                        log.info(" Received message from AF");
                        sofiByAFProcessor.processSendOperationalFlightInternalEvent(sendOperationalFlightInternalEvent, soapHeader);
                    } else if (airlineCode.equals(KL)) {
                        log.info(" Received message from KL");
                        sofiByKLProcessor.processSendOperationalFlightInternalEvent(sendOperationalFlightInternalEvent, soapHeader);
                    } else {
                        log.error(" Received message is neither AF nor KL. Cannot process the message further.");
                        throw new CustomException(null, ErrorCodes.E0001.name(), ErrorCodes.E0001.getDescription(), null);
                    }
                } else {
                    log.error(" SOFI input message parsing failed!! ");
                }
            } catch (JAXBException jaxbException) {
                log.error(" An error occurred while generating the output xml", jaxbException);
            } catch (Exception exception) {
                log.error(" Unexpected exception occurred:", exception);
            }
        });
        return null;
    }

}
