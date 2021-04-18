package app.everydayempire.tv.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;

import app.everydayempire.tv.R;
import app.everydayempire.tv.events.AudioTargetUpdateEvent;

public class AdjustAudioFragment extends Fragment {

    private static final String ARG_TARGET = "target";
    private static final String ARG_VOLUME = "volume";

    public static final int TARGET_VIDEO = 60600 + 1;
    public static final int TARGET_SONG = 60600 + 2;

    private AdjustAudioViewModel mModel;
    private int mTarget;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float volume = requireArguments().getFloat(ARG_VOLUME);
        AdjustAudioViewModel.Factory factory = new AdjustAudioViewModel.Factory(volume);
        mModel = new ViewModelProvider(this, factory).get(AdjustAudioViewModel.class);
        mTarget = requireArguments().getInt(ARG_TARGET);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_adjust_audio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Slider delay = view.findViewById(R.id.delay);
        delay.setLabelFormatter(value ->
                String.format(Locale.US, "%dms", (int)value - 1000));
        //noinspection ConstantConditions
        delay.setValue(mModel.delay.getValue());
        delay.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {

            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) { }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                mModel.delay.postValue(slider.getValue());
            }
        });
        Slider volume = view.findViewById(R.id.volume);
        volume.setLabelFormatter(value -> String.format(Locale.US, "%d%%", (int)value));
        //noinspection ConstantConditions
        volume.setValue(mModel.volume.getValue());
        volume.addOnChangeListener((v, value, user) -> mModel.volume.postValue(value));
        view.findViewById(R.id.delay_minus).setOnClickListener(v -> {
            float value = mModel.delay.getValue();
            if (value >= 10) {
                delay.setValue(value - 10);
            }
        });
        view.findViewById(R.id.delay_plus).setOnClickListener(v -> {
            float value = mModel.delay.getValue();
            if (value <= 990) {
                delay.setValue(value + 10);
            }
        });
        view.findViewById(R.id.volume_minus).setOnClickListener(v -> {
            float value = mModel.volume.getValue();
            if (value >= 5) {
                volume.setValue(value - 5);
            }
        });
        view.findViewById(R.id.volume_plus).setOnClickListener(v -> {
            float value = mModel.volume.getValue();
            if (value <= 95) {
                volume.setValue(value + 5);
            }
        });
        mModel.delay.observe(getViewLifecycleOwner(), value -> {
            EventBus.getDefault()
                    .post(new AudioTargetUpdateEvent(mTarget, value, mModel.volume.getValue()));
        });
        mModel.volume.observe(getViewLifecycleOwner(), value -> {
            EventBus.getDefault()
                    .post(new AudioTargetUpdateEvent(mTarget, mModel.delay.getValue(), value));
        });
    }

    public static AdjustAudioFragment newInstance(int target, float volume) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_TARGET, target);
        arguments.putFloat(ARG_VOLUME, volume);
        AdjustAudioFragment fragment = new AdjustAudioFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    public static final class AdjustAudioViewModel extends ViewModel {

        public final MutableLiveData<Float> delay = new MutableLiveData<>(1000f);
        public final MutableLiveData<Float> volume;

        public AdjustAudioViewModel(float value) {
            volume = new MutableLiveData<>(value);
        }

        private static class Factory implements ViewModelProvider.Factory {

            private final float mVolume;

            public Factory(float volume) {
                mVolume = volume;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new AdjustAudioViewModel(mVolume);
            }
        }
    }
}
