package app.everydayempire.tv.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputLayout;
import com.jakewharton.rxbinding4.widget.RxTextView;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.thefinestartist.finestwebview.FinestWebView;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.everydayempire.tv.fragments.EditBarpadFragment;
import io.reactivex.rxjava3.disposables.Disposable;
import jp.wasabeef.recyclerview.adapters.SlideInBottomAnimationAdapter;
import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import app.everydayempire.tv.R;
import app.everydayempire.tv.SharedConstants;
import app.everydayempire.tv.common.DiffUtilCallback;
import app.everydayempire.tv.common.LoadingState;
import app.everydayempire.tv.data.SongDataSource;
import app.everydayempire.tv.data.SongSectionDataSource;
import app.everydayempire.tv.data.models.Song;
import app.everydayempire.tv.data.models.SongSection;
import app.everydayempire.tv.utils.IntentUtil;
import app.everydayempire.tv.utils.TempUtil;
import app.everydayempire.tv.utils.VideoUtil;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;
import nl.changer.audiowife.AudioWife;

public class SongPickerActivity extends AppCompatActivity {

    public static final String EXTRA_FROM_HOME = "from_home";
    public static final String EXTRA_FROM_BARPAD = "from_barpad";
    private static final String TAG = "SongPickerActivity";
    public static final String EXTRA_BARPAD_VALUE = "barpad_value";

