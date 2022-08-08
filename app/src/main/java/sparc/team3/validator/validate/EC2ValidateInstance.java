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
import java.net.InetAddress;
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
        return true;
    }

    /**
     * @deprecated pinging the ec2 server requires the security groups to allow pinging of the server,
     * reachability is technically guaranteed by the waiter in the restore and the web app validation will check this as well.
     *
     * Ping the public ip address and dns name to see if the instance is reachable.
     *
     * @return boolean whether the instance is pingable
     * @throws IOException if an I/O error occurs when sending or receiving
     * @throws InterruptedException – if the operation is interrupted
     */
    public Boolean validateWithPing(String entryPoint) throws IOException, InterruptedException {

        InetAddress urlCheck = InetAddress.getByName(instance.publicDnsName());
        InetAddress ipCheck = InetAddress.getByName(instance.publicIpAddress());

        boolean passUrl = urlCheck.isReachable(10000);
        boolean passIp = ipCheck.isReachable(10000);

        if(passUrl && passIp) {
            logger.info("EC2 instance {} is reachable at {} and {}", instance.instanceId(), instance.publicDnsName(), instance.publicIpAddress());
            return true;
        }

        if(!passUrl)
            logger.warn("Validation Error: EC2 instance {} is not reachable at {}", instance.instanceId(), instance.publicDnsName());
        if(!passIp)
            logger.warn("Validation Error: EC2 instance {} is not reachable at {}", instance.instanceId(), instance.publicIpAddress());

        return false;


    }
}
