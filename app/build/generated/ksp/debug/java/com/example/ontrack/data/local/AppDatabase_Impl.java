package com.example.ontrack.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.example.ontrack.data.local.dao.HabitDao;
import com.example.ontrack.data.local.dao.HabitDao_Impl;
import com.example.ontrack.data.local.dao.HabitLogDao;
import com.example.ontrack.data.local.dao.HabitLogDao_Impl;
import com.example.ontrack.data.local.dao.SystemDao;
import com.example.ontrack.data.local.dao.SystemDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile SystemDao _systemDao;

  private volatile HabitDao _habitDao;

  private volatile HabitLogDao _habitLogDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `systems` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `goal` TEXT NOT NULL, `duration` INTEGER, `startDate` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `systemId` INTEGER NOT NULL, `title` TEXT NOT NULL, `frequencyType` TEXT NOT NULL, `targetCount` INTEGER NOT NULL, FOREIGN KEY(`systemId`) REFERENCES `systems`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_systemId` ON `habits` (`systemId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `habit_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `habitId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_logs_habitId` ON `habit_logs` (`habitId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_logs_date` ON `habit_logs` (`date`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_logs_habitId_date` ON `habit_logs` (`habitId`, `date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'cd335b1da5b3a39d4e99f5adbe2d877b')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `systems`");
        db.execSQL("DROP TABLE IF EXISTS `habits`");
        db.execSQL("DROP TABLE IF EXISTS `habit_logs`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsSystems = new HashMap<String, TableInfo.Column>(5);
        _columnsSystems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSystems.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSystems.put("goal", new TableInfo.Column("goal", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSystems.put("duration", new TableInfo.Column("duration", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSystems.put("startDate", new TableInfo.Column("startDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSystems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSystems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSystems = new TableInfo("systems", _columnsSystems, _foreignKeysSystems, _indicesSystems);
        final TableInfo _existingSystems = TableInfo.read(db, "systems");
        if (!_infoSystems.equals(_existingSystems)) {
          return new RoomOpenHelper.ValidationResult(false, "systems(com.example.ontrack.data.local.entity.SystemEntity).\n"
                  + " Expected:\n" + _infoSystems + "\n"
                  + " Found:\n" + _existingSystems);
        }
        final HashMap<String, TableInfo.Column> _columnsHabits = new HashMap<String, TableInfo.Column>(5);
        _columnsHabits.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("systemId", new TableInfo.Column("systemId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("frequencyType", new TableInfo.Column("frequencyType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("targetCount", new TableInfo.Column("targetCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHabits = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysHabits.add(new TableInfo.ForeignKey("systems", "CASCADE", "NO ACTION", Arrays.asList("systemId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesHabits = new HashSet<TableInfo.Index>(1);
        _indicesHabits.add(new TableInfo.Index("index_habits_systemId", false, Arrays.asList("systemId"), Arrays.asList("ASC")));
        final TableInfo _infoHabits = new TableInfo("habits", _columnsHabits, _foreignKeysHabits, _indicesHabits);
        final TableInfo _existingHabits = TableInfo.read(db, "habits");
        if (!_infoHabits.equals(_existingHabits)) {
          return new RoomOpenHelper.ValidationResult(false, "habits(com.example.ontrack.data.local.entity.HabitEntity).\n"
                  + " Expected:\n" + _infoHabits + "\n"
                  + " Found:\n" + _existingHabits);
        }
        final HashMap<String, TableInfo.Column> _columnsHabitLogs = new HashMap<String, TableInfo.Column>(4);
        _columnsHabitLogs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitLogs.put("habitId", new TableInfo.Column("habitId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitLogs.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitLogs.put("isCompleted", new TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHabitLogs = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysHabitLogs.add(new TableInfo.ForeignKey("habits", "CASCADE", "NO ACTION", Arrays.asList("habitId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesHabitLogs = new HashSet<TableInfo.Index>(3);
        _indicesHabitLogs.add(new TableInfo.Index("index_habit_logs_habitId", false, Arrays.asList("habitId"), Arrays.asList("ASC")));
        _indicesHabitLogs.add(new TableInfo.Index("index_habit_logs_date", false, Arrays.asList("date"), Arrays.asList("ASC")));
        _indicesHabitLogs.add(new TableInfo.Index("index_habit_logs_habitId_date", true, Arrays.asList("habitId", "date"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoHabitLogs = new TableInfo("habit_logs", _columnsHabitLogs, _foreignKeysHabitLogs, _indicesHabitLogs);
        final TableInfo _existingHabitLogs = TableInfo.read(db, "habit_logs");
        if (!_infoHabitLogs.equals(_existingHabitLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "habit_logs(com.example.ontrack.data.local.entity.HabitLogEntity).\n"
                  + " Expected:\n" + _infoHabitLogs + "\n"
                  + " Found:\n" + _existingHabitLogs);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "cd335b1da5b3a39d4e99f5adbe2d877b", "8ffb7af30b7ed2c461d0cb4c4ae6af83");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "systems","habits","habit_logs");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `systems`");
      _db.execSQL("DELETE FROM `habits`");
      _db.execSQL("DELETE FROM `habit_logs`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(SystemDao.class, SystemDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HabitDao.class, HabitDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HabitLogDao.class, HabitLogDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public SystemDao systemDao() {
    if (_systemDao != null) {
      return _systemDao;
    } else {
      synchronized(this) {
        if(_systemDao == null) {
          _systemDao = new SystemDao_Impl(this);
        }
        return _systemDao;
      }
    }
  }

  @Override
  public HabitDao habitDao() {
    if (_habitDao != null) {
      return _habitDao;
    } else {
      synchronized(this) {
        if(_habitDao == null) {
          _habitDao = new HabitDao_Impl(this);
        }
        return _habitDao;
      }
    }
  }

  @Override
  public HabitLogDao habitLogDao() {
    if (_habitLogDao != null) {
      return _habitLogDao;
    } else {
      synchronized(this) {
        if(_habitLogDao == null) {
          _habitLogDao = new HabitLogDao_Impl(this);
        }
        return _habitLogDao;
      }
    }
  }
}
