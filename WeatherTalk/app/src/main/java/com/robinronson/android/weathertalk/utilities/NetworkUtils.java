package com.robinronson.android.weathertalk.utilities;

import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by robinronson on 4/22/17.
 */

public class NetworkUtils {
    final static String OPENWEATHER_BASE_URL =
            "http://api.openweathermap.org/data/2.5/weather?";
    final static String PARAM_QUERY = "zip";
    final static String PARAM_UNIT = "units";
    final static String unit = "metric";
    final static String ID = "APPID";
    final static String KEY = "c6131635613cc8e803e15fc27f610a15";

    public static URL buildUrl(String weatherSearchQuery) {
        Uri builtUri = Uri.parse(OPENWEATHER_BASE_URL).buildUpon()
                .appendQueryParameter(PARAM_QUERY,weatherSearchQuery)
                .appendQueryParameter(PARAM_UNIT,unit)
                .appendQueryParameter(ID,KEY)
                .build();
        URL url = null;
        try {
            url = new  URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    public static String getResponceFromHttpURL(URL url) throws IOException{
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.connect();
        try {InputStream in = urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            StringBuffer buffer = new StringBuffer();
            while ((line = reader.readLine()) != null){
                buffer.append(line);
            }
            return buffer.toString();
        } finally {
            urlConnection.disconnect();
        }

        }

    }




