package app.everydayempire.tv.activities;

import android.os.Bundle;

import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;
import app.everydayempire.tv.R;

public class IntroActivity extends com.heinrichreimersoftware.materialintro.app.IntroActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addSlide(new SimpleSlide.Builder()
                .title(R.string.slide_1_title)
                .description(R.string.slide_1_description)
                .image(R.drawable.slide_1_image)
                .background(android.R.color.black)
                .backgroundDark(R.color.colorSlideButton)
                .build());
        addSlide(new SimpleSlide.Builder()
                .title(R.string.slide_2_title)
                .description(R.string.slide_2_description)
                .image(R.drawable.slide_2_image)
                .background(android.R.color.black)
                .backgroundDark(R.color.colorSlideButton)
                .build());
    }
}
