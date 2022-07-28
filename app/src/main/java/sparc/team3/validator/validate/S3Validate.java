package sparc.team3.validator.validate;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.HashMap;
import java.util.List;


/**
 * Class to validate original and restored S3 buckets
 */
public class S3Validate {

    private final S3Client s3;
    private final String originalBucket;
    private final String restoredBucket;

    public S3Validate(S3Client s3, String originalBucket, String restoredBucket){

        this.s3 = s3;
        this.originalBucket = originalBucket;
        this.restoredBucket = restoredBucket;

    }

    /**
     * Before validating objects, all objects need to be copied to the same bucket with the "SHA-256" checksum
     * algorithm turned on.
     * This function is called within the ChecksumValidate function
     */
    private void CopyS3Objects() {

        // initialize ListObjectsRequest
        ListObjectsRequest listObjects = ListObjectsRequest
                .builder()
                .bucket(restoredBucket)
                .build();

        ListObjectsResponse res = s3.listObjects(listObjects);
        List<S3Object> objects = res.contents();

        System.out.println("Preparing S3 buckets for validation...");

        // iterate through each object in the bucket and perform the copy action
        for (S3Object myValue : objects) {

            // initialize CopyObjectsRequest
            CopyObjectRequest copyObjects = CopyObjectRequest
                    .builder()
                    .sourceBucket(restoredBucket)
                    .sourceKey(myValue.key())
                    .destinationBucket(restoredBucket)
                    .destinationKey(myValue.key())
                    .checksumAlgorithm("SHA256") // turn on checksum for SHA256
                    .storageClass(StorageClass.STANDARD_IA) // added in to ensure copy action passes through
                    .build();

            // copy objects
            s3.copyObject(copyObjects);
        }

    }

    /**
     * Extract S3 objects from a given S3 bucket, and returning their key and checksum values in a HashMap
     * @param bucketName the string name of the bucket to get the objects of
     * @return a HashMap of S3 objects
     */
    private HashMap<String, String> GetS3Objects(String bucketName){

        // initialize return map
        HashMap<String, String> s3Objects = new HashMap<>();

        // initialize ListObjectsRequest
        ListObjectsRequest listObjects = ListObjectsRequest
                .builder()
                .bucket(bucketName)
                .build();

        ListObjectsResponse res = s3.listObjects(listObjects);
        List<S3Object> objects = res.contents();

        // retrieve the keys of the S3 objects and add them to s3Objects
        for (S3Object myValue : objects) {

            // initialize AWS GetObjectAttributesResponse object to get checksum value
            GetObjectAttributesResponse
                    objectAttributes = s3.getObjectAttributes(GetObjectAttributesRequest.builder().bucket(bucketName).key(myValue.key())
                    .objectAttributes(ObjectAttributes.OBJECT_PARTS, ObjectAttributes.CHECKSUM).build());

            // add S3 object key and checksum value to map
            s3Objects.put(myValue.key(), objectAttributes.checksum().checksumSHA256());
        }

        return s3Objects;
    }

    /**
     * Cross-check the checksum values of each S3 object in the original S3 bucket and the restored S3 bucket.
     * If all values match, return true; otherwise, false
     * @return a boolean value of whether the checksums match
     */
    public boolean ChecksumValidate() {

        // copy objects to get SHA-256 checksum values
        CopyS3Objects();

        HashMap<String, String> originalObjs = GetS3Objects(originalBucket);
        HashMap<String, String> restoredObjs = GetS3Objects(restoredBucket);

        for (String key : originalObjs.keySet()){
            if (restoredObjs.containsKey(key)){
                // compare checksum value of the obj
                if (!originalObjs.get(key).equals(restoredObjs.get(key))){
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }
}