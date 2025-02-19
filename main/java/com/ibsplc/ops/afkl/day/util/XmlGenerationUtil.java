package com.ibsplc.ops.afkl.day.util;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

public final class XmlGenerationUtil {

    private XmlGenerationUtil() {
        super();
    }

    /**
     * This method is used to generate the xml from the object received
     *
     * @param targetObj   object to convert into xml
     * @param targetClass targetObj type
     * @param <T>
     * @return
     * @throws JAXBException
     */
    public static <T> String generateXmlFromObj(Object targetObj, Class<T> targetClass) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(targetClass);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(targetObj, writer);
        return writer.toString();
    }

}
