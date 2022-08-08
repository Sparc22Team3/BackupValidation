package sparc.team3.validator.validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import sparc.team3.validator.util.InstanceSettings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public class EC2ValidateInstance implements Callable<Boolean> {
    private final Ec2Client ec2Client;
    private final InstanceSettings instanceSettings;
    private final Logger logger;
    private Instance instance;


    public EC2ValidateInstance(Ec2Client ec2Client, InstanceSettings instanceSettings) {
        this.ec2Client = ec2Client;
        this.instanceSettings = instanceSettings;
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
    }

    public void setEC2Instance(Instance instance){
        this.instance = instance;
    }

    @Override
    public Boolean call() throws IOException, InterruptedException {
        return validateWithPing("");
    }

    /**
     * Ping url and check status code.
     *
     * @return boolean whether the instance is pingable
     * @throws IOException if an I/O error occurs when sending or receiving
     * @throws InterruptedException – if the operation is interrupted
     */
    public Boolean validateWithPing(String entryPoint) throws IOException, InterruptedException {
        String url = instance.publicDnsName();


        try {
            waitForEC2Checks();
        } catch (InterruptedException | TimeoutException e) {
            logger.error("Waiting for the EC2 instance to be ready has timed out or otherwise been interrupted", e);
            return false;
        }

        url = "http://" + url + entryPoint;
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        logger.info("EC2 Instance responded with {}", httpResponse.statusCode());

        return httpResponse.statusCode() == 200;

    }
}
