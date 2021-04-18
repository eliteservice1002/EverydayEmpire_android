package app.everydayempire.tv.fragments;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.jakewharton.rxbinding4.widget.RxTextView;
import com.kaopiz.kprogresshud.KProgressHUD;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import app.everydayempire.tv.MainApplication;
import app.everydayempire.tv.R;
import app.everydayempire.tv.SharedConstants;
import app.everydayempire.tv.activities.MainActivity;
import app.everydayempire.tv.activities.RecorderActivity;
import app.everydayempire.tv.activities.SongPickerActivity;
import app.everydayempire.tv.common.LoadingState;
import app.everydayempire.tv.data.api.REST;
import app.everydayempire.tv.data.models.Barpad;
import app.everydayempire.tv.data.models.Song;
import app.everydayempire.tv.data.models.User;
import app.everydayempire.tv.data.models.Wrappers;
import app.everydayempire.tv.utils.VideoUtil;
import io.reactivex.rxjava3.disposables.Disposable;
import nl.changer.audiowife.AudioWife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;
import static app.everydayempire.tv.activities.MainActivity.EXTRA_AUDIO;
import static app.everydayempire.tv.activities.MainActivity.EXTRA_SONG;
import static app.everydayempire.tv.activities.MainActivity.EXTRA_USER;
import static app.everydayempire.tv.activities.RecorderActivity.EXTRA_BARPAD;

public class EditBarpadFragment extends Fragment {

    public static final String ARG_BARPAD = "barpad";
    private static final String TAG = "EditBarpadFragment";

    private int mBarpad;
    private Barpad mResult;
    private final List<Disposable> mDisposables = new ArrayList<>();
    private EditBarpadFragment.EditBarpadFragmentViewModel mModel;
    private MainActivity.MainActivityViewModel mModel1;
    private boolean mRecord = false;

