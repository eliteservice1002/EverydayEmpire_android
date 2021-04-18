package app.everydayempire.tv.events;

import androidx.annotation.Nullable;

import app.everydayempire.tv.data.models.Clip;

public class RecordingOptionsEvent {

    private final Clip mClip;

    public RecordingOptionsEvent(@Nullable Clip clip) {
        mClip = clip;
    }

    public Clip getClip() {
        return mClip;
    }
}
