package util;

import DM.DialogManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

public class OkHttpClientUtil {
    private static final OkHttpClient CLIENTINSTANCE = new OkHttpClient.Builder().connectTimeout(DialogManager.timeout, TimeUnit.MILLISECONDS).writeTimeout(DialogManager.timeout, TimeUnit.MILLISECONDS).readTimeout(DialogManager.timeout, TimeUnit.MILLISECONDS).build();

    private final static MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static OkHttpClient getClientInstance(){
        return CLIENTINSTANCE;
    }

    public static MediaType getJSON(){
        return JSON;
    }
}
