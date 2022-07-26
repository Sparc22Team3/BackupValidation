/**
 * This class is created to help facilitate the process of retrieving objects stored in a given
 * S3 bucket.
 */

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import java.util.ArrayList;
import java.util.List;

import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;


public class S3Bucket {

    S3Client s3Client;
    String bucketName;
    List<S3Object> objects;

    public S3Bucket(S3Client s3Client, String bucketName) {

        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.objects = getBucketObjects(s3Client, bucketName);
    }

    public List<S3Object> getBucketObjects(S3Client s3Client, String bucketName) {

        List<S3Object> objects = new ArrayList<S3Object>();

        try {
            ListObjectsRequest listObjects = ListObjectsRequest
                    .builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsResponse res = s3Client.listObjects(listObjects);
            return res.contents();

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

        return objects;
    }

}
