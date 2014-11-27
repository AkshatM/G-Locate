package com.example.jingyuliu.glocate;
import com.loopj.android.http.*;

/**
 * Created by JingyuLiu on 11/25/2014.
 */
public class MyFuckingClient {
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
