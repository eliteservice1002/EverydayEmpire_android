package app.everydayempire.tv.data.dbs;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import app.everydayempire.tv.data.daos.DraftDao;
import app.everydayempire.tv.data.entities.Draft;

@Database(entities = {Draft.class}, version = 4, exportSchema = false)
public abstract class ClientDatabase extends RoomDatabase {

    public abstract DraftDao drafts();
}
