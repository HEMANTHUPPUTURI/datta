package com.ibsplc.ops.afkl.day.util;

import lombok.extern.slf4j.Slf4j;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.StringReader;

/**
 * validation util used to validate the xml message
 */
@Slf4j
public final class XmlValidationUtil {

    private XmlValidationUtil() {
        super();
    }

    /**
     * Validate xml against xsd.
     *
     * @param xml the xml
     * @param xsd the xsd
     */
    public static boolean validateXMLAgainstXSD(String xml, String xsd) {

        boolean isValidXml = Boolean.FALSE;
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            schemaFactory.newSchema(XmlValidationUtil.class.getClassLoader().getResource(xsd))
                    .newValidator().validate(new StreamSource(new StringReader(xml)));
            isValidXml = Boolean.TRUE;
        } catch (SAXException | IOException exception) {
            log.error(" An exception occurred during XSD validation", exception);
        }
        return isValidXml;
    }
}
