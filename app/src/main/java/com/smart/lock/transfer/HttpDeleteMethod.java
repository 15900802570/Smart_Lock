package com.smart.lock.transfer;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

public class HttpDeleteMethod extends HttpEntityEnclosingRequestBase {

    public static final String METHOD_NAME = "DELETE";

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }

    public HttpDeleteMethod(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    public HttpDeleteMethod(final URI uri) {
        super();
        setURI(uri);
    }

    public HttpDeleteMethod() {
        super();
    }

}
