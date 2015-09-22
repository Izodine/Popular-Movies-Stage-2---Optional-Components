package com.syncedsoftware.popularmovies;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Izodine on 7/19/2015.
 */
public class GridViewItem implements Parcelable {

    private byte[] imageBytes = null;
    private String responseString;

    private GridViewItem(Parcel input) {
        responseString = input.readString();
        input.readByteArray(imageBytes);
    }

    public GridViewItem(String responseString) {

        this.responseString = responseString;

    }

    public GridViewItem(String responseString, byte[] imageBytes) {

        this.responseString = responseString;

        // Convert byte[] to Byte[] for parcelable
        this.imageBytes = imageBytes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getContentsString());
        dest.writeByteArray(imageBytes);
    }

    public final Parcelable.Creator<GridViewItem> CREATOR = new Parcelable.Creator<GridViewItem>() {

        @Override
        public GridViewItem createFromParcel(Parcel source) {
            return new GridViewItem(source);
        }

        @Override
        public GridViewItem[] newArray(int size) {
            return new GridViewItem[size];
        }
    };

    public String getContentsString() {
        return responseString;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }
}
