package app.everydayempire.tv.data;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;
import app.everydayempire.tv.MainApplication;
import app.everydayempire.tv.common.LoadingState;
import app.everydayempire.tv.data.api.REST;
import app.everydayempire.tv.data.models.Barpad;
import app.everydayempire.tv.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BarpadDataSource extends PageKeyedDataSource<Integer, Barpad> {

    private static final String TAG = "BarpadDataSource";
    public static final String EXTRA_USER = "user";
    private final Bundle mParams;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public BarpadDataSource(@NonNull Bundle params) {
        mParams = params;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Barpad> callback) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        int user = mParams.getInt(EXTRA_USER);
        rest.barpadsIndex(user, 1)
                .enqueue(new Callback<Wrappers.Paginated<Barpad>>() {
                    @Override
                    public void onResponse(@Nullable Call<Wrappers.Paginated<Barpad>> call, @Nullable Response<Wrappers.Paginated<Barpad>> response) {
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Barpad> barpads = response.body();
                            //noinspection ConstantConditions

                            callback.onResult(barpads.data, null, 2);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(Call<Wrappers.Paginated<Barpad>> call, Throwable t) {
                        Log.e(TAG, "Fetching Barpads has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, Barpad> callback
    ) {
    }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Barpad> callback) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        int user = mParams.getInt(EXTRA_USER);
        rest.barpadsIndex(user, params.key)
                .enqueue(new Callback<Wrappers.Paginated<Barpad>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Barpad>> call,
                            @Nullable Response<Wrappers.Paginated<Barpad>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Barpad> barpads = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(barpads.data,params.key + 1);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Barpad>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching Barpads has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, Barpad> {

        @NonNull public Bundle params;

        public MutableLiveData<BarpadDataSource> source = new MutableLiveData<>();

        public Factory(@NonNull Bundle params) {
            this.params = params;
        }

        @NonNull
        @Override
        public DataSource<Integer, Barpad> create() {
            BarpadDataSource source = new BarpadDataSource(params);
            this.source.postValue(source);
            return source;
        }
    }
}