    private MediaPlayer mMediaPlayer;
    private Button playBtn, pauseBtn;
    private SeekBar seekBar;
    private static int sTime = 0, eTime = 0;
    private Handler hdlr = new Handler();

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SharedConstants.REQUEST_CODE_PICK_BARPAD_SONG && resultCode == RESULT_OK && data != null) {
            Log.e("Music", "barpad.song.title");
            Song song = data.getParcelableExtra(EXTRA_SONG);
            Uri file = data.getParcelableExtra(EXTRA_AUDIO);
            updateBarpad(song, file);
        }
    }

    private void updateBarpad(Song song, Uri file) {
        Barpad barpad = mModel.barpad.getValue();
        barpad.song = song;
        mModel.barpad.setValue(barpad);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBarpad = requireArguments().getInt(ARG_BARPAD);
        mModel = new ViewModelProvider(this).get(EditBarpadFragment.EditBarpadFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_edit_barpad, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Disposable disposable : mDisposables) {
            disposable.dispose();
        }

        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }

            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        mDisposables.clear();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            playBtn.setVisibility(View.VISIBLE);
            pauseBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LoadingState state = mModel.state.getValue();
        Barpad barpad = mModel.barpad.getValue();
        if (barpad == null && state != LoadingState.LOADING && mBarpad != 0) {
            loadBarpad();
        }

        if (mBarpad == 0) {
            mModel.state.setValue(LoadingState.LOADED);
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            playBtn.setVisibility(View.GONE);
            pauseBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        User user = requireActivity().getIntent().getParcelableExtra(EXTRA_USER);
        if (user != null) {
            mModel.user = user;
        }
        else  {
            mModel1 = new ViewModelProvider(requireActivity())
                    .get(MainActivity.MainActivityViewModel.class);
            mModel.user = mModel1.user.getValue();
        }
        ImageButton back = view.findViewById(R.id.header_back);
        back.setImageResource(R.drawable.ic_back);
        back.setOnClickListener(c -> {
            ((MainActivity)requireActivity()).popBackStack();
        });
        back.setVisibility(View.VISIBLE);

        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.barpad_label);

        view.findViewById(R.id.header_more).setVisibility(View.GONE);

        Button button = view.findViewById(R.id.header_button);
        button.setOnClickListener(c ->  {
            mRecord = false;
            updateWithServer();
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }

                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            ((MainActivity)requireActivity()).showBarpadPage();
        });
        button.setText("SAVE");
        button.setVisibility(View.VISIBLE);

        ImageButton record = view.findViewById(R.id.record_with_barpad);
        ImageButton record1 = view.findViewById(R.id.record_with_barpad3);

        record.setOnClickListener(c -> {
            mRecord = true;
            updateWithServer();
        });

        record1.setOnClickListener(c -> {
            mRecord = true;
            updateWithServer();
        });

        playBtn = view.findViewById(R.id.play);
        pauseBtn = view.findViewById(R.id.pause);
        seekBar = view.findViewById(R.id.seekbar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mMediaPlayer != null && fromUser){
                    mMediaPlayer.seekTo(progress);
                }
            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.start();

                    pauseBtn.setVisibility(View.VISIBLE);
                    playBtn.setVisibility(View.GONE);
                }
            }
        });

        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.pause();
                    pauseBtn.setVisibility(View.GONE);
                    playBtn.setVisibility(View.VISIBLE);
                }
            }
        });



        TextView change = view.findViewById(R.id.change_beat);

        change.setOnClickListener(c -> {
            Intent intent = new Intent(requireActivity(), SongPickerActivity.class);
            intent.putExtra(SongPickerActivity.EXTRA_FROM_HOME, true);
            intent.putExtra(SongPickerActivity.EXTRA_FROM_BARPAD, true);
            intent.putExtra(SongPickerActivity.EXTRA_BARPAD_VALUE, mBarpad);
            startActivityForResult(intent, SharedConstants.REQUEST_CODE_PICK_BARPAD_SONG);
        });

        Button select = view.findViewById(R.id.select_beat_button);
        select.setVisibility(View.VISIBLE);
        select.setText(R.string.select_beat);
        select.setOnClickListener(c -> {
            Intent intent = new Intent(requireActivity(), SongPickerActivity.class);
            intent.putExtra(SongPickerActivity.EXTRA_FROM_HOME, true);
            intent.putExtra(SongPickerActivity.EXTRA_FROM_BARPAD, true);
            intent.putExtra(SongPickerActivity.EXTRA_BARPAD_VALUE, mBarpad);
            startActivityForResult(intent, SharedConstants.REQUEST_CODE_PICK_BARPAD_SONG);
        });

        Disposable disposable;
        TextInputLayout barpad_name = view.findViewById(R.id.barpad_name);
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(barpad_name.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel.name = editable != null ? editable.toString() : null;
                });

        TextInputLayout barpad_description = view.findViewById(R.id.barpad_description);
        EditText description = view.findViewById(R.id.edit_description);
        description.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if (description.hasFocus()) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    switch (event.getAction() & MotionEvent.ACTION_MASK){
                        case MotionEvent.ACTION_SCROLL:
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                            return true;
                    }
                }
                return false;
            }
        });
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(barpad_description.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel.description = editable != null ? editable.toString() : null;
                });

        mModel.errors.observe(getViewLifecycleOwner(), errors -> {
            barpad_name.setError(null);
            barpad_description.setError(null);
            if (errors == null) {
                return;
            }
            if (errors.containsKey("name")) {
                barpad_name.setError(errors.get("name"));
            }
            if (errors.containsKey("description")) {
                barpad_description.setError(errors.get("description"));
            }
        });

        View content = view.findViewById(R.id.content);
        View loading = view.findViewById(R.id.loading);

        mModel.state.observe(getViewLifecycleOwner(), state -> {
            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
            content.setVisibility(state == LoadingState.LOADED ? View.VISIBLE : View.GONE);
        });

        mModel.barpad.observe(getViewLifecycleOwner(), barpad -> {
                barpad_name.getEditText().setText(mModel.name = barpad.name);
                barpad_description.getEditText().setText(mModel.description = barpad.description);

                if (barpad.song != null) {
                    view.findViewById(R.id.select_beat_layout).setVisibility(View.GONE);
                    view.findViewById(R.id.play_beat_layout).setVisibility(View.VISIBLE);

                    TextView information = view.findViewById(R.id.information);
                    information.setText(barpad.song.title + " | " + barpad.song.artist);
                    information.setOnClickListener(c -> {
                        Intent intent = new Intent(requireActivity(), SongPickerActivity.class);
                        intent.putExtra(SongPickerActivity.EXTRA_FROM_HOME, true);
                        intent.putExtra(SongPickerActivity.EXTRA_FROM_BARPAD, true);
                        intent.putExtra(SongPickerActivity.EXTRA_BARPAD_VALUE, mBarpad);
                        requireActivity().startActivityForResult(intent, SharedConstants.REQUEST_CODE_PICK_BARPAD_SONG);
                    });

                    File songs = new File(((MainActivity)requireActivity()).getFilesDir(), "songs");
                    if (!songs.exists() && !songs.mkdirs()) {
                        Log.w(TAG, "Could not create directory at " + songs);
                    }
                    String extension = barpad.song.audio.substring(barpad.song.audio.lastIndexOf(".") + 1);
                    File audio = new File(songs, barpad.song.id + extension);

                    Uri file = Uri.fromFile(audio);

                    if (audio.exists()) {
                        PlaySelection(barpad.song, file, view);
                        return;
                    }

                    KProgressHUD progress = KProgressHUD.create(requireActivity())
                            .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                            .setLabel(getString(R.string.progress_title))
                            .setCancellable(false)
                            .show();
                    WorkRequest request = VideoUtil.createDownloadRequest(barpad.song.audio, audio, false);
                    WorkManager wm = WorkManager.getInstance(requireActivity());
                    wm.enqueue(request);
                    wm.getWorkInfoByIdLiveData(request.getId())
                            .observe(requireActivity(), info -> {
                                boolean ended = info.getState() == WorkInfo.State.CANCELLED
                                        || info.getState() == WorkInfo.State.FAILED
                                        || info.getState() == WorkInfo.State.SUCCEEDED;
                                if (ended) {
                                    progress.dismiss();
                                }

                                if (info.getState() == WorkInfo.State.SUCCEEDED) {
                                    PlaySelection(barpad.song, file, view);
                                }
                            });
                }
                else {
                    view.findViewById(R.id.select_beat_layout).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.play_beat_layout).setVisibility(View.GONE);
                }
            });
    }

    private Runnable UpdateSongTime = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer != null) {
                sTime = mMediaPlayer.getCurrentPosition();
                seekBar.setProgress(sTime);
                hdlr.postDelayed(this, 100);
            }
        }
    };

    private void loadBarpad() {
        mModel.state.setValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.barpadsShow(mBarpad)
                .enqueue(new Callback<Wrappers.Single<Barpad>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<Barpad>> call,
                            @Nullable Response<Wrappers.Single<Barpad>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Fetching Barpad returned " + code + '.');
                        if (response != null && response.isSuccessful()) {
                            //noinspection ConstantConditions
                            Barpad barpad = response.body().data;
                            mModel.barpad.setValue(barpad);
                            mModel.state.setValue(LoadingState.LOADED);
                        } else {
                            mModel.state.setValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<Barpad>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed when trying to fetch Barpad.", t);
                        mModel.state.setValue(LoadingState.ERROR);
                    }
                });
    }

    public static EditBarpadFragment newInstance(Barpad barpad) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_BARPAD, barpad.id);
        EditBarpadFragment fragment = new EditBarpadFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    private void showErrors(JSONObject json) throws Exception {
        JSONObject errors = json.getJSONObject("errors");
        Map<String, String> messages = new HashMap<>();
        String[] keys = new String[]{
                "name", "description"};
        for (String key : keys) {
            JSONArray fields = errors.optJSONArray(key);
            if (fields != null) {
                messages.put(key, fields.getString(0));
            }
        }

        mModel.errors.postValue(messages);
    }

    private void updateWithServer() {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        mModel.errors.postValue(null);
        REST rest = MainApplication.getContainer().get(REST.class);
        Call<Wrappers.Single<Barpad>> call;
        if (mBarpad > 0) {
            call = rest.barpadsUpdate(
                    mBarpad,
                    mModel.song != null ? mModel.song.id+"" : null,
                    mModel.user.id,
                    mModel.description,
                    mModel.name
            );
        }
        else {
             call = rest.barpadsCreate(
                    mModel.song == null ? null : mModel.song.id+"",
                    mModel.user.id,
                    mModel.description,
                    mModel.name
            );
        }

        call.enqueue(new Callback<Wrappers.Single<Barpad>>() {

            @Override
            public void onResponse(
                    @Nullable Call<Wrappers.Single<Barpad>> call,
                    @Nullable Response<Wrappers.Single<Barpad>> response
            ) {
                progress.dismiss();
                if (response != null) {
                    if (response.isSuccessful()) {
                        mResult = response.body().data;
                        Toast.makeText(requireContext(), R.string.message_barpad_updated, Toast.LENGTH_SHORT).show();

                        if (mRecord) {
                            File songs = new File(requireActivity().getFilesDir(), "songs");
                            if (!songs.exists() && !songs.mkdirs()) {
                                Log.w(TAG, "Could not create directory at " + songs);
                            }

                            if (mResult.song == null) {
                                Intent intent = new Intent(requireActivity(), RecorderActivity.class);
                                intent.putExtra(EXTRA_BARPAD, mResult);
                                startActivityForResult(intent, SharedConstants.REQUEST_CODE_PICK_BARPAD_SONG);
                            }
                            else {
                                String extension = mResult.song.audio.substring(mResult.song.audio.lastIndexOf(".") + 1);
                                File audio = new File(songs, mResult.song.id + extension);
                                Intent intent = new Intent(requireActivity(), RecorderActivity.class);
                                intent.putExtra(EXTRA_BARPAD, mResult);
                                intent.putExtra(RecorderActivity.EXTRA_SONG, mResult.song);
                                intent.putExtra(RecorderActivity.EXTRA_AUDIO, Uri.fromFile(audio));
                                startActivityForResult(intent, SharedConstants.REQUEST_CODE_PICK_BARPAD_SONG);
                            }

                        }
                    } else if (response.code() == 422) {
                        try {
                            String content = response.errorBody().string();
                            showErrors(new JSONObject(content));
                        } catch (Exception ignore) {
                        }
                    }
                }
            }

            @Override
            public void onFailure(
                    @Nullable Call<Wrappers.Single<Barpad>> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Failed when trying to update Barpad.", t);
                progress.dismiss();
            }
        });
    }

    public static class EditBarpadFragmentViewModel extends ViewModel {

        public String description;
        public String name;
        public Song song;
        public User user;

        public final MutableLiveData<Map<String, String>> errors = new MutableLiveData<>();
        public final MutableLiveData<LoadingState> state = new MutableLiveData<>();
        public final MutableLiveData<Barpad> barpad = new MutableLiveData<>();

    }

    public void PlaySelection(Song song, Uri file,  View view) {
        mModel.song = song;

        mMediaPlayer = MediaPlayer.create(this.requireActivity(), file);
        mMediaPlayer.setOnCompletionListener(mp -> mMediaPlayer = null);

        mMediaPlayer.start();
        eTime = mMediaPlayer.getDuration();
        sTime = mMediaPlayer.getCurrentPosition();
        seekBar.setMax(eTime);
        seekBar.setProgress(sTime);
        hdlr.postDelayed(UpdateSongTime, 100);
    }
}
