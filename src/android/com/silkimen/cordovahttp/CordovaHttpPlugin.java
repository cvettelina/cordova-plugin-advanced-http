package com.silkimen.cordovahttp;

import java.security.KeyStore;

import com.silkimen.http.TLSConfiguration;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.util.Base64;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CordovaHttpPlugin extends CordovaPlugin {
  private static final String TAG = "Cordova-Plugin-HTTP";

  private TLSConfiguration tlsConfiguration;
  private final TrustManager[] noOpTrustManagers;
  private final HostnameVerifier noOpVerifier;
  
  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    this.tlsConfiguration = new TLSConfiguration();

    try {
      KeyStore store = KeyStore.getInstance("AndroidCAStore");
      String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);

      store.load(null);
      tmf.init(store);
      
      this.noOpTrustManagers = new TrustManager[] { new X509TrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }

      public void checkClientTrusted(X509Certificate[] chain, String authType) {
        // intentionally left blank
      }

      public void checkServerTrusted(X509Certificate[] chain, String authType) {
        // intentionally left blank
      }
    } };

    this.noOpVerifier = new HostnameVerifier() {
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    };

      this.tlsConfiguration.setHostnameVerifier(this.noOpVerifier);
      this.tlsConfiguration.setTrustManagers(this.noOpTrustManagers);
    } catch (Exception e) {
      Log.e(TAG, "An error occured while loading system's CA certificates", e);
    }
  }
  
  

  @Override
  public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext)
      throws JSONException {

    if (action == null) {
      return false;
    }

    if ("get".equals(action)) {
      return this.executeHttpRequestWithoutData(action, args, callbackContext);
    } else if ("head".equals(action)) {
      return this.executeHttpRequestWithoutData(action, args, callbackContext);
    } else if ("delete".equals(action)) {
      return this.executeHttpRequestWithoutData(action, args, callbackContext);
    } else if ("post".equals(action)) {
      return this.executeHttpRequestWithData(action, args, callbackContext);
    } else if ("put".equals(action)) {
      return this.executeHttpRequestWithData(action, args, callbackContext);
    } else if ("patch".equals(action)) {
      return this.executeHttpRequestWithData(action, args, callbackContext);
    } else if ("uploadFile".equals(action)) {
      return this.uploadFile(args, callbackContext);
    } else if ("downloadFile".equals(action)) {
      return this.downloadFile(args, callbackContext);
    } else if ("setServerTrustMode".equals(action)) {
      return this.setServerTrustMode(args, callbackContext);
    } else if ("setClientAuthMode".equals(action)) {
      return this.setClientAuthMode(args, callbackContext);
    } else {
      return false;
    }
  }

  private boolean executeHttpRequestWithoutData(final String method, final JSONArray args,
      final CallbackContext callbackContext) throws JSONException {

    String url = args.getString(0);
    JSONObject headers = args.getJSONObject(1);
    int timeout = args.getInt(2) * 1000;
    boolean followRedirect = args.getBoolean(3);
    String responseType = args.getString(4);

    CordovaHttpOperation request = new CordovaHttpOperation(method.toUpperCase(), url, headers, timeout, followRedirect,
        responseType, this.tlsConfiguration, callbackContext);

    cordova.getThreadPool().execute(request);

    return true;
  }

  private boolean executeHttpRequestWithData(final String method, final JSONArray args,
      final CallbackContext callbackContext) throws JSONException {

    String url = args.getString(0);
    Object data = args.get(1);
    String serializer = args.getString(2);
    JSONObject headers = args.getJSONObject(3);
    int timeout = args.getInt(4) * 1000;
    boolean followRedirect = args.getBoolean(5);
    String responseType = args.getString(6);

    CordovaHttpOperation request = new CordovaHttpOperation(method.toUpperCase(), url, serializer, data, headers,
        timeout, followRedirect, responseType, this.tlsConfiguration, callbackContext);

    cordova.getThreadPool().execute(request);

    return true;
  }

  private boolean uploadFile(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String url = args.getString(0);
    JSONObject headers = args.getJSONObject(1);
    String filePath = args.getString(2);
    String uploadName = args.getString(3);
    int timeout = args.getInt(4) * 1000;
    boolean followRedirect = args.getBoolean(5);
    String responseType = args.getString(6);

    CordovaHttpUpload upload = new CordovaHttpUpload(url, headers, filePath, uploadName, timeout, followRedirect,
        responseType, this.tlsConfiguration, callbackContext);

    cordova.getThreadPool().execute(upload);

    return true;
  }

  private boolean downloadFile(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String url = args.getString(0);
    JSONObject headers = args.getJSONObject(1);
    String filePath = args.getString(2);
    int timeout = args.getInt(3) * 1000;
    boolean followRedirect = args.getBoolean(4);

    CordovaHttpDownload download = new CordovaHttpDownload(url, headers, filePath, timeout, followRedirect,
        this.tlsConfiguration, callbackContext);

    cordova.getThreadPool().execute(download);

    return true;
  }

  private boolean setServerTrustMode(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    CordovaServerTrust runnable = new CordovaServerTrust(args.getString(0), this.cordova.getActivity(),
        this.tlsConfiguration, callbackContext);

    cordova.getThreadPool().execute(runnable);

    return true;
  }

  private boolean setClientAuthMode(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    byte[] pkcs = args.isNull(2) ? null : Base64.decode(args.getString(2), Base64.DEFAULT);

    CordovaClientAuth runnable = new CordovaClientAuth(args.getString(0), args.isNull(1) ? null : args.getString(1),
        pkcs, args.getString(3), this.cordova.getActivity(), this.cordova.getActivity().getApplicationContext(),
        this.tlsConfiguration, callbackContext);

    cordova.getThreadPool().execute(runnable);

    return true;
  }
}
