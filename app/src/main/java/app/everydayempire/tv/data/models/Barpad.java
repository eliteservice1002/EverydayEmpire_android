package app.everydayempire.tv.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

import androidx.annotation.Nullable;

public class Barpad implements Parcelable {

    public int id;
    public String name;
    public String description;
    public User user;
    @Nullable
    public Song song;

    public Barpad() {
    }

    protected Barpad(Parcel in) {
        id = in.readInt();
        name = in.readString();
        description = in.readString();
        user = in.readParcelable(User.class.getClassLoader());
        song = in.readParcelable(Song.class.getClassLoader());
    }

    public static final Creator<Barpad> CREATOR = new Creator<Barpad>() {
        @Override
        public Barpad createFromParcel(Parcel in) {
            return new Barpad(in);
        }

        @Override
        public Barpad[] newArray(int size) {
            return new Barpad[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeParcelable(user, flags);
        dest.writeParcelable(song, flags);
    }
}
