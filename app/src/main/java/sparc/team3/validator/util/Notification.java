package sparc.team3.validator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

public class Notification {

    /**
     * Creates a topic with the given name
     * @param topicName String of topic name
     * @param snsClient SnsClient to use
     * @return String of SNS Topic ARN
     */
    public static String createSNSTopic(String topicName, SnsClient snsClient) {
        Logger logger = LoggerFactory.getLogger(javax.management.Notification.class);
        CreateTopicResponse result = null;
        try {
            CreateTopicRequest request = CreateTopicRequest.builder()
                    .name(topicName)
                    .build();

            result = snsClient.createTopic(request);
            return result.topicArn();
        } catch (SnsException e) {

            logger.error(e.awsErrorDetails().errorMessage());
        }
        return "";
    }

    /**
     * Send a message to the given topic ARN via the SnsClient
     * @param message String of message to send
     * @param topicArn String of topic ARN to sent message to.
     * @param snsClient SnsClient to use
     */
    public static void sendSnsMessage(String message, String topicArn, SnsClient snsClient){
        Logger logger = LoggerFactory.getLogger(javax.management.Notification.class);
            try {
                PublishRequest request = PublishRequest.builder()
                        .message(message)
                        .topicArn(topicArn)
                        .build();

                PublishResponse result = snsClient.publish(request);
                logger.info(result.messageId() + " Message sent. Status is " + result.sdkHttpResponse().statusCode());

            } catch (SnsException e) {
                logger.error("SNS Error sending message: {}", e.awsErrorDetails().errorMessage(), e);
            }

    }
}
