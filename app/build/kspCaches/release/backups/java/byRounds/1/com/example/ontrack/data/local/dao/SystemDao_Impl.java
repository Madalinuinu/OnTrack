package com.example.ontrack.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.ontrack.data.local.entity.SystemEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SystemDao_Impl implements SystemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SystemEntity> __insertionAdapterOfSystemEntity;

  private final EntityDeletionOrUpdateAdapter<SystemEntity> __updateAdapterOfSystemEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateStreak;

  private final SharedSQLiteStatement __preparedStmtOfUpdateFreezeMonth;

  private final SharedSQLiteStatement __preparedStmtOfResetStreak;

  public SystemDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSystemEntity = new EntityInsertionAdapter<SystemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `systems` (`id`,`name`,`goal`,`duration`,`startDate`,`sortOrder`,`currentStreak`,`lastStreakDate`,`pausedFromEpochDay`,`pausedToEpochDay`,`freezeMonthKey`,`freezeDaysUsedThisMonth`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SystemEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getGoal());
        if (entity.getDuration() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getDuration());
        }
        statement.bindLong(5, entity.getStartDate());
        statement.bindLong(6, entity.getSortOrder());
        statement.bindLong(7, entity.getCurrentStreak());
        statement.bindLong(8, entity.getLastStreakDate());
        if (entity.getPausedFromEpochDay() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getPausedFromEpochDay());
        }
        if (entity.getPausedToEpochDay() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getPausedToEpochDay());
        }
        statement.bindLong(11, entity.getFreezeMonthKey());
        statement.bindLong(12, entity.getFreezeDaysUsedThisMonth());
      }
    };
    this.__updateAdapterOfSystemEntity = new EntityDeletionOrUpdateAdapter<SystemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `systems` SET `id` = ?,`name` = ?,`goal` = ?,`duration` = ?,`startDate` = ?,`sortOrder` = ?,`currentStreak` = ?,`lastStreakDate` = ?,`pausedFromEpochDay` = ?,`pausedToEpochDay` = ?,`freezeMonthKey` = ?,`freezeDaysUsedThisMonth` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SystemEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getGoal());
        if (entity.getDuration() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getDuration());
        }
        statement.bindLong(5, entity.getStartDate());
        statement.bindLong(6, entity.getSortOrder());
        statement.bindLong(7, entity.getCurrentStreak());
        statement.bindLong(8, entity.getLastStreakDate());
        if (entity.getPausedFromEpochDay() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getPausedFromEpochDay());
        }
        if (entity.getPausedToEpochDay() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getPausedToEpochDay());
        }
        statement.bindLong(11, entity.getFreezeMonthKey());
        statement.bindLong(12, entity.getFreezeDaysUsedThisMonth());
        statement.bindLong(13, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM systems WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateStreak = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE systems SET currentStreak = ?, lastStreakDate = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateFreezeMonth = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE systems SET freezeMonthKey = ?, freezeDaysUsedThisMonth = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfResetStreak = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE systems SET currentStreak = 0, lastStreakDate = -1, freezeMonthKey = 0, freezeDaysUsedThisMonth = 0 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertSystem(final SystemEntity system,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSystemEntity.insertAndReturnId(system);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSystem(final SystemEntity system,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSystemEntity.handle(system);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateStreak(final long systemId, final int streak, final long lastDate,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateStreak.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, streak);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, lastDate);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, systemId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateStreak.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateFreezeMonth(final long systemId, final int monthKey, final int daysUsed,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateFreezeMonth.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, monthKey);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, daysUsed);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, systemId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateFreezeMonth.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object resetStreak(final long systemId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfResetStreak.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, systemId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfResetStreak.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SystemEntity>> getAllSystems() {
    final String _sql = "SELECT * FROM systems ORDER BY sortOrder ASC, startDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"systems"}, new Callable<List<SystemEntity>>() {
      @Override
      @NonNull
      public List<SystemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfCurrentStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "currentStreak");
          final int _cursorIndexOfLastStreakDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastStreakDate");
          final int _cursorIndexOfPausedFromEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedFromEpochDay");
          final int _cursorIndexOfPausedToEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedToEpochDay");
          final int _cursorIndexOfFreezeMonthKey = CursorUtil.getColumnIndexOrThrow(_cursor, "freezeMonthKey");
          final int _cursorIndexOfFreezeDaysUsedThisMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "freezeDaysUsedThisMonth");
          final List<SystemEntity> _result = new ArrayList<SystemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SystemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final Integer _tmpDuration;
            if (_cursor.isNull(_cursorIndexOfDuration)) {
              _tmpDuration = null;
            } else {
              _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            }
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final int _tmpCurrentStreak;
            _tmpCurrentStreak = _cursor.getInt(_cursorIndexOfCurrentStreak);
            final long _tmpLastStreakDate;
            _tmpLastStreakDate = _cursor.getLong(_cursorIndexOfLastStreakDate);
            final Long _tmpPausedFromEpochDay;
            if (_cursor.isNull(_cursorIndexOfPausedFromEpochDay)) {
              _tmpPausedFromEpochDay = null;
            } else {
              _tmpPausedFromEpochDay = _cursor.getLong(_cursorIndexOfPausedFromEpochDay);
            }
            final Long _tmpPausedToEpochDay;
            if (_cursor.isNull(_cursorIndexOfPausedToEpochDay)) {
              _tmpPausedToEpochDay = null;
            } else {
              _tmpPausedToEpochDay = _cursor.getLong(_cursorIndexOfPausedToEpochDay);
            }
            final int _tmpFreezeMonthKey;
            _tmpFreezeMonthKey = _cursor.getInt(_cursorIndexOfFreezeMonthKey);
            final int _tmpFreezeDaysUsedThisMonth;
            _tmpFreezeDaysUsedThisMonth = _cursor.getInt(_cursorIndexOfFreezeDaysUsedThisMonth);
            _item = new SystemEntity(_tmpId,_tmpName,_tmpGoal,_tmpDuration,_tmpStartDate,_tmpSortOrder,_tmpCurrentStreak,_tmpLastStreakDate,_tmpPausedFromEpochDay,_tmpPausedToEpochDay,_tmpFreezeMonthKey,_tmpFreezeDaysUsedThisMonth);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSystemById(final long id, final Continuation<? super SystemEntity> $completion) {
    final String _sql = "SELECT * FROM systems WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SystemEntity>() {
      @Override
      @Nullable
      public SystemEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfCurrentStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "currentStreak");
          final int _cursorIndexOfLastStreakDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastStreakDate");
          final int _cursorIndexOfPausedFromEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedFromEpochDay");
          final int _cursorIndexOfPausedToEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedToEpochDay");
          final int _cursorIndexOfFreezeMonthKey = CursorUtil.getColumnIndexOrThrow(_cursor, "freezeMonthKey");
          final int _cursorIndexOfFreezeDaysUsedThisMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "freezeDaysUsedThisMonth");
          final SystemEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final Integer _tmpDuration;
            if (_cursor.isNull(_cursorIndexOfDuration)) {
              _tmpDuration = null;
            } else {
              _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            }
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final int _tmpCurrentStreak;
            _tmpCurrentStreak = _cursor.getInt(_cursorIndexOfCurrentStreak);
            final long _tmpLastStreakDate;
            _tmpLastStreakDate = _cursor.getLong(_cursorIndexOfLastStreakDate);
            final Long _tmpPausedFromEpochDay;
            if (_cursor.isNull(_cursorIndexOfPausedFromEpochDay)) {
              _tmpPausedFromEpochDay = null;
            } else {
              _tmpPausedFromEpochDay = _cursor.getLong(_cursorIndexOfPausedFromEpochDay);
            }
            final Long _tmpPausedToEpochDay;
            if (_cursor.isNull(_cursorIndexOfPausedToEpochDay)) {
              _tmpPausedToEpochDay = null;
            } else {
              _tmpPausedToEpochDay = _cursor.getLong(_cursorIndexOfPausedToEpochDay);
            }
            final int _tmpFreezeMonthKey;
            _tmpFreezeMonthKey = _cursor.getInt(_cursorIndexOfFreezeMonthKey);
            final int _tmpFreezeDaysUsedThisMonth;
            _tmpFreezeDaysUsedThisMonth = _cursor.getInt(_cursorIndexOfFreezeDaysUsedThisMonth);
            _result = new SystemEntity(_tmpId,_tmpName,_tmpGoal,_tmpDuration,_tmpStartDate,_tmpSortOrder,_tmpCurrentStreak,_tmpLastStreakDate,_tmpPausedFromEpochDay,_tmpPausedToEpochDay,_tmpFreezeMonthKey,_tmpFreezeDaysUsedThisMonth);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<SystemEntity> getSystemByIdFlow(final long id) {
    final String _sql = "SELECT * FROM systems WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"systems"}, new Callable<SystemEntity>() {
      @Override
      @Nullable
      public SystemEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfCurrentStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "currentStreak");
          final int _cursorIndexOfLastStreakDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastStreakDate");
          final int _cursorIndexOfPausedFromEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedFromEpochDay");
          final int _cursorIndexOfPausedToEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedToEpochDay");
          final int _cursorIndexOfFreezeMonthKey = CursorUtil.getColumnIndexOrThrow(_cursor, "freezeMonthKey");
          final int _cursorIndexOfFreezeDaysUsedThisMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "freezeDaysUsedThisMonth");
          final SystemEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final Integer _tmpDuration;
            if (_cursor.isNull(_cursorIndexOfDuration)) {
              _tmpDuration = null;
            } else {
              _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            }
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final int _tmpCurrentStreak;
            _tmpCurrentStreak = _cursor.getInt(_cursorIndexOfCurrentStreak);
            final long _tmpLastStreakDate;
            _tmpLastStreakDate = _cursor.getLong(_cursorIndexOfLastStreakDate);
            final Long _tmpPausedFromEpochDay;
            if (_cursor.isNull(_cursorIndexOfPausedFromEpochDay)) {
              _tmpPausedFromEpochDay = null;
            } else {
              _tmpPausedFromEpochDay = _cursor.getLong(_cursorIndexOfPausedFromEpochDay);
            }
            final Long _tmpPausedToEpochDay;
            if (_cursor.isNull(_cursorIndexOfPausedToEpochDay)) {
              _tmpPausedToEpochDay = null;
            } else {
              _tmpPausedToEpochDay = _cursor.getLong(_cursorIndexOfPausedToEpochDay);
            }
            final int _tmpFreezeMonthKey;
            _tmpFreezeMonthKey = _cursor.getInt(_cursorIndexOfFreezeMonthKey);
            final int _tmpFreezeDaysUsedThisMonth;
            _tmpFreezeDaysUsedThisMonth = _cursor.getInt(_cursorIndexOfFreezeDaysUsedThisMonth);
            _result = new SystemEntity(_tmpId,_tmpName,_tmpGoal,_tmpDuration,_tmpStartDate,_tmpSortOrder,_tmpCurrentStreak,_tmpLastStreakDate,_tmpPausedFromEpochDay,_tmpPausedToEpochDay,_tmpFreezeMonthKey,_tmpFreezeDaysUsedThisMonth);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object nextSortOrder(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM systems";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
