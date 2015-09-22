package com.syncedsoftware.popularmovies;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivityFragment extends Fragment {

    private final String LOG_TAG = FetchPopularMoviesTask.class.getSimpleName();
    private static boolean isTablet;
    private FetchPopularMoviesTask fetchPopularMoviesTask = null;
    private boolean sortByPopularity = true;
    private Context context;
    private WebImageAdapter adapter;
    private ArrayList<GridViewItem> movieData = new ArrayList<>(15);

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // Tablet/Phone specific flag
        isTablet = getResources().getBoolean(R.bool.isTablet);

        setHasOptionsMenu(true);

        if (savedInstanceState == null) { //Data doesn't exist, fetch it
            fetchPopularMoviesTask = new FetchPopularMoviesTask();
            fetchPopularMoviesTask.execute();
            this.context = getActivity();
        } else { //Data exists, load it
            movieData = savedInstanceState.getParcelableArrayList("data");
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("data", movieData);
    }

    public void dispatchToastOnUIThread(final String text) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        adapter = new WebImageAdapter(getActivity(),
                R.layout.grid_view_poster,
                R.id.posterImageView,
                movieData);

        GridView gridView = (GridView) rootView.findViewById(R.id.gridView);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            gridView.setNumColumns(5);
        } else {
            gridView.setNumColumns(3);
        }
        gridView.setAdapter(adapter);

        return rootView;
    }

    private void updateAdapter(JSONObject[] dataArray) {
        adapter.clear();
        // Drop data into arrays
        for (JSONObject aDataArray : dataArray) {
            movieData.add(new GridViewItem(aDataArray.toString()));
        }
        adapter.notifyDataSetChanged();
    }

    private void updateAdapterWithImageBytes(JSONObject[] dataArray, List<byte[]> imageByteList) {
        adapter.clear();
        // Drop data into arrays
        for (int i = 0; i < dataArray.length; i++) {
            movieData.add(new GridViewItem(dataArray[i].toString(), imageByteList.get(i)));
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (fetchPopularMoviesTask == null) {
            fetchPopularMoviesTask = new FetchPopularMoviesTask();
            fetchPopularMoviesTask.execute();
            return false;
        }

        AsyncTask.Status status = fetchPopularMoviesTask.getStatus();
        if (id == R.id.action_sortbypop) {
            sortByPopularity = true;

            if (status != AsyncTask.Status.RUNNING || fetchPopularMoviesTask.isCancelled()) {
                fetchPopularMoviesTask = new FetchPopularMoviesTask();
                fetchPopularMoviesTask.execute();
            } else {
                dispatchToastOnUIThread("Sorting operating already in progress.");
            }
            return true;
        }

        if (id == R.id.action_sortbyhighrating) {
            sortByPopularity = false;
            if (status != AsyncTask.Status.RUNNING || fetchPopularMoviesTask.isCancelled()) {
                fetchPopularMoviesTask = new FetchPopularMoviesTask();
                fetchPopularMoviesTask.execute();
            } else {
                dispatchToastOnUIThread("Sorting operating already in progress.");
            }
            return true;
        }

        if (id == R.id.action_sortbyfavorite) {

            if (status != AsyncTask.Status.RUNNING || fetchPopularMoviesTask.isCancelled()) {
                Cursor cursor = getActivity().getContentResolver().query(MovieContentProvider.CONTENT_URI,
                        new String[]{MovieContentProvider.MOVIE,
                                MovieContentProvider.POSTER},
                        null,
                        null,
                        null);
                if (cursor.getCount() <= 0) {
                    return false;
                }
                cursor.moveToFirst();
                JSONObject[] movieArray = new JSONObject[cursor.getCount()];
                List<byte[]> imageBytesList = new ArrayList<>();

                int counter = 0;
                while (!cursor.isAfterLast()) {
                    try {
                        movieArray[counter] = new JSONObject(cursor.getString(cursor.getColumnIndex(MovieContentProvider.MOVIE)));
                        imageBytesList.add(cursor.getBlob(cursor.getColumnIndex(MovieContentProvider.POSTER)));
                        counter++;
                        cursor.moveToNext();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        cursor.close();
                    }
                }
                cursor.close();
                updateAdapterWithImageBytes(movieArray, imageBytesList);
            } else {
                dispatchToastOnUIThread("Sorting operating already in progress.");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class FetchPopularMoviesTask extends AsyncTask<String, Void, JSONObject[]> {

        @Override
        protected JSONObject[] doInBackground(String... params) {

            HashMap<String, String> queryParams = new HashMap<>();
            JSONObject[] movieResponses = null;
            queryParams.put("append_to_response", "trailers,reviews");

            if (sortByPopularity)
                queryParams.put("sort_by", "popularity.desc");
            else
                queryParams.put("sort_by", "vote_average.desc");

            try {
                JSONArray rootArray = new JSONObject(getMovieInformation("http://api.themoviedb.org/3/",
                        "discover",
                        null,
                        "movie",
                        queryParams))
                        .getJSONArray("results");

                JSONObject[] dataObject = new JSONObject[rootArray.length()];
                movieResponses = new JSONObject[rootArray.length()];
                for (int i = 0; i < rootArray.length(); i++) {
                    dataObject[i] = rootArray.getJSONObject(i);
                    movieResponses[i] = new JSONObject(getMovieInformation("http://api.themoviedb.org/3/",
                            "movie",
                            Integer.toString(dataObject[i].getInt("id")),
                            null,
                            queryParams));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return movieResponses;
        }

        private String getMovieInformation(String BASE_URL,
                                           String MODE,
                                           String MOVIE_ID,
                                           String NODE,
                                           HashMap<String, String> queryParams) {

            return getData(buildURL(BASE_URL,
                    new String[]{MODE, MOVIE_ID, NODE},
                    queryParams));
        }

        private String buildURL(String BASE_URL,
                                String[] paths,
                                HashMap<String, String> params) {
            Uri.Builder movieDataURIBuilder = new Uri.Builder();
            movieDataURIBuilder.encodedPath(BASE_URL);
            if (paths != null) {
                for (String path : paths) {
                    if (path != null) {
                        movieDataURIBuilder.appendPath(path);
                    }
                }
            }
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    movieDataURIBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
                }
            }
            final String APIKEY = "<<INSERT API KEY HERE>>";
            movieDataURIBuilder.appendQueryParameter("api_key", APIKEY);
            return movieDataURIBuilder.build().toString();
        }

        private String getData(String dataUrl) {
            URL url;
            String popMoviesResponse = null;
            StringBuffer buffer;
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try {
                url = new URL(dataUrl);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                buffer = new StringBuffer();
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                if (buffer.length() == 0) {
                    dispatchToastOnUIThread("No data found");
                }
                popMoviesResponse = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                dispatchToastOnUIThread("Server is overloaded or internet connection is off.\n" +
                        "Make sure internet is enabled, and trigger a sort to try again.");
                cancel(true);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream ", e);
                    }
                }
            }
            return popMoviesResponse;
        }


        @Override
        protected void onPostExecute(JSONObject[] dataArray) {
            super.onPostExecute(dataArray);
            updateAdapter(dataArray);
        }

        @Override
        protected void onCancelled(JSONObject[] s) {
            super.onCancelled(s);
            Toast.makeText(context, "Fetch cancelled.", Toast.LENGTH_LONG)
                    .show();
        }

    }

    public class WebImageAdapter extends ArrayAdapter {

        private Context context;
        private List<ImageView> imageViews = new ArrayList<>();

        public WebImageAdapter(Context context, int resource, int textViewResourceId, List<GridViewItem> objects) {
            super(context, resource, textViewResourceId, objects);
            this.context = context;
        }

        @Override
        public void clear() {
            super.clear();
            getImageViews().clear();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            String url = null;
            byte[] imageBytes = ((GridViewItem) getItem(position)).getImageBytes();
            try {
                url = JSONMovieDBUtils.getUrl(
                        new JSONObject(((GridViewItem) getItem(position))
                                .getContentsString()))
                        .toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ImageView imageView;

            if (convertView == null) {

                imageView = new ImageView(context);
                imageView.setAdjustViewBounds(true);
                getImageViews().add(imageView);

            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentManager fm = getFragmentManager();

                    //Data field
                    Bundle fieldMap = new Bundle();
                    fieldMap.putParcelable("movie_parcel", (GridViewItem) getItem(position));

                    DetailsFragment detailsFragment = new DetailsFragment();
                    detailsFragment.setArguments(fieldMap);
                    FragmentTransaction ft = fm.beginTransaction();
                    if (isTablet) {
                        ft.replace(R.id.detailsFragmentContainer, detailsFragment);
                    } else {
                        ft.replace(R.id.mainFragmentContainer, detailsFragment);
                    }
                    ft.addToBackStack(null);
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.commit();

                }
            });

            if (imageBytes == null) {
                Picasso.with(context).load(url).into(imageView);
            } else {
                imageView.setImageBitmap(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
            }
            return imageView;

        }

        public List<ImageView> getImageViews() {
            return imageViews;
        }


    }


}
