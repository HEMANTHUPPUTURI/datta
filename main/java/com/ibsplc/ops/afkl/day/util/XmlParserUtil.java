package com.ibsplc.ops.afkl.day.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import com.ibsplc.ops.afkl.day.enums.ErrorCodes;
import com.ibsplc.si.event.schema.sofi.SoapHeader;
import com.ibsplc.si.framework.exception.CustomException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static com.ibsplc.ops.afkl.day.constants.SOFIByAFKLConstants.NEW_LINE;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * This class is used to map SendOperationalFlightInternalEvent from input xml
 */
@Slf4j
@ApplicationScoped
@SuppressWarnings("deprecation")
public final class XmlParserUtil {

    private XmlParserUtil() {
        super();
    }

    /**
     * This is used to map sourceClass object from input xml
     *
     * @param message the message
     * @return the sourceClass object
     */
    public <T> T parseXmlToObject(Class<T> sourceClass, String message) {

        T result = null;
        try {
            JacksonXmlModule module = new JacksonXmlModule();
            module.setDefaultUseWrapper(false);
            XmlMapper xmlMapper = new XmlMapper(module);
            xmlMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            xmlMapper.registerModule(new JakartaXmlBindAnnotationModule());
            result = xmlMapper.readValue(message, sourceClass);
        } catch (Exception exception) {
            log.error(" Error while convertXMLToObject: {} ", getStackTrace(exception));
        }
        return result;
    }

    /**
     * This method is used to parse the xml
     *
     * @param headerXml
     * @return
     */
    public SoapHeader parseSoapHeaderFromXml(String headerXml) {
        SoapHeader soapHeader = null;
        try {
            headerXml = "<SoapHeader>\n" + headerXml + "</SoapHeader>";
            JacksonXmlModule module = new JacksonXmlModule();
            module.setDefaultUseWrapper(false);
            XmlMapper xmlMapper = new XmlMapper(module);
            xmlMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            xmlMapper.registerModule(new JakartaXmlBindAnnotationModule());
            soapHeader = xmlMapper.readValue(headerXml, SoapHeader.class);
        } catch (Exception exception) {
            log.error(" Error while parsing SoapHeader: {} ", getStackTrace(exception));
        }
        return soapHeader;
    }

    /**
     * This method is used to extract the soapHeader & soapBody
     *
     * @param message
     * @param toExtractBody
     * @return
     * @throws Exception
     */
    public String extractXmlPartsFromSoapMessage(String message, boolean toExtractBody) throws Exception {
        SOAPMessage soapMessage = MessageFactory.newInstance().createMessage(
                null, new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8))
        );
        NodeList nodes;
        if (toExtractBody) {
            nodes = soapMessage.getSOAPBody().getChildNodes();
        } else {
            nodes = soapMessage.getSOAPHeader().getChildNodes();
        }
        if (nodes.getLength() > 0) {
            StringBuilder extractedContent = new StringBuilder();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node child = nodes.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    extractedContent.append(nodeToString(child)).append(NEW_LINE);
                }
            }
            return extractedContent.toString();
        }
        throw new CustomException(null, ErrorCodes.E0003.name(), ErrorCodes.E0003.getDescription(),
                new Exception(ErrorCodes.E0003.getDescription()));
    }

    private static String nodeToString(Node node) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        DOMSource source = new DOMSource(node);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
        return writer.toString();
    }

}