    private final List<Disposable> mDisposables = new ArrayList<>();
    private SongPickerActivityViewModel mModel;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SharedConstants.REQUEST_CODE_PICK_SONG_FILE && resultCode == RESULT_OK && data != null) {
            try {
                closeWithSelection(null, copySongFile(data.getData()));
            } catch (Exception e) {
                Log.e(TAG, "Failed to copy song file on phone.");
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_picker);
        ImageButton close = findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(view -> finish());
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.beats_label);
        boolean home = getIntent().getBooleanExtra(EXTRA_FROM_HOME, false);
        if (home) {
            ImageButton close2 = findViewById(R.id.header_more);
            close2.setImageResource(R.drawable.ic_baseline_volume_off_24);
            close2.setOnClickListener(view -> closeWithSelection(null, null));
        } else {
            findViewById(R.id.header_more).setVisibility(View.GONE);
        }

        mModel = new ViewModelProvider(this).get(SongPickerActivityViewModel.class);
        TextInputLayout q = findViewById(R.id.q);
        q.getEditText().setText(mModel.searchTerm.getValue());
        Disposable disposable = RxTextView.afterTextChangeEvents(q.getEditText())
                .skipInitialValue()
                .debounce(250, TimeUnit.MILLISECONDS)
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel.searchTerm.postValue(editable != null ? editable.toString() : null);
                });
        mDisposables.add(disposable);
        RecyclerView songs = findViewById(R.id.songs);
        SongAdapter adapter1 = new SongAdapter();
        songs.setAdapter(new SlideInLeftAnimationAdapter(adapter1));
        mModel.songs.observe(this, adapter1::submitList);
        SwipeRefreshLayout swipe = findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            SongDataSource source = mModel.factory1.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        View browse = findViewById(R.id.browse);
        browse.setOnClickListener(v ->
                IntentUtil.startChooser(
                        this, SharedConstants.REQUEST_CODE_PICK_SONG_FILE, "audio/*"));
        View empty = findViewById(R.id.empty);
        View loading = findViewById(R.id.loading);
        mModel.state1.observe(this, state -> {
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }

            List<?> list = mModel.songs.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        RecyclerView sections = findViewById(R.id.sections);
        LinearLayoutManager llm =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        sections.setLayoutManager(llm);
        SongSectionAdapter adapter2 = new SongSectionAdapter();
        sections.setAdapter(new SlideInBottomAnimationAdapter(adapter2));
        OverScrollDecoratorHelper.setUpOverScroll(
                sections, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL);
        mModel.searchTerm.observe(this, search -> {
            mModel.factory1.q = search;
            SongDataSource source = mModel.factory1.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        mModel.sections.observe(this, adapter2::submitList);
        mModel.selection.observe(this, integers -> {
            mModel.factory1.sections = integers;
            SongDataSource source = mModel.factory1.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        View sheet = findViewById(R.id.song_preview_sheet);
        BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(sheet);
        TextView title2 = sheet.findViewById(R.id.header_title);
        title2.setText(R.string.preview_label);
        ImageButton close2 = sheet.findViewById(R.id.header_back);
        close2.setImageResource(R.drawable.ic_baseline_close_24);
        close2.setOnClickListener(v -> bsb.setState(BottomSheetBehavior.STATE_COLLAPSED));
        sheet.findViewById(R.id.header_more).setVisibility(View.GONE);
        bsb.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            @Override
            public void onStateChanged(@NonNull View sheet, int state) {
                Log.v(TAG, "Song preview sheet state is: " + state);
                if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                    AudioWife.getInstance().release();
                }
            }

            @Override
            public void onSlide(@NonNull View sheet, float offset) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AudioWife.getInstance().release();
        for (Disposable disposable : mDisposables) {
            disposable.dispose();
        }

        mDisposables.clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AudioWife.getInstance().pause();
    }

    public void downloadSelectedSong(final Song song) {
        File songs = new File(getFilesDir(), "songs");
        if (!songs.exists() && !songs.mkdirs()) {
            Log.w(TAG, "Could not create directory at " + songs);
        }

        String extension = song.audio.substring(song.audio.lastIndexOf(".") + 1);
        File audio = new File(songs, song.id + extension);
        if (audio.exists()) {
            playSelection(song, Uri.fromFile(audio));
            return;
        }

        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        WorkRequest request = VideoUtil.createDownloadRequest(song.audio, audio, false);
        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue(request);
        wm.getWorkInfoByIdLiveData(request.getId())
                .observe(this, info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED
                            || info.getState() == WorkInfo.State.SUCCEEDED;
                    if (ended) {
                        progress.dismiss();
                    }

                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        playSelection(song, Uri.fromFile(audio));
                    }
                });
    }

    private void closeWithSelection(@Nullable Song song, Uri file) {
        boolean barpad = getIntent().getBooleanExtra(EXTRA_FROM_BARPAD, false);
        int barpad_id = getIntent().getIntExtra(EXTRA_BARPAD_VALUE, 0);
        Intent data = new Intent();

        if (song != null) {
            data.putExtra(MainActivity.EXTRA_SONG, song);
        }

        if (file != null) {
            data.putExtra(MainActivity.EXTRA_AUDIO, file);
        }

        data.putExtra(MainActivity.EXTRA_BARPAD_VALUE, barpad_id);

        setResult(RESULT_OK, data);
        finish();
    }

    private Uri copySongFile(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        File target = TempUtil.createNewFile(this, "audio");
        OutputStream os = new FileOutputStream(target);
        IOUtils.copy(is, os);
        is.close();
        os.close();
        return Uri.fromFile(target);
    }

    private void playSelection(Song song, Uri file) {
        View sheet = findViewById(R.id.song_preview_sheet);
        AudioWife.getInstance().release();
        AudioWife.getInstance()
                .init(this, file)
                .setPlayView(sheet.findViewById(R.id.play))
                .setPauseView(sheet.findViewById(R.id.pause))
                .setSeekBar(sheet.findViewById(R.id.seekbar))
                .setRuntimeView(sheet.findViewById(R.id.start))
                .setTotalTimeView(sheet.findViewById(R.id.end))
                .play();
        Button button = sheet.findViewById(R.id.header_button);
        button.setBackgroundTintList(getResources().getColorStateList(R.color.emoji_black));
        button.setTextColor(Color.WHITE);
        if (TextUtils.isEmpty(song.details)) {
            button.setVisibility(View.GONE);
        } else {
            button.setOnClickListener(v ->
                    showUrlBrowser(song.details, null, false));
            button.setText(R.string.lease_label);
            button.setVisibility(View.VISIBLE);
        }

        sheet.findViewById(R.id.header_extra).setVisibility(View.GONE);
        sheet.findViewById(R.id.header_more).setVisibility(View.GONE);
        TextView song2 = sheet.findViewById(R.id.song);
        song2.setText(song.title);
        sheet.findViewById(R.id.use)
                .setOnClickListener(v -> closeWithSelection(song, file));
        BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(sheet);
        bsb.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void showUrlBrowser(String url, @Nullable String title, boolean internal) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        if (internal || !getResources().getBoolean(R.bool.external_browser_enabled)) {
            FinestWebView.Builder builder = new FinestWebView.Builder(this);
            if (title != null) {
                builder.titleDefault(title);
            }

            builder.show(url);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    private class SongAdapter extends PagedListAdapter<Song, SongViewHolder> {

        public SongAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            Song song = getItem(position);
            if (TextUtils.isEmpty(song.cover)) {
                holder.icon.setActualImageResource(R.drawable.image_placeholder);
            } else {
                holder.icon.setImageURI(song.cover);
            }

            holder.title.setText(song.title);
            List<String> information = new ArrayList<>();
            if (!TextUtils.isEmpty(song.album)) {
                information.add(song.album);
            }

            if (!TextUtils.isEmpty(song.artist)) {
                information.add(song.artist);
            }

            information.add(song.duration + "s");
            holder.info.setText(StringUtils.join(information, " | "));
            holder.itemView.setOnClickListener(view -> downloadSelectedSong(song));
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(SongPickerActivity.this)
                    .inflate(R.layout.item_song, parent, false);
            return new SongViewHolder(view);
        }
    }

    public static class SongPickerActivityViewModel extends ViewModel {

        public SongPickerActivityViewModel() {
            PagedList.Config config1 = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory1 = new SongDataSource.Factory(null, null);
            state1 = Transformations.switchMap(factory1.source, input -> input.state);
            songs = new LivePagedListBuilder<>(factory1, config1).build();
            PagedList.Config config2 = new PagedList.Config.Builder()
                    .setPageSize(100)
                    .build();
            factory2 = new SongSectionDataSource.Factory();
            state2 = Transformations.switchMap(factory2.source, input -> input.state);
            sections = new LivePagedListBuilder<>(factory2, config2).build();
        }

        public final MutableLiveData<String> searchTerm = new MutableLiveData<>();
        public final LiveData<PagedList<Song>> songs;
        public final SongDataSource.Factory factory1;
        public final SongSectionDataSource.Factory factory2;
        public final LiveData<PagedList<SongSection>> sections;
        public final MutableLiveData<List<Integer>> selection = new MutableLiveData<>(new ArrayList<>());
        public final LiveData<LoadingState> state1;
        public final LiveData<LoadingState> state2;
    }

    private class SongSectionAdapter extends PagedListAdapter<SongSection, SongSectionViewHolder> {

        public SongSectionAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        public void onBindViewHolder(@NonNull SongSectionViewHolder holder, int position) {
            SongSection section = getItem(position);
            holder.chip.setText(section.name);
            List<Integer> now = mModel.selection.getValue();
            holder.chip.setChecked(now != null && now.contains(section.id));
            holder.chip.setOnCheckedChangeListener((v, checked) -> {
                List<Integer> then = mModel.selection.getValue();
                if (checked && !then.contains(section.id)) {
                    then.add(section.id);
                } else if (!checked && then.contains(section.id)) {
                    then.remove((Integer) section.id);
                }

                mModel.selection.postValue(then);
            });
        }

        @NonNull
        @Override
        public SongSectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(SongPickerActivity.this)
                    .inflate(R.layout.item_article_section, parent, false);
            return new SongSectionViewHolder(view);
        }
    }

    private static class SongSectionViewHolder extends RecyclerView.ViewHolder {

        public Chip chip;

        public SongSectionViewHolder(@NonNull View root) {
            super(root);
            chip = root.findViewById(R.id.chip);
            chip.setCheckable(true);
        }
    }

    private static class SongViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView icon;
        public TextView title;
        public TextView info;

        public SongViewHolder(@NonNull View root) {
            super(root);
            icon = root.findViewById(R.id.icon);
            title = root.findViewById(R.id.title);
            info = root.findViewById(R.id.info);
        }
    }
}
