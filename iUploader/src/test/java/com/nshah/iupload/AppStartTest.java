package com.nshah.iupload;

import org.junit.Assert;
import org.junit.Test;

public class AppStartTest {

    @Test
    public void Test() {
	try {
	    AppStarter.main(null);
	} catch (Exception e) {
	    Assert.fail("Failed to start http server on 5000 port");
	}
    }
}
