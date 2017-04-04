package com.nshah.iupload.api;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.nshah.iupload.exception.UnsupportedFileFormatException;
import com.nshah.iupload.model.Response;
import com.nshah.iupload.service.ImageRepo;

public class ImageApi extends AbstractVerticle {

    private static Logger LOGGER = LoggerFactory.getLogger(ImageApi.class);
    public static final String GET_IMAGES = "uploader.get.images";
    public static final String ADD_IMAGE = "uploader.add.images";
    private static final String POST_BUCKET_NAME = "nshah-images";
    private static final String GET_BUCKET_NAME = "nshah-rimages";

    private final ImageRepo service;

    enum ImageType {
	jpg("image/jpeg"), png("image/png"), jpeg("image/jpeg"), bmp("image/bmp"), gif("image/gif");

	private String mimeType;

	private ImageType(String mimeType) {
	    this.mimeType = mimeType;
	}

	public String getMimeType() {
	    return mimeType;
	}
    }

    public ImageApi() {
	service = ImageRepo.getInstance();
    }

    private Handler<Message<JsonObject>> addImageHandler() {
	return msg -> {
	    LOGGER.info("Upload new Image");
	    try {
		JsonObject json = msg.body();
		String uploadedF = json.getString("uploadedF");
		String fileName = json.getString("fileName");
		Buffer buff = vertx.fileSystem().readFileBlocking(uploadedF);
		InputStream is = new ByteArrayInputStream(buff.getBytes());
		String file = URLDecoder.decode(fileName, "UTF-8");
		ImageType type = checkFileFormat(file);
		LOGGER.info("Vertx file read in {}", System.currentTimeMillis() - json.getLong("start"));
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(buff.length());
		meta.setContentType(type.getMimeType());
		service.add(POST_BUCKET_NAME, is, file, meta);
		msg.reply(Json.encodePrettily("Image Uploaded Successfully"));
		json.put("start", System.currentTimeMillis());
	    } catch (UnsupportedFileFormatException e) {
		msg.fail(HttpResponseStatus.BAD_REQUEST.code(), Json.encodePrettily(e.getMessage()));
	    } catch (Exception e) {
		String err = "Failed to add image";
		LOGGER.error(err, e);
		msg.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), Json.encodePrettily(err));
	    }
	};
    }

    private ImageType checkFileFormat(String file) throws UnsupportedFileFormatException {
	Matcher matcher = Pattern.compile(".*\\.([^\\.]*)").matcher(file);
	if (!matcher.matches()) {
	    System.out.println("Failed to infer image type for file " + file);
	    throw new UnsupportedFileFormatException("Failed to infer image type for file" + file);
	}
	String imageType = matcher.group(1);
	ImageType type;
	try {
	    type = ImageType.valueOf(imageType.toLowerCase());
	} catch (Exception e) {
	    String err = "Failed because only " + Arrays.toString(ImageType.values()) + "formats are supported";
	    LOGGER.error(err);
	    throw new UnsupportedFileFormatException(err);
	}
	return type;
    }

    private Handler<Message<JsonObject>> getImagesHandler() {
	return msg -> {
	    LOGGER.info("Get list of Image keys");
	    try {
		JsonObject json = msg.body();
		String token = json.getString("token");
		Response result = service.getAll(GET_BUCKET_NAME, token);
		msg.reply(Json.encodePrettily(result));
	    } catch (Exception e) {
		String err = "Failed to get Image keys";
		LOGGER.error(err, e);
		msg.fail(HttpResponseStatus.NOT_FOUND.code(), Json.encodePrettily(err));
	    }
	};
    }

    @Override
    public void start() throws Exception {
	super.start();
	LOGGER.info("ImageAPI vertical is deployed");
	EventBus eb = vertx.eventBus();
	eb.<JsonObject> consumer(ADD_IMAGE).handler(addImageHandler());
	eb.<JsonObject> consumer(GET_IMAGES).handler(getImagesHandler());
	LOGGER.debug("ImageApi service init Completed");
    }

}
