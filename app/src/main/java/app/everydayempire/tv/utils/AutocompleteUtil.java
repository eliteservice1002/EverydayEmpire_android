package app.everydayempire.tv.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.widget.EditText;

import com.google.android.material.color.MaterialColors;
import com.otaliastudios.autocomplete.Autocomplete;
import com.otaliastudios.autocomplete.AutocompleteCallback;
import com.otaliastudios.autocomplete.CharPolicy;

import app.everydayempire.tv.R;
import app.everydayempire.tv.autocomplete.HashtagPresenter;
import app.everydayempire.tv.autocomplete.UserPresenter;
import app.everydayempire.tv.data.models.Hashtag;
import app.everydayempire.tv.data.models.User;

final public class AutocompleteUtil {

    public static void setupForHashtags(Context context, EditText input) {
        int color = MaterialColors.getColor(context, R.attr.colorSurface, Color.BLACK);
        Autocomplete.<Hashtag>on(input)
                .with(5)
                .with(new ColorDrawable(color))
                .with(new CharPolicy('#'))
                .with(new HashtagPresenter(context))
                .with(new AutocompleteCallback<Hashtag>() {

                    @Override
                    public boolean onPopupItemClicked(Editable editable, Hashtag item) {
                        int[] range = CharPolicy.getQueryRange(editable);
                        if (range == null) {
                            return false;
                        }

                        editable.replace(range[0], range[1], item.name);
                        return true;
                    }

                    @Override
                    public void onPopupVisibilityChanged(boolean shown) {
                    }
                })
                .build();
    }

    public static void setupForUsers(Context context, EditText input) {
        int color = MaterialColors.getColor(context, R.attr.colorSurface, Color.BLACK);
        Autocomplete.<User>on(input)
                .with(5)
                .with(new ColorDrawable(color))
                .with(new CharPolicy('@'))
                .with(new UserPresenter(context))
                .with(new AutocompleteCallback<User>() {

                    @Override
                    public boolean onPopupItemClicked(Editable editable, User item) {
                        int[] range = CharPolicy.getQueryRange(editable);
                        if (range == null) {
                            return false;
                        }

                        editable.replace(range[0], range[1], item.username);
                        return true;
                    }

                    @Override
                    public void onPopupVisibilityChanged(boolean shown) {
                    }
                })
                .build();
    }
}
