package sparc.team3.validator.validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeoutException;

public class EC2ValidateInstance {
    private final Instance instance;
    private final Ec2Client ec2Client;
    private final Logger logger;

    public EC2ValidateInstance(Ec2Client ec2Client, Instance instance) {
        this.ec2Client = ec2Client;
        this.instance = instance;
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
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

        return httpResponse.statusCode() == 200;

    }

    /**
     * Busy wait for EC2 to complete setup.
     *
     * @throws InterruptedException when sleep is interrupted
     * @throws TimeoutException when waiting for the EC2 instance times out
     */
    private void waitForEC2Checks() throws InterruptedException, TimeoutException {

        //Wait for ec2 instance to complete setup
        //@TODO should we change to waiter?
        int attempts = 0;
        while (attempts < 11) {

            try {
                DescribeInstanceStatusRequest statusReq = DescribeInstanceStatusRequest
                        .builder().instanceIds(instance.instanceId()).build();

                DescribeInstanceStatusResponse statusRes = ec2Client.describeInstanceStatus(statusReq);

                String running = statusRes.instanceStatuses().get(0).instanceState().name().toString();
                String sysPass = statusRes.instanceStatuses().get(0).systemStatus().status().toString();
                String reachPass = statusRes.instanceStatuses().get(0).instanceStatus().status().toString();

                logger.info("EC2 instance {}:\t\tRunning:{}\t\tSys Pass:{}\t\tReach Pass:{}", instance.instanceId(), running, sysPass, reachPass);

                if ((running.equals("running")) && (sysPass.equals("passed") || sysPass.equals("ok")) && (reachPass.equals("passed") || reachPass.equals("ok"))) {
                    break;
                }

            } catch (AwsServiceException e) {
                logger.error("Problem getting status of EC2 instance {}", instance.instanceId(), e);
                return;
            }

            Thread.sleep(60000);
            attempts++;
        }

        if (attempts >= 11) {
            throw new TimeoutException("EC2 Instance Timeout");
        }
    }

    /**
     * Terminate ec2Instance attached to client.
     */
    public void terminateEC2Instance() {
        logger.info("Terminating Instance {}", instance.instanceId());

        TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder().instanceIds(instance.instanceId()).build();
        ec2Client.terminateInstances(terminateRequest);

    }
}
