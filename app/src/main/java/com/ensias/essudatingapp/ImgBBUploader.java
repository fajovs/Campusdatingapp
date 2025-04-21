package com.ensias.essudatingapp;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class ImgBBUploader {

    private static final String API_URL = "https://api.imgbb.com/1/upload";
    private static final String API_KEY = "433fd6c7b2f033842a3aeaabf5d01382"; // Replace with your ImgBB API key

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    public static void upload(final Context context, final byte[] imageData, final UploadCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Convert image data to Base64
                    String base64Image = Base64.encodeToString(imageData, Base64.DEFAULT);

                    // Create connection
                    URL url = new URL(API_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);

                    // Create request parameters
                    String postData = "key=" + API_KEY + "&image=" + URLEncoder.encode(base64Image, "UTF-8");

                    // Send request
                    DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                    wr.writeBytes(postData);
                    wr.flush();
                    wr.close();

                    // Get response
                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        // Parse JSON response
                        JSONObject jsonResponse = new JSONObject(response.toString());

                        if (jsonResponse.getBoolean("success")) {
                            JSONObject data = jsonResponse.getJSONObject("data");
                            final String imageUrl = data.getString("url");

                            // Return success on main thread
                            android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSuccess(imageUrl);
                                }
                            });
                        } else {
                            final String error = "Upload failed: " + jsonResponse.getString("error");

                            // Return error on main thread
                            android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFailure(error);
                                }
                            });
                        }
                    } else {
                        final String error = "HTTP Error: " + responseCode;

                        // Return error on main thread
                        android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailure(error);
                            }
                        });
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    final String error = "Exception: " + e.getMessage();

                    // Return error on main thread
                    android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(error);
                        }
                    });
                }
            }
        }).start();
    }
}
