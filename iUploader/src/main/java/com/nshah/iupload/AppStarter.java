package com.nshah.iupload;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

import com.nshah.iupload.api.ImageApi;

public class AppStarter {

    public static void main(String[] args) {
	final Vertx vertx = Vertx.vertx();
	vertx.deployVerticle(new AppRouter());
	vertx.deployVerticle(new ImageApi(), new DeploymentOptions().setMultiThreaded(true).setWorker(true));
    }
}
