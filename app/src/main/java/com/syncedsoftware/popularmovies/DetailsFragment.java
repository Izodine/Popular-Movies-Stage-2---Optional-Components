package com.syncedsoftware.popularmovies;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;

public class DetailsFragment extends Fragment {

    ContentResolver mResolver;
    private ShareActionProvider mShareActionProvider;
    private String mTrailerShareString;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mTrailerShareString);
        sendIntent.setType("text/plain");
        setShareIntent(sendIntent);

        switch (id) {

            case R.id.action_share_trailer_menu:

                startActivity(sendIntent);

                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_details, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.action_share_trailer);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        // Default intent
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mTrailerShareString);
        sendIntent.setType("text/plain");
        setShareIntent(sendIntent);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mResolver = getActivity().getContentResolver();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_details, container, false);

        final CheckBox favoriteCheckButton = (CheckBox) view.findViewById(R.id.favoriteCheckBox);
        favoriteCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String movieData = ((GridViewItem) getArguments()
                        .getParcelable("movie_parcel"))
                        .getContentsString();

                String id = JSONMovieDBUtils.getStringNodeInResults(movieData, "id");

                if (idExistsInDB(id)) {
                    mResolver.delete(MovieContentProvider.CONTENT_URI, (MovieContentProvider._ID + "=?"), new String[]{id});
                    Toast.makeText(getActivity(),
                            "Removed from favorites: " + JSONMovieDBUtils.getStringNodeInResults(movieData, "title"),
                            Toast.LENGTH_LONG).show();
                } else {
                    byte[] imageBytes = imageViewToByteArray();

                    ContentValues values = new ContentValues();
                    values.put(MovieContentProvider.MOVIE, movieData);
                    values.put(MovieContentProvider._ID, id);
                    values.put(MovieContentProvider.POSTER, imageBytes);

                    mResolver.insert(MovieContentProvider.CONTENT_URI, values);
                    Toast.makeText(getActivity(),
                            "Added to favorites: " + JSONMovieDBUtils.getStringNodeInResults(movieData, "title"),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        return view;
    }

    private byte[] imageViewToByteArray() {
        ImageView imageView = (ImageView) getActivity().findViewById(R.id.detailsPosterImageView);
        Bitmap drawable = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        drawable.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    private boolean idExistsInDB(String id) {
        // Linear search through records
        Cursor cursor = getActivity().getContentResolver().query(MovieContentProvider.CONTENT_URI,
                new String[]{MovieContentProvider._ID},
                null,
                null,
                null);

        if (cursor.getCount() <= 0) {
            return false;
        }

        cursor.moveToFirst();

        do {
            Log.d("Izodine", cursor.getString(cursor.getColumnIndex("_id")));
            if (cursor.getString(cursor.getColumnIndex("_id")).equals(id)) {
                cursor.close();
                return true;
            }
        } while (cursor.moveToNext());
        cursor.close();
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        initDetailFields(getArguments());
    }

    public void initDetailFields(Bundle fieldMap) {
        GridViewItem moviePacel = fieldMap.getParcelable("movie_parcel");
        String response = moviePacel.getContentsString();

        // Check is movie is favorited
        if (idExistsInDB(JSONMovieDBUtils.getStringNodeInResults(response, "id"))) {
            CheckBox checkBox = (CheckBox) getActivity().findViewById(R.id.favoriteCheckBox);
            checkBox.setChecked(true);
        }

        // If bytes != null, this means loading offline images
        if (moviePacel.getImageBytes() != null) {
            ((ImageView) getActivity().findViewById(R.id.detailsPosterImageView)).setImageBitmap(
                    BitmapFactory.decodeByteArray(moviePacel.getImageBytes(), 0, moviePacel.getImageBytes().length)
            );
        } else {
            Picasso.with(getActivity())
                    .load(String.valueOf(JSONMovieDBUtils.getUrl(response)))
                    .into((ImageView) getActivity().findViewById(R.id.detailsPosterImageView));
        }

        ((TextView) getActivity().findViewById(R.id.detailsTitleTextView))
                .setText(JSONMovieDBUtils.getStringNodeInResults(response, "title"));

        ((TextView) getActivity().findViewById(R.id.detailsSynopsisTextView))
                .setText("Overview: " + JSONMovieDBUtils.getStringNodeInResults(response, "overview"));

        ((TextView) getActivity().findViewById(R.id.detailsReleaseDateView))
                .setText("Released: " + JSONMovieDBUtils.getStringNodeInResults(response, "release_date"));

        ((TextView) getActivity().findViewById(R.id.detailsUserRatingTextView))
                .setText("Rating: " + JSONMovieDBUtils.getStringNodeInResults(response, "vote_average")
                        + " / 10");

        String[] reviews = JSONMovieDBUtils.getReviews(response);

        TableLayout reviewTableLayout = ((TableLayout) getActivity().findViewById(R.id.reviewTableView));
        if (reviews == null) {
            TextView view = new TextView(getActivity());
            view.setText("No reviews found.");
            reviewTableLayout.addView(view);
        } else {
            for (String review : reviews) {
                TextView view = new TextView(getActivity());
                view.setText(review);
                if (review.contains("Author")) {
                    view.setTypeface(null, Typeface.BOLD_ITALIC);
                }
                if (review.contains("\"")) {
                    view.setTypeface(null, Typeface.ITALIC);
                }
                reviewTableLayout.addView(view);
            }
        }

        final String[] trailers = JSONMovieDBUtils.getYoutubeTrailers(response);
        TableLayout trailerTableLayout = ((TableLayout) getActivity().findViewById(R.id.trailerTableView));
        if (trailers == null) {
            TextView view = new TextView(getActivity());
            view.setText("No media found.");
            trailerTableLayout.addView(view);
        } else {
            for (String trailer : trailers) {
                String[] videoInfo = trailer.split(":");
                final String link = videoInfo[0];
                final Uri destUri = Uri.parse("https://www.youtube.com/watch?v=" + link);
                mTrailerShareString = destUri.toString();
                String name = videoInfo[1];
                Button button = new Button(getActivity());
                button.setText(name);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent youtubeIntent = new Intent(Intent.ACTION_VIEW, destUri);
                        startActivity(youtubeIntent);
                    }
                });
                trailerTableLayout.addView(button);
            }
        }
    }

}
