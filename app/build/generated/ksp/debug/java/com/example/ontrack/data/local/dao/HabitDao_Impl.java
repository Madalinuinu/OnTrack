package com.example.ontrack.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.ontrack.data.local.entity.FrequencyType;
import com.example.ontrack.data.local.entity.FrequencyTypeConverter;
import com.example.ontrack.data.local.entity.HabitEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class HabitDao_Impl implements HabitDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HabitEntity> __insertionAdapterOfHabitEntity;

  private final FrequencyTypeConverter __frequencyTypeConverter = new FrequencyTypeConverter();

  public HabitDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHabitEntity = new EntityInsertionAdapter<HabitEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `habits` (`id`,`systemId`,`title`,`frequencyType`,`targetCount`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HabitEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getSystemId());
        statement.bindString(3, entity.getTitle());
        final String _tmp = __frequencyTypeConverter.toStorage(entity.getFrequencyType());
        statement.bindString(4, _tmp);
        statement.bindLong(5, entity.getTargetCount());
      }
    };
  }

  @Override
  public Object insertHabits(final List<HabitEntity> habits,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHabitEntity.insert(habits);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HabitEntity>> getHabitsForSystem(final long systemId) {
    final String _sql = "SELECT * FROM habits WHERE systemId = ? ORDER BY id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, systemId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habits"}, new Callable<List<HabitEntity>>() {
      @Override
      @NonNull
      public List<HabitEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSystemId = CursorUtil.getColumnIndexOrThrow(_cursor, "systemId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfFrequencyType = CursorUtil.getColumnIndexOrThrow(_cursor, "frequencyType");
          final int _cursorIndexOfTargetCount = CursorUtil.getColumnIndexOrThrow(_cursor, "targetCount");
          final List<HabitEntity> _result = new ArrayList<HabitEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpSystemId;
            _tmpSystemId = _cursor.getLong(_cursorIndexOfSystemId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final FrequencyType _tmpFrequencyType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfFrequencyType);
            _tmpFrequencyType = __frequencyTypeConverter.fromString(_tmp);
            final int _tmpTargetCount;
            _tmpTargetCount = _cursor.getInt(_cursorIndexOfTargetCount);
            _item = new HabitEntity(_tmpId,_tmpSystemId,_tmpTitle,_tmpFrequencyType,_tmpTargetCount);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
