package com.syncedsoftware.popularmovies;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Izodine on 9/8/2015.
 */
public class JSONMovieDBUtils {

    private JSONMovieDBUtils() {
    } // No JSONUtils instances for you! (Get the reference?)

    public static String getStringNodeInResults(JSONObject response, String nodeName) {

        String resultNode = null;

        try {
            resultNode = response.getString(nodeName);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return resultNode;
    }


    public static String getStringNodeInResults(String responseString, String nodeName) {

        String resultNode = null;
        try {
            resultNode = getStringNodeInResults(new JSONObject(responseString), nodeName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return resultNode;
    }

    public static String[] getYoutubeTrailers(JSONObject responseObject) {

        String[] trailers = null;

        try {
            JSONArray reviewArray = responseObject
                    .getJSONObject("trailers")
                    .getJSONArray("youtube");

            trailers = new String[reviewArray.length()];

            for (int i = 0; i < reviewArray.length(); i++) {
                JSONObject reviewNode = reviewArray.getJSONObject(i);
                trailers[i] = reviewNode.getString("source") + ":" + reviewNode.getString("name");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return trailers;

    }

    public static String[] getReviews(JSONObject response) {

        StringBuilder resultNode = new StringBuilder();

        try {
            JSONArray reviewArray = response
                    .getJSONObject("reviews")
                    .getJSONArray("results");

            for (int i = 0; i < reviewArray.length(); i++) {
                JSONObject reviewNode = reviewArray.getJSONObject(i);
                resultNode.append("\"")
                        .append(reviewNode.getString("content"))
                        .append("\"")
                        .append(":")
                        .append("— ")
                        .append(reviewNode.getString("author"))
                        .append(":")
                        .append('\n');
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (resultNode.length() == 0) {
            return null;
        }
        return resultNode.toString().split(":");
    }

    public static String[] getYoutubeTrailers(String responseString) {
        String[] trailers = null;

        try {
            trailers = getYoutubeTrailers(new JSONObject(responseString));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return trailers;
    }

    public static String[] getReviews(String responseString) {
        try {
            return getReviews(new JSONObject(responseString));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static URL getUrl(JSONObject response) {

        URL imageUrl = null;

        try {
            imageUrl = new URL("http://image.tmdb.org/t/p/w185/" + response
                    .get("poster_path")
                    .toString());
        } catch (MalformedURLException | JSONException e) {
            e.printStackTrace();
        }

        return imageUrl;
    }

    public static URL getUrl(String responseString) {

        URL url;

        try {
            url = getUrl(new JSONObject(responseString));
            return url;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
