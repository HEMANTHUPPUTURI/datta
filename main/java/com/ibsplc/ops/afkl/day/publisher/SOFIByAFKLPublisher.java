package com.ibsplc.ops.afkl.day.publisher;

import com.ibsplc.si.framework.messagehandler.SqsMessageOperationService;
import com.ibsplc.si.framework.util.LoggerUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Slf4j
@ApplicationScoped
@SuppressWarnings("unused")
public class SOFIByAFKLPublisher {

    @Inject
    private SqsMessageOperationService sqsMessageOperationService;

    /**
     * this method id used for publishing the message to output SQS
     */
    public void publish(String message, String sqsUrl, String messageType) {
        log.info(" Publishing {} message... ", messageType);
        try {
            SendMessageResponse response = sqsMessageOperationService.sendMessage(sqsUrl, message);
            if (ObjectUtils.isNotEmpty(response) && response.sdkHttpResponse().isSuccessful()) {
                log.info(" Published {} message to SQS: {}", messageType, LoggerUtility.sanitizeMessage(message));
            } else {
                log.error(" Failed to publish the {} output message to SQS", messageType);
            }
        } catch (Exception exception) {
            log.error(" An error occurred while publishing the {} output message to SQS", messageType, exception);
        }
    }
}
