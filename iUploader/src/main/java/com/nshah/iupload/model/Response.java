package com.nshah.iupload.model;

import java.util.List;

public class Response {

    private List<String> imageKeys;
    private String token;

    public Response() {
    }

    public Response(List<String> imageKeys, String token) {
	super();
	this.imageKeys = imageKeys;
	this.token = token;
    }

    public String getToken() {
	return token;
    }

    public void setToken(String token) {
	this.token = token;
    }

    public List<String> getImageKeys() {
	return imageKeys;
    }

    public void setImageKeys(List<String> imageKeys) {
	this.imageKeys = imageKeys;
    }

    @Override
    public String toString() {
	return "Response [imageKeys=" + imageKeys + ", token=" + token + "]";
    }

}
