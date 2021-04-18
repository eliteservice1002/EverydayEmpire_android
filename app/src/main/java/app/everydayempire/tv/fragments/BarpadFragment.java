package app.everydayempire.tv.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.everydayempire.tv.MainApplication;
import app.everydayempire.tv.R;
import app.everydayempire.tv.SharedConstants;
import app.everydayempire.tv.activities.MainActivity;
import app.everydayempire.tv.ads.BannerAdProvider;
import app.everydayempire.tv.common.DiffUtilCallback;
import app.everydayempire.tv.common.LoadingState;
import app.everydayempire.tv.data.BarpadDataSource;
import app.everydayempire.tv.data.api.REST;
import app.everydayempire.tv.data.models.Advertisement;
import app.everydayempire.tv.data.models.Barpad;
import app.everydayempire.tv.data.models.Clip;
import app.everydayempire.tv.data.models.User;
import app.everydayempire.tv.data.models.Wrappers;
import app.everydayempire.tv.utils.AdsUtil;
import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BarpadFragment extends Fragment {

    private BarpadFragment.BarpadFragmentViewModel mModel;
    private MainActivity.MainActivityViewModel mModel1;

    private static final String TAG = "BarpadFragment";
    private static final String ARG_PARAMS = "params";
    public static final String EXTRA_USER = "user";
    public User mUser;
    private Bundle mParams;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Bundle arguments = getArguments();
        if (arguments != null) {
            mParams = arguments.getBundle(ARG_PARAMS);
        }

        if (mParams == null) {
            mParams = new Bundle();
        }

        User user = requireActivity().getIntent().getParcelableExtra(EXTRA_USER);
        if (user != null) {
            mUser = user;
            mParams.putInt(EXTRA_USER, mUser.id);
        }
        else  {
            mModel1 = new ViewModelProvider(requireActivity())
                    .get(MainActivity.MainActivityViewModel.class);
            mUser = mModel1.user.getValue();
            mParams.putInt(EXTRA_USER, mUser.id);
        }

        BarpadFragment.BarpadFragmentViewModel.Factory factory =
                new BarpadFragment.BarpadFragmentViewModel.Factory(mParams);
        mModel = new ViewModelProvider(this, factory).get(BarpadFragment.BarpadFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_barpad, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton back = view.findViewById(R.id.header_back);
        back.setImageResource(R.drawable.ic_back);
        back.setOnClickListener(c -> ((MainActivity)requireActivity()).popBackStack());
        back.setVisibility(View.VISIBLE);

        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.barpad_label);

        view.findViewById(R.id.header_more).setVisibility(View.GONE);

        Button button = view.findViewById(R.id.header_button);
        button.setOnClickListener(c -> ((MainActivity)requireActivity()).showEditBarpadPage(0));
        button.setText(R.string.barpad_new_label);
        button.setVisibility(View.VISIBLE);

        RecyclerView barpads = view.findViewById(R.id.barpad_list);
        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, true);
        lm.setStackFromEnd(true);
        barpads.setLayoutManager(lm);
        BarpadFragment.BarpadAdapter adapter = new BarpadFragment.BarpadAdapter();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int last = lm.findLastCompletelyVisibleItemPosition();
                if (last == -1 || positionStart >= adapter.getItemCount() - 1 && last == positionStart - 1) {
                    barpads.scrollToPosition(positionStart);
                }
            }
        });
        barpads.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        barpads.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        OverScrollDecoratorHelper.setUpOverScroll(
                barpads, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        mModel.barpads.observe(getViewLifecycleOwner(), adapter::submitList);

        View empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);

        mModel.state.observe(getViewLifecycleOwner(), state -> {
            List<?> list = mModel.barpads.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                barpads.setVisibility(list == null || list.isEmpty() ? View.GONE : View.VISIBLE);
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }


    public static BarpadFragment newInstance(@Nullable Bundle params) {
        BarpadFragment fragment = new BarpadFragment();
        Bundle arguments = new Bundle();
        arguments.putBundle(ARG_PARAMS, params);
        fragment.setArguments(arguments);

        return fragment;
    }


    private class BarpadAdapter extends PagedListAdapter<Barpad, BarpadFragment.BarpadViewHolder> {

        protected BarpadAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        public void onBindViewHolder(@NonNull BarpadFragment.BarpadViewHolder holder, int position) {
            Barpad barpad = getItem(position);
            //noinspection ConstantConditions
            holder.photo.setImageResource(R.drawable.barpad_edit);
            holder.name.setText(barpad.name);
            holder.action.setText(R.string.barpad_action_button_label);
            holder.action.setOnClickListener(c->((MainActivity)requireActivity()).showEditBarpadPage(barpad.id));
        }

        @NonNull
        @Override
        public BarpadFragment.BarpadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_barpad, parent, false);
            return new BarpadFragment.BarpadViewHolder(view);
        }
    }

    public static class BarpadFragmentViewModel extends ViewModel {

        public BarpadFragmentViewModel(@NonNull Bundle params) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new BarpadDataSource.Factory(params);
            state = Transformations.switchMap(factory.source, input -> input.state);
            barpads = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<Barpad>> barpads;
        public final BarpadDataSource.Factory factory;
        public final LiveData<LoadingState> state;

        private static class Factory implements ViewModelProvider.Factory {

            @NonNull private final Bundle mParams;

            public Factory(@NonNull Bundle params) {
                mParams = params;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new BarpadFragment.BarpadFragmentViewModel(mParams);
            }
        }
    }

    private static class BarpadViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView photo;
        public TextView name;
        public Button action;

        public BarpadViewHolder(@NonNull View root) {
            super(root);
            photo = root.findViewById(R.id.photo);
            name = root.findViewById(R.id.name);
            action = root.findViewById(R.id.barpad_open_button);
        }
    }
}
