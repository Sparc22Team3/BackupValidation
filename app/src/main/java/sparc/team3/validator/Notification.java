package sparc.team3.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

public class Notification {

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
