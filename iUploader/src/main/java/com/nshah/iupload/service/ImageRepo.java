package com.nshah.iupload.service;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.nshah.iupload.model.Response;

public class ImageRepo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageRepo.class);

    BasicAWSCredentials awsCreds = new BasicAWSCredentials("XXXXXXXXXXXX", "XXXXXXXXXXXXX");
    AmazonS3 s3Client;
    Random rand = new Random();
    private static final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
	@Override
	protected DateFormat initialValue() {
	    return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	}
    };
    private static ImageRepo REPO = new ImageRepo();

    private ImageRepo() {
	s3Client = AmazonS3Client.builder().withRegion(Regions.AP_SOUTH_1).withCredentials(new AWSStaticCredentialsProvider(awsCreds))
		.build();
	System.out.println(s3Client.getRegionName());
    }

    public static ImageRepo getInstance() {
	return REPO;
    }

    public void add(String bucketName, InputStream value, String fileName, ObjectMetadata meta) {
	long startTime = System.currentTimeMillis();
	s3Client.putObject(new PutObjectRequest(bucketName, Integer.toHexString(rand.nextInt(65535)) + "-"
		+ dateFormat.get().format(new Date()) + "/" + fileName, value, meta).withCannedAcl(CannedAccessControlList.PublicRead));
	LOGGER.info("File upload to s3 completed in {}", System.currentTimeMillis() - startTime);
    }

    public Response getAll(String bucketName, String token) {
	long startTime = System.currentTimeMillis();
	List<String> list = new ArrayList<String>();
	Response res = new Response();
	try {
	    LOGGER.debug("Listing objects");
	    final ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withMaxKeys(9);
	    ListObjectsV2Result result;
	    if (token != null) {
		req.setContinuationToken(token);
	    }
	    result = s3Client.listObjectsV2(req);
	    for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
		list.add(objectSummary.getKey());
		LOGGER.debug(" - " + objectSummary.getKey() + "  " + "(size = " + objectSummary.getSize() + ")");
	    }
	    LOGGER.debug("Next Continuation Token : " + result.getNextContinuationToken());
	    res.setImageKeys(list);
	    if (result.getNextContinuationToken() != null) {
		try {
		    res.setToken(URLEncoder.encode(result.getNextContinuationToken(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
		    LOGGER.error("Faile to encode token {}", result.getNextContinuationToken());
		}
		LOGGER.info("TOKEN: " + result.getNextContinuationToken());
	    }

	} catch (AmazonServiceException ase) {
	    LOGGER.error(
		    "Caught an AmazonServiceException, which means your request made it to Amazon S3, but was rejected with an error response for some reason.",
		    ase.getMessage());
	} catch (AmazonClientException ace) {
	    LOGGER.error("Caught an AmazonClientException, which means the client encountered an internal error while trying to communicate with S3, such as not being able to access the network.");
	    LOGGER.error("Error Message: {}", ace.getMessage());
	}
	LOGGER.info("Fetching image keys from s3 completed in {}", System.currentTimeMillis() - startTime);
	return res;

    }

}