package org.tvheadend.tvhclient.domain.repository.data_source;

import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.domain.entity.ChannelTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import timber.log.Timber;

public class ChannelTagData implements DataSourceInterface<ChannelTag> {

    private final AppRoomDatabase db;

    public ChannelTagData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(ChannelTag item) {
        AsyncTask.execute(() -> db.getChannelTagDao().insert(item));
    }

    public void addItems(List<ChannelTag> items) {
        AsyncTask.execute(() -> db.getChannelTagDao().insert(new ArrayList<>(items)));
    }

    @Override
    public void updateItem(ChannelTag item) {
        AsyncTask.execute(() -> db.getChannelTagDao().update(item));
    }

    @Override
    public void removeItem(ChannelTag item) {
        AsyncTask.execute(() -> db.getChannelTagDao().delete(item));
    }

    public void updateSelectedChannelTags(Set<Integer> ids) {
        AsyncTask.execute(() -> {
            List<ChannelTag> channelTags = db.getChannelTagDao().loadAllChannelTagsSync();
            for (ChannelTag channelTag : channelTags) {
                channelTag.setSelected(false);
                if (ids.contains(channelTag.getTagId())) {
                    channelTag.setSelected(true);
                }
            }
            db.getChannelTagDao().update(channelTags);
        });
    }


    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return null;
    }

    @Override
    public LiveData<List<ChannelTag>> getLiveDataItems() {
        return db.getChannelTagDao().loadAllChannelTags();
    }

    @Override
    @Nullable
    public LiveData<ChannelTag> getLiveDataItemById(@NonNull Object id) {
        return null;
    }

    @Override
    @Nullable
    public ChannelTag getItemById(@NonNull Object id) {
        try {
            return new ChannelTagByIdTask(db, (int) id).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading channel tag by id task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading channel tag by id task aborted", e);
        }
        return null;
    }

    @Override
    @NonNull
    public List<ChannelTag> getItems() {
        List<ChannelTag> channelTags = new ArrayList<>();
        try {
            channelTags = new ChannelTagListTask(db).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading all channel tags task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading all channel tags task aborted", e);
        }
        return channelTags;
    }

    public LiveData<List<Integer>> getLiveDataSelectedItemIds() {
        return db.getChannelTagDao().loadAllSelectedItemIds();
    }

    public int getItemCount() {
        try {
            return new ChannelTagCountTask(db).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading channel tag count task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading channel tag count task aborted", e);
        }
        return 0;
    }

    private static class ChannelTagListTask extends AsyncTask<Void, Void, List<ChannelTag>> {
        private final AppRoomDatabase db;

        ChannelTagListTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected List<ChannelTag> doInBackground(Void... voids) {
            return db.getChannelTagDao().loadAllChannelTagsSync();
        }
    }

    private static class ChannelTagCountTask extends AsyncTask<Void, Void, Integer> {
        private final AppRoomDatabase db;

        ChannelTagCountTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            return db.getChannelTagDao().getItemCountSync();
        }
    }

    private static class ChannelTagByIdTask extends AsyncTask<Void, Void, ChannelTag> {
        private final AppRoomDatabase db;
        private final int id;

        ChannelTagByIdTask(AppRoomDatabase db, int id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected ChannelTag doInBackground(Void... voids) {
            return db.getChannelTagDao().loadChannelTagByIdSync(id);
        }
    }
}
