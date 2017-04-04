package com.nshah.iresizer;

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createPartialMock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*" })
@PrepareForTest({ S3Event.class, ResizeHandler.class, S3EventNotificationRecord.class, S3Object.class })
public class ResizerHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResizerHandlerTest.class);

    public S3ObjectInputStream getFile(String fileName) {
	File file = new File("src/test/resources/" + fileName);
	FileInputStream stream;
	try {
	    stream = new FileInputStream(file);
	    return new S3ObjectInputStream(stream, null);
	} catch (FileNotFoundException e) {
	    LOGGER.error("Fail to read file from src/test/resource");
	}
	return null;
    }

    @Test
    public void SuccessTest() {
	S3Event event = createMock(S3Event.class);
	ResizeHandler handler = createPartialMock(ResizeHandler.class, "getS3Client", "getObjectKey");
	AmazonS3Client s3 = createMock(AmazonS3Client.class);
	expect(handler.getS3Client()).andReturn(s3);
	List<S3EventNotificationRecord> records = new ArrayList<S3EventNotificationRecord>();
	S3EventNotificationRecord record = createMock(S3EventNotificationRecord.class);
	S3Object object = PowerMock.createMock(S3Object.class);

	records.add(record);
	expect(event.getRecords()).andReturn(records);
	expect(handler.getObjectKey(record)).andReturn("test.png");
	expect(s3.getObject(EasyMock.anyObject(GetObjectRequest.class))).andReturn(object);

	ObjectMetadata meta = new ObjectMetadata();
	meta.setContentType("image/png");
	expect(object.getObjectMetadata()).andReturn(meta);
	expect(object.getObjectContent()).andReturn(getFile("test.png"));
	expect(s3.putObject(EasyMock.anyObject(PutObjectRequest.class))).andReturn(null);

	PowerMock.replayAll();

	String response = handler.handleRequest(event, new TestContext());
	Assert.assertEquals(response, "OK");
	PowerMock.verifyAll();
    }

    @Test
    public void decodeFailTest() throws UnsupportedEncodingException {
	S3Event event = createMock(S3Event.class);
	ResizeHandler handler = createPartialMock(ResizeHandler.class, "getS3Client", "getObjectKey");
	List<S3EventNotificationRecord> records = new ArrayList<S3EventNotificationRecord>();
	S3EventNotificationRecord record = createMock(S3EventNotificationRecord.class);

	records.add(record);
	expect(event.getRecords()).andReturn(records);
	expect(handler.getObjectKey(record)).andReturn("<ãSâ==!@#$%^&*()");

	PowerMock.replayAll();

	String response = handler.handleRequest(event, new TestContext());
	Assert.assertEquals(response, "FAILED");
	PowerMock.verifyAll();
    }

    @Test
    public void resizedFileTest() {
	S3Event event = createMock(S3Event.class);
	ResizeHandler handler = createPartialMock(ResizeHandler.class, "getS3Client", "getObjectKey");
	AmazonS3Client s3 = createMock(AmazonS3Client.class);
	expect(handler.getS3Client()).andReturn(s3);
	List<S3EventNotificationRecord> records = new ArrayList<S3EventNotificationRecord>();
	S3EventNotificationRecord record = createMock(S3EventNotificationRecord.class);
	S3Object object = PowerMock.createMock(S3Object.class);

	records.add(record);
	expect(event.getRecords()).andReturn(records);
	expect(handler.getObjectKey(record)).andReturn("SPACE.JPG");
	expect(s3.getObject(EasyMock.anyObject(GetObjectRequest.class))).andReturn(object);

	ObjectMetadata meta = new ObjectMetadata();
	meta.setContentType("image/jpg");
	expect(object.getObjectMetadata()).andReturn(meta);
	expect(object.getObjectContent()).andReturn(getFile("SPACE.JPG"));
	expect(s3.putObject(EasyMock.anyObject(PutObjectRequest.class))).andReturn(null);

	PowerMock.replayAll();

	String response = handler.handleRequest(event, new TestContext());
	Assert.assertEquals(response, "OK");
	PowerMock.verifyAll();
    }

    @Test
    public void invalidFileName() {
	S3Event event = createMock(S3Event.class);
	ResizeHandler handler = createPartialMock(ResizeHandler.class, "getS3Client", "getObjectKey");
	List<S3EventNotificationRecord> records = new ArrayList<S3EventNotificationRecord>();
	S3EventNotificationRecord record = createMock(S3EventNotificationRecord.class);

	records.add(record);
	expect(event.getRecords()).andReturn(records);
	expect(handler.getObjectKey(record)).andReturn("test123");
	PowerMock.replayAll();

	String response = handler.handleRequest(event, new TestContext());
	Assert.assertEquals(response, "FAILED");
	PowerMock.verifyAll();
    }
}
