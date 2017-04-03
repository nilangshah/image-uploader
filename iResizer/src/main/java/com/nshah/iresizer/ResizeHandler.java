package com.nshah.iresizer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class ResizeHandler implements RequestHandler<S3Event, Object> {

    private static final int MAX_WIDTH = 640;
    private static final int MAX_HEIGHT = 480;
    private static final String SRC_BUCKET_NAME = "nshah-images";
    private static final String DEST_BUCKET_NAME = "nshah-rimages";
    BasicAWSCredentials awsCreds = new BasicAWSCredentials("XXXXXXXXXXXX", "XXXXXXXXXXXXX");
    
    /**
     * Resizes input image to 640x480 and upload it to S3 bucket
     */
    public String handleRequest(S3Event s3event, Context context) {
	long startTime = System.currentTimeMillis();
	for (S3EventNotificationRecord record : s3event.getRecords()) {
	    String srcKey = record.getS3().getObject().getKey();
	    String name;
	    try {
		name = URLDecoder.decode(srcKey, "UTF-8");
	    } catch (UnsupportedEncodingException e) {
		context.getLogger().log("Failed to decode file name" + srcKey);
		return "FAILED";
	    }
	    context.getLogger().log("Received image key: " + name);
	    AmazonS3 s3Client = AmazonS3Client.builder().withRegion(Regions.AP_SOUTH_1)
		    .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
	    s3Client.setRegion(Region.getRegion(Regions.AP_SOUTH_1));
	    S3Object s3Object = s3Client.getObject(new GetObjectRequest(SRC_BUCKET_NAME, name));
	    // Infer the image type.
	    Matcher matcher = Pattern.compile(".*\\.([^\\.]*)").matcher(name);
	    if (!matcher.matches()) {
		context.getLogger().log("Failed to infer image type for file " + name);
		return "FAILED";
	    }
	    String imageType = matcher.group(1);
	    ObjectMetadata metadata = s3Object.getObjectMetadata();
	    InputStream objectData = s3Object.getObjectContent();
	    byte[] resizedImage = resizeImage(objectData, context, imageType);
	    if (resizedImage != null) {
		InputStream resizedIs = new ByteArrayInputStream(resizedImage);
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(resizedImage.length);
		meta.setContentType(metadata.getContentType());
		s3Client.putObject(new PutObjectRequest(DEST_BUCKET_NAME, name, resizedIs, meta)
			.withCannedAcl(CannedAccessControlList.PublicRead));
		context.getLogger().log("Successfully resized image and uploaded to bucket in " + (System.currentTimeMillis() - startTime));
	    }
	}
	return "OK";
    }

    /**
     * Resize image using ImgScalr library
     * 
     * @param is
     * @param context
     * @param type
     * @return
     */
    private byte[] resizeImage(InputStream is, Context context, String type) {
	long startTime = System.currentTimeMillis();
	try {
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    BufferedImage bImageFromConvert = ImageIO.read(is);
	    context.getLogger().log("Reading image compelted in " + (System.currentTimeMillis() - startTime));
	    if (bImageFromConvert.getWidth() > 640 || bImageFromConvert.getHeight() > 480) {
		BufferedImage scaledImage = Scalr.resize(bImageFromConvert, Method.QUALITY, Mode.AUTOMATIC, MAX_WIDTH, MAX_HEIGHT);
		context.getLogger().log("Scaling image SUCCESS in " + (System.currentTimeMillis() - startTime));
		ImageIO.write(scaledImage, type.toLowerCase(), os);
		context.getLogger().log("Image resize SUCCESS in " + (System.currentTimeMillis() - startTime));

	    } else {
		ImageIO.write(bImageFromConvert, type.toLowerCase(), os);
		context.getLogger().log("Image resize SKIPED in " + (System.currentTimeMillis() - startTime));
	    }
	    return os.toByteArray();
	} catch (Exception e) {
	    context.getLogger().log("Error occurred while resizing image, will uplaod the original image." + e);
	    context.getLogger().log("Image resize FAILED in " + (System.currentTimeMillis() - startTime));
	}
	return null;
    }

}
