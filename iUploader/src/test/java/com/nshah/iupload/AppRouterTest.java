package com.nshah.iupload;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.http.client.ClientProtocolException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nshah.iupload.api.ImageApi;
import com.nshah.iupload.model.Response;

@RunWith(VertxUnitRunner.class)
public class AppRouterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppRouterTest.class);
    Vertx vertx;
    HttpServer server;
    Response result;

    @Before
    public void before(TestContext context) {
	vertx = Vertx.vertx();
	vertx.deployVerticle(new AppRouter(), context.asyncAssertSuccess());
	result = new Response();
	result.setImageKeys(Arrays.asList("test.png", "demo.JPG"));
	result.setToken("test-token");
	// vertx.deployVerticle(new ImageApi(), new
	// DeploymentOptions().setMultiThreaded(true).setWorker(true),
	// context.asyncAssertSuccess());
    }

    @After
    public void after(TestContext context) {
	vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void getImagesSuccess(TestContext context) throws ClientProtocolException, IOException {
	// Send a request and get a response
	Async async = context.async();
	io.vertx.core.http.HttpClient httpclient = vertx.createHttpClient();
	EventBus eb = vertx.eventBus();
	eb.<JsonObject> consumer(ImageApi.GET_IMAGES).handler(getImagesHandler());

	final HttpClientRequest req = httpclient.get(5000, "localhost", "/images", new Handler<HttpClientResponse>() {
	    public void handle(HttpClientResponse response) {
		response.bodyHandler(res -> {
		    LOGGER.info(res.toString());
		    context.assertEquals(Json.encodePrettily(result), res.toString());
		});
		async.complete();
	    }
	});
	req.end();
	async.awaitSuccess();

    }

    @Test
    public void getImagesFail(TestContext context) throws ClientProtocolException, IOException {
	// Send a request and get a response
	Async async = context.async();
	io.vertx.core.http.HttpClient httpclient = vertx.createHttpClient();
	EventBus eb = vertx.eventBus();
	// eb.<JsonObject> consumer(ADD_IMAGE).handler(addImageHandler());
	eb.<JsonObject> consumer(ImageApi.GET_IMAGES).handler(getFailImagesHandler());

	final HttpClientRequest req = httpclient.get(5000, "localhost", "/images", new Handler<HttpClientResponse>() {
	    public void handle(HttpClientResponse response) {
		response.bodyHandler(res -> {
		    LOGGER.info(res.toString());
		});
		context.assertEquals(500, response.statusCode());
		async.complete();
	    }
	});
	req.end();
	async.awaitSuccess();
    }

    @Test
    public void postSuccess(TestContext context) throws ClientProtocolException, IOException {
	// Send a request and get a response
	Async async = context.async();
	io.vertx.core.http.HttpClient httpclient = vertx.createHttpClient();
	EventBus eb = vertx.eventBus();
	eb.<JsonObject> consumer(ImageApi.ADD_IMAGE).handler(addImagesHandler());
	final HttpClientRequest req = httpclient.post(5000, "localhost", "/images", new Handler<HttpClientResponse>() {
	    public void handle(HttpClientResponse response) {
		response.bodyHandler(res -> {
		    LOGGER.info(res.toString());
		});
		context.assertEquals(201, response.statusCode());
		async.complete();
	    }
	}).setChunked(false);

	Buffer bodyBuffer = getBody("src/test/resources/test.png");
	req.putHeader("Content-Type", "multipart/form-data; boundary=MyBoundary");
	req.putHeader("accept", "application/json");
	req.end(bodyBuffer);
	async.awaitSuccess();
    }

    private Buffer getBody(String filename) {
	Buffer buffer = Buffer.buffer();
	buffer.appendString("--MyBoundary\r\n");
	buffer.appendString("Content-Disposition: form-data; name=\"image\"; filename=\"blob.jpg\"\r\n");
	buffer.appendString("Content-Type: application/octet-stream\r\n");
	buffer.appendString("Content-Transfer-Encoding: binary\r\n");
	buffer.appendString("\r\n");
	try {
	    buffer.appendBytes(Files.readAllBytes(Paths.get(filename)));
	    buffer.appendString("\r\n");
	} catch (IOException e) {
	    e.printStackTrace();

	}
	buffer.appendString("--MyBoundary--\r\n");
	return buffer;
    }

    @Test
    public void postBadRequest(TestContext context) throws ClientProtocolException, IOException {
	// Send a request and get a response
	Async async = context.async();
	io.vertx.core.http.HttpClient httpclient = vertx.createHttpClient();
	EventBus eb = vertx.eventBus();
	eb.<JsonObject> consumer(ImageApi.ADD_IMAGE).handler(addFailImagesHandler());
	final HttpClientRequest req = httpclient.post(5000, "localhost", "/images", new Handler<HttpClientResponse>() {
	    public void handle(HttpClientResponse response) {
		response.bodyHandler(res -> {
		    LOGGER.info(res.toString());
		});
		context.assertEquals(400, response.statusCode());
		async.complete();
	    }
	}).setChunked(false);

	Buffer bodyBuffer = getTxtBody("src/test/resources/abc.txt");
	req.putHeader("Content-Type", "multipart/form-data; boundary=MyBoundary");
	req.putHeader("accept", "application/json");
	req.end(bodyBuffer);
	async.awaitSuccess();
    }

    private Buffer getTxtBody(String filename) {
	Buffer buffer = Buffer.buffer();
	buffer.appendString("--MyBoundary\r\n");
	buffer.appendString("Content-Disposition: form-data; name=\"text\"; filename=\"abc.txt\"\r\n");
	buffer.appendString("Content-Type: application/octet-stream\r\n");
	buffer.appendString("Content-Transfer-Encoding: binary\r\n");
	buffer.appendString("\r\n");
	try {
	    buffer.appendBytes(Files.readAllBytes(Paths.get(filename)));
	    buffer.appendString("\r\n");
	} catch (IOException e) {
	    e.printStackTrace();

	}
	buffer.appendString("--MyBoundary--\r\n");
	return buffer;
    }

    
    private Handler<Message<JsonObject>> getImagesHandler() {
	return msg -> {
	    msg.reply(Json.encodePrettily(result));
	};
    }

    private Handler<Message<JsonObject>> getFailImagesHandler() {
	return msg -> {
	    msg.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), Json.encodePrettily("Failed to fetch images"));
	};
    }

    private Handler<Message<JsonObject>> addImagesHandler() {
	return msg -> {
	    msg.reply(Json.encodePrettily("Image Uploaded Successfully"));
	};
    }
    
    private Handler<Message<JsonObject>> addFailImagesHandler() {
	return msg -> {
	    msg.fail(HttpResponseStatus.BAD_REQUEST.code(), Json.encodePrettily("Txt format not supported"));
	};
    }
}
