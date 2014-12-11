package com.example.jingyuliu.glocate;
import com.loopj.android.http.*;

/**
 * Created by JingyuLiu on 11/25/2014.
 * Asynchronous HTTP client for Android that invokes LoopJ technology. Responsible for posting, getting from/to server
 * Functions are predefined by HTTP client. More information here: http://loopj.com/android-async-http/
 */
public class MyClient {
    private static final String BASE_URL = "http://jingyuliu.com/glocate/";

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
