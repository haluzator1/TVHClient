package org.tvheadend.tvhclient.domain.repository.data_source;

import androidx.lifecycle.LiveData;
import android.os.AsyncTask;
import androidx.annotation.NonNull;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.domain.entity.SeriesRecording;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import timber.log.Timber;

public class SeriesRecordingData implements DataSourceInterface<SeriesRecording> {

    private final AppRoomDatabase db;

    public SeriesRecordingData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(SeriesRecording item) {
        AsyncTask.execute(() -> db.getSeriesRecordingDao().insert(item));
    }

    @Override
    public void updateItem(SeriesRecording item) {
        AsyncTask.execute(() -> db.getSeriesRecordingDao().update(item));
    }

    @Override
    public void removeItem(SeriesRecording item) {
        AsyncTask.execute(() -> db.getSeriesRecordingDao().delete(item));
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return db.getSeriesRecordingDao().getRecordingCount();
    }

    public LiveData<List<SeriesRecording>> getLiveDataItems() {
        return db.getSeriesRecordingDao().loadAllRecordings();
    }

    @Override
    public LiveData<SeriesRecording> getLiveDataItemById(@NonNull Object id) {
        return db.getSeriesRecordingDao().loadRecordingById((String) id);
    }

    @Override
    public SeriesRecording getItemById(@NonNull Object id) {
        try {
            return new SeriesRecordingByIdTask(db, (String) id).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading series recording by id task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading series recording by id task aborted", e);
        }
        return null;
    }

    @Override
    @NonNull
    public List<SeriesRecording> getItems() {
        return new ArrayList<>();
    }

    private static class SeriesRecordingByIdTask extends AsyncTask<Void, Void, SeriesRecording> {
        private final AppRoomDatabase db;
        private final String id;

        SeriesRecordingByIdTask(AppRoomDatabase db, String id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected SeriesRecording doInBackground(Void... voids) {
            return db.getSeriesRecordingDao().loadRecordingByIdSync(id);
        }
    }
}
