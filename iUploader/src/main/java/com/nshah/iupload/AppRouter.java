package com.nshah.iupload;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nshah.iupload.api.ImageApi;

public class AppRouter extends AbstractVerticle {

    private static Logger LOGGER = LoggerFactory.getLogger(AppRouter.class);

    public AppRouter() {
    }

    @Override
    public void start() throws Exception {
	LOGGER.debug("Image upload service Init");
	Router router = Router.router(vertx);
	// We need cookies, sessions and request bodies
	router.route().handler(CookieHandler.create());
	router.route().handler(BodyHandler.create());

	addImageApis(router);
	router.route().handler(StaticHandler.create());
	vertx.createHttpServer().requestHandler(router::accept).listen(8080);
	LOGGER.debug("AppRouter service init Completed");
    }

    private void addImageApis(Router router) {
	router.route(HttpMethod.GET, "/images").handler(new Handler<RoutingContext>() {
	    @Override
	    public void handle(RoutingContext rc) {
		long startTime = System.currentTimeMillis();
		setResponseHeaders(rc);
		JsonObject json = new JsonObject();
		String token = rc.request().getParam("token");
		json.put("token", token);
		LOGGER.info("Token is " + token);
		vertx.eventBus().send(ImageApi.GET_IMAGES, json, result -> {
		    if (result.succeeded()) {
			LOGGER.info("Image keys fetched in {}", (System.currentTimeMillis() - startTime));
			rc.response().setStatusCode(HttpResponseStatus.OK.code()).end((String) result.result().body());
		    } else {
			rc.response().setStatusCode(((ReplyException) result.cause()).failureCode()).end(result.cause().getMessage());
		    }
		});
	    }
	});

	router.route(HttpMethod.POST, "/images").handler(new Handler<RoutingContext>() {
	    @Override
	    public void handle(RoutingContext rc) {
		long startTime = System.currentTimeMillis();
		setResponseHeaders(rc);
		Set<FileUpload> files = rc.fileUploads();
		if (files != null && !files.isEmpty()) {
		    JsonObject json = new JsonObject();
		    FileUpload fUp = files.iterator().next();
		    json.put("uploadedF", fUp.uploadedFileName());
		    json.put("fileName", fUp.fileName());
		    json.put("start", startTime);
		    vertx.eventBus().<String> send(ImageApi.ADD_IMAGE, json, result -> {
			if (result.succeeded()) {
			    LOGGER.info("Image upload completed in {}", (System.currentTimeMillis() - startTime));
			    rc.response().setStatusCode(HttpResponseStatus.CREATED.code()).end(result.result().body());
			} else {
			    rc.response().setStatusCode(((ReplyException) result.cause()).failureCode()).end(result.cause().getMessage());
			}
		    });
		}

	    }
	});

    }

    private void setResponseHeaders(RoutingContext rc) {
	rc.response().putHeader("content-type", "application/json; charset=utf-8");
    }
}
