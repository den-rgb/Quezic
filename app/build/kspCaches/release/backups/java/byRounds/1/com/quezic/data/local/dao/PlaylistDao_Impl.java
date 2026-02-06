package com.quezic.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.quezic.data.local.entity.PlaylistEntity;
import com.quezic.data.local.entity.PlaylistSongCrossRef;
import com.quezic.data.local.entity.PlaylistWithSongs;
import com.quezic.data.local.entity.SongEntity;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
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
public final class PlaylistDao_Impl implements PlaylistDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PlaylistEntity> __insertionAdapterOfPlaylistEntity;

  private final EntityInsertionAdapter<PlaylistSongCrossRef> __insertionAdapterOfPlaylistSongCrossRef;

  private final EntityDeletionOrUpdateAdapter<PlaylistEntity> __deletionAdapterOfPlaylistEntity;

  private final EntityDeletionOrUpdateAdapter<PlaylistEntity> __updateAdapterOfPlaylistEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdatePlaylistTimestamp;

  private final SharedSQLiteStatement __preparedStmtOfDeletePlaylistById;

  private final SharedSQLiteStatement __preparedStmtOfRemoveFromPlaylist;

  private final SharedSQLiteStatement __preparedStmtOfClearPlaylist;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSongPosition;

  public PlaylistDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPlaylistEntity = new EntityInsertionAdapter<PlaylistEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `playlists` (`id`,`name`,`description`,`coverUrl`,`createdAt`,`updatedAt`,`isSmartPlaylist`,`smartPlaylistType`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlaylistEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        if (entity.getCoverUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getCoverUrl());
        }
        statement.bindLong(5, entity.getCreatedAt());
        statement.bindLong(6, entity.getUpdatedAt());
        final int _tmp = entity.isSmartPlaylist() ? 1 : 0;
        statement.bindLong(7, _tmp);
        if (entity.getSmartPlaylistType() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getSmartPlaylistType());
        }
      }
    };
    this.__insertionAdapterOfPlaylistSongCrossRef = new EntityInsertionAdapter<PlaylistSongCrossRef>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `playlist_songs` (`playlistId`,`songId`,`position`,`addedAt`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlaylistSongCrossRef entity) {
        statement.bindLong(1, entity.getPlaylistId());
        statement.bindString(2, entity.getSongId());
        statement.bindLong(3, entity.getPosition());
        statement.bindLong(4, entity.getAddedAt());
      }
    };
    this.__deletionAdapterOfPlaylistEntity = new EntityDeletionOrUpdateAdapter<PlaylistEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `playlists` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlaylistEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfPlaylistEntity = new EntityDeletionOrUpdateAdapter<PlaylistEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `playlists` SET `id` = ?,`name` = ?,`description` = ?,`coverUrl` = ?,`createdAt` = ?,`updatedAt` = ?,`isSmartPlaylist` = ?,`smartPlaylistType` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlaylistEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        if (entity.getCoverUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getCoverUrl());
        }
        statement.bindLong(5, entity.getCreatedAt());
        statement.bindLong(6, entity.getUpdatedAt());
        final int _tmp = entity.isSmartPlaylist() ? 1 : 0;
        statement.bindLong(7, _tmp);
        if (entity.getSmartPlaylistType() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getSmartPlaylistType());
        }
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfUpdatePlaylistTimestamp = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE playlists SET updatedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeletePlaylistById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM playlists WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRemoveFromPlaylist = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM playlist_songs WHERE playlistId = ? AND songId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearPlaylist = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM playlist_songs WHERE playlistId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateSongPosition = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE playlist_songs SET position = ? WHERE playlistId = ? AND songId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertPlaylist(final PlaylistEntity playlist,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfPlaylistEntity.insertAndReturnId(playlist);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertPlaylistSong(final PlaylistSongCrossRef crossRef,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlaylistSongCrossRef.insert(crossRef);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePlaylist(final PlaylistEntity playlist,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPlaylistEntity.handle(playlist);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePlaylist(final PlaylistEntity playlist,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPlaylistEntity.handle(playlist);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePlaylistTimestamp(final long playlistId, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdatePlaylistTimestamp.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, playlistId);
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
          __preparedStmtOfUpdatePlaylistTimestamp.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePlaylistById(final long playlistId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePlaylistById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
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
          __preparedStmtOfDeletePlaylistById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object removeFromPlaylist(final long playlistId, final String songId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRemoveFromPlaylist.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, songId);
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
          __preparedStmtOfRemoveFromPlaylist.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearPlaylist(final long playlistId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearPlaylist.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
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
          __preparedStmtOfClearPlaylist.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSongPosition(final long playlistId, final String songId,
      final int newPosition, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSongPosition.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, newPosition);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, playlistId);
        _argIndex = 3;
        _stmt.bindString(_argIndex, songId);
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
          __preparedStmtOfUpdateSongPosition.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PlaylistEntity>> getAllPlaylists() {
    final String _sql = "SELECT * FROM playlists WHERE isSmartPlaylist = 0 ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"playlists"}, new Callable<List<PlaylistEntity>>() {
      @Override
      @NonNull
      public List<PlaylistEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "coverUrl");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsSmartPlaylist = CursorUtil.getColumnIndexOrThrow(_cursor, "isSmartPlaylist");
          final int _cursorIndexOfSmartPlaylistType = CursorUtil.getColumnIndexOrThrow(_cursor, "smartPlaylistType");
          final List<PlaylistEntity> _result = new ArrayList<PlaylistEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlaylistEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverUrl;
            if (_cursor.isNull(_cursorIndexOfCoverUrl)) {
              _tmpCoverUrl = null;
            } else {
              _tmpCoverUrl = _cursor.getString(_cursorIndexOfCoverUrl);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsSmartPlaylist;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSmartPlaylist);
            _tmpIsSmartPlaylist = _tmp != 0;
            final String _tmpSmartPlaylistType;
            if (_cursor.isNull(_cursorIndexOfSmartPlaylistType)) {
              _tmpSmartPlaylistType = null;
            } else {
              _tmpSmartPlaylistType = _cursor.getString(_cursorIndexOfSmartPlaylistType);
            }
            _item = new PlaylistEntity(_tmpId,_tmpName,_tmpDescription,_tmpCoverUrl,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsSmartPlaylist,_tmpSmartPlaylistType);
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
  public Object getPlaylistById(final long id,
      final Continuation<? super PlaylistEntity> $completion) {
    final String _sql = "SELECT * FROM playlists WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PlaylistEntity>() {
      @Override
      @Nullable
      public PlaylistEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "coverUrl");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsSmartPlaylist = CursorUtil.getColumnIndexOrThrow(_cursor, "isSmartPlaylist");
          final int _cursorIndexOfSmartPlaylistType = CursorUtil.getColumnIndexOrThrow(_cursor, "smartPlaylistType");
          final PlaylistEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverUrl;
            if (_cursor.isNull(_cursorIndexOfCoverUrl)) {
              _tmpCoverUrl = null;
            } else {
              _tmpCoverUrl = _cursor.getString(_cursorIndexOfCoverUrl);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsSmartPlaylist;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSmartPlaylist);
            _tmpIsSmartPlaylist = _tmp != 0;
            final String _tmpSmartPlaylistType;
            if (_cursor.isNull(_cursorIndexOfSmartPlaylistType)) {
              _tmpSmartPlaylistType = null;
            } else {
              _tmpSmartPlaylistType = _cursor.getString(_cursorIndexOfSmartPlaylistType);
            }
            _result = new PlaylistEntity(_tmpId,_tmpName,_tmpDescription,_tmpCoverUrl,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsSmartPlaylist,_tmpSmartPlaylistType);
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
  public Flow<PlaylistEntity> getPlaylistByIdFlow(final long id) {
    final String _sql = "SELECT * FROM playlists WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"playlists"}, new Callable<PlaylistEntity>() {
      @Override
      @Nullable
      public PlaylistEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "coverUrl");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfIsSmartPlaylist = CursorUtil.getColumnIndexOrThrow(_cursor, "isSmartPlaylist");
          final int _cursorIndexOfSmartPlaylistType = CursorUtil.getColumnIndexOrThrow(_cursor, "smartPlaylistType");
          final PlaylistEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverUrl;
            if (_cursor.isNull(_cursorIndexOfCoverUrl)) {
              _tmpCoverUrl = null;
            } else {
              _tmpCoverUrl = _cursor.getString(_cursorIndexOfCoverUrl);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsSmartPlaylist;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSmartPlaylist);
            _tmpIsSmartPlaylist = _tmp != 0;
            final String _tmpSmartPlaylistType;
            if (_cursor.isNull(_cursorIndexOfSmartPlaylistType)) {
              _tmpSmartPlaylistType = null;
            } else {
              _tmpSmartPlaylistType = _cursor.getString(_cursorIndexOfSmartPlaylistType);
            }
            _result = new PlaylistEntity(_tmpId,_tmpName,_tmpDescription,_tmpCoverUrl,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsSmartPlaylist,_tmpSmartPlaylistType);
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
  public Flow<PlaylistWithSongs> getPlaylistWithSongs(final long id) {
    final String _sql = "SELECT * FROM playlists WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"playlist_songs", "songs",
        "playlists"}, new Callable<PlaylistWithSongs>() {
      @Override
      @Nullable
      public PlaylistWithSongs call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
            final int _cursorIndexOfCoverUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "coverUrl");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
            final int _cursorIndexOfIsSmartPlaylist = CursorUtil.getColumnIndexOrThrow(_cursor, "isSmartPlaylist");
            final int _cursorIndexOfSmartPlaylistType = CursorUtil.getColumnIndexOrThrow(_cursor, "smartPlaylistType");
            final LongSparseArray<ArrayList<SongEntity>> _collectionSongs = new LongSparseArray<ArrayList<SongEntity>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionSongs.containsKey(_tmpKey)) {
                _collectionSongs.put(_tmpKey, new ArrayList<SongEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipsongsAscomQuezicDataLocalEntitySongEntity(_collectionSongs);
            final PlaylistWithSongs _result;
            if (_cursor.moveToFirst()) {
              final PlaylistEntity _tmpPlaylist;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpName;
              _tmpName = _cursor.getString(_cursorIndexOfName);
              final String _tmpDescription;
              if (_cursor.isNull(_cursorIndexOfDescription)) {
                _tmpDescription = null;
              } else {
                _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
              }
              final String _tmpCoverUrl;
              if (_cursor.isNull(_cursorIndexOfCoverUrl)) {
                _tmpCoverUrl = null;
              } else {
                _tmpCoverUrl = _cursor.getString(_cursorIndexOfCoverUrl);
              }
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpUpdatedAt;
              _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
              final boolean _tmpIsSmartPlaylist;
              final int _tmp;
              _tmp = _cursor.getInt(_cursorIndexOfIsSmartPlaylist);
              _tmpIsSmartPlaylist = _tmp != 0;
              final String _tmpSmartPlaylistType;
              if (_cursor.isNull(_cursorIndexOfSmartPlaylistType)) {
                _tmpSmartPlaylistType = null;
              } else {
                _tmpSmartPlaylistType = _cursor.getString(_cursorIndexOfSmartPlaylistType);
              }
              _tmpPlaylist = new PlaylistEntity(_tmpId,_tmpName,_tmpDescription,_tmpCoverUrl,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsSmartPlaylist,_tmpSmartPlaylistType);
              final ArrayList<SongEntity> _tmpSongsCollection;
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              _tmpSongsCollection = _collectionSongs.get(_tmpKey_1);
              _result = new PlaylistWithSongs(_tmpPlaylist,_tmpSongsCollection);
            } else {
              _result = null;
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<SongEntity>> getSongsInPlaylist(final long playlistId) {
    final String _sql = "\n"
            + "        SELECT s.* FROM songs s\n"
            + "        INNER JOIN playlist_songs ps ON s.id = ps.songId\n"
            + "        WHERE ps.playlistId = ?\n"
            + "        ORDER BY ps.position ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"songs",
        "playlist_songs"}, new Callable<List<SongEntity>>() {
      @Override
      @NonNull
      public List<SongEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localPath");
          final int _cursorIndexOfSourceType = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceType");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfSourceUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceUrl");
          final int _cursorIndexOfGenre = CursorUtil.getColumnIndexOrThrow(_cursor, "genre");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfPlayCount = CursorUtil.getColumnIndexOrThrow(_cursor, "playCount");
          final int _cursorIndexOfLastPlayedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayedAt");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final List<SongEntity> _result = new ArrayList<SongEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SongEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpArtist;
            _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final String _tmpLocalPath;
            if (_cursor.isNull(_cursorIndexOfLocalPath)) {
              _tmpLocalPath = null;
            } else {
              _tmpLocalPath = _cursor.getString(_cursorIndexOfLocalPath);
            }
            final String _tmpSourceType;
            _tmpSourceType = _cursor.getString(_cursorIndexOfSourceType);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpSourceUrl;
            if (_cursor.isNull(_cursorIndexOfSourceUrl)) {
              _tmpSourceUrl = null;
            } else {
              _tmpSourceUrl = _cursor.getString(_cursorIndexOfSourceUrl);
            }
            final String _tmpGenre;
            if (_cursor.isNull(_cursorIndexOfGenre)) {
              _tmpGenre = null;
            } else {
              _tmpGenre = _cursor.getString(_cursorIndexOfGenre);
            }
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final int _tmpPlayCount;
            _tmpPlayCount = _cursor.getInt(_cursorIndexOfPlayCount);
            final Long _tmpLastPlayedAt;
            if (_cursor.isNull(_cursorIndexOfLastPlayedAt)) {
              _tmpLastPlayedAt = null;
            } else {
              _tmpLastPlayedAt = _cursor.getLong(_cursorIndexOfLastPlayedAt);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            _item = new SongEntity(_tmpId,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpDuration,_tmpThumbnailUrl,_tmpLocalPath,_tmpSourceType,_tmpSourceId,_tmpSourceUrl,_tmpGenre,_tmpAddedAt,_tmpPlayCount,_tmpLastPlayedAt,_tmpIsFavorite);
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
  public Object getPlaylistSongCount(final long playlistId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM playlist_songs WHERE playlistId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
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

  @Override
  public Object getPlaylistTotalDuration(final long playlistId,
      final Continuation<? super Long> $completion) {
    final String _sql = "SELECT SUM(s.duration) FROM songs s INNER JOIN playlist_songs ps ON s.id = ps.songId WHERE ps.playlistId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @Nullable
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final Long _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(0);
            }
            _result = _tmp;
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
  public Object getMaxPosition(final long playlistId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT MAX(position) FROM playlist_songs WHERE playlistId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @Nullable
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
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
  public Object isSongInPlaylist(final long playlistId, final String songId,
      final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlistId = ? AND songId = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    _argIndex = 2;
    _statement.bindString(_argIndex, songId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp != 0;
          } else {
            _result = false;
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
  public Flow<Integer> getPlaylistCount() {
    final String _sql = "SELECT COUNT(*) FROM playlists WHERE isSmartPlaylist = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"playlists"}, new Callable<Integer>() {
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
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getPlaylistsContainingSong(final String songId,
      final Continuation<? super List<Long>> $completion) {
    final String _sql = "SELECT playlistId FROM playlist_songs WHERE songId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, songId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Long> _result = new ArrayList<Long>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Long _item;
            _item = _cursor.getLong(0);
            _result.add(_item);
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

  private void __fetchRelationshipsongsAscomQuezicDataLocalEntitySongEntity(
      @NonNull final LongSparseArray<ArrayList<SongEntity>> _map) {
    if (_map.isEmpty()) {
      return;
    }
    if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      RelationUtil.recursiveFetchLongSparseArray(_map, true, (map) -> {
        __fetchRelationshipsongsAscomQuezicDataLocalEntitySongEntity(map);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `songs`.`id` AS `id`,`songs`.`title` AS `title`,`songs`.`artist` AS `artist`,`songs`.`album` AS `album`,`songs`.`duration` AS `duration`,`songs`.`thumbnailUrl` AS `thumbnailUrl`,`songs`.`localPath` AS `localPath`,`songs`.`sourceType` AS `sourceType`,`songs`.`sourceId` AS `sourceId`,`songs`.`sourceUrl` AS `sourceUrl`,`songs`.`genre` AS `genre`,`songs`.`addedAt` AS `addedAt`,`songs`.`playCount` AS `playCount`,`songs`.`lastPlayedAt` AS `lastPlayedAt`,`songs`.`isFavorite` AS `isFavorite`,_junction.`playlistId` FROM `playlist_songs` AS _junction INNER JOIN `songs` ON (_junction.`songId` = `songs`.`id`) WHERE _junction.`playlistId` IN (");
    final int _inputSize = _map.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (int i = 0; i < _map.size(); i++) {
      final long _item = _map.keyAt(i);
      _stmt.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      // _junction.playlistId;
      final int _itemKeyIndex = 15;
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfId = 0;
      final int _cursorIndexOfTitle = 1;
      final int _cursorIndexOfArtist = 2;
      final int _cursorIndexOfAlbum = 3;
      final int _cursorIndexOfDuration = 4;
      final int _cursorIndexOfThumbnailUrl = 5;
      final int _cursorIndexOfLocalPath = 6;
      final int _cursorIndexOfSourceType = 7;
      final int _cursorIndexOfSourceId = 8;
      final int _cursorIndexOfSourceUrl = 9;
      final int _cursorIndexOfGenre = 10;
      final int _cursorIndexOfAddedAt = 11;
      final int _cursorIndexOfPlayCount = 12;
      final int _cursorIndexOfLastPlayedAt = 13;
      final int _cursorIndexOfIsFavorite = 14;
      while (_cursor.moveToNext()) {
        final long _tmpKey;
        _tmpKey = _cursor.getLong(_itemKeyIndex);
        final ArrayList<SongEntity> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final SongEntity _item_1;
          final String _tmpId;
          _tmpId = _cursor.getString(_cursorIndexOfId);
          final String _tmpTitle;
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
          final String _tmpArtist;
          _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
          final String _tmpAlbum;
          if (_cursor.isNull(_cursorIndexOfAlbum)) {
            _tmpAlbum = null;
          } else {
            _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
          }
          final long _tmpDuration;
          _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
          final String _tmpThumbnailUrl;
          if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
            _tmpThumbnailUrl = null;
          } else {
            _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
          }
          final String _tmpLocalPath;
          if (_cursor.isNull(_cursorIndexOfLocalPath)) {
            _tmpLocalPath = null;
          } else {
            _tmpLocalPath = _cursor.getString(_cursorIndexOfLocalPath);
          }
          final String _tmpSourceType;
          _tmpSourceType = _cursor.getString(_cursorIndexOfSourceType);
          final String _tmpSourceId;
          _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
          final String _tmpSourceUrl;
          if (_cursor.isNull(_cursorIndexOfSourceUrl)) {
            _tmpSourceUrl = null;
          } else {
            _tmpSourceUrl = _cursor.getString(_cursorIndexOfSourceUrl);
          }
          final String _tmpGenre;
          if (_cursor.isNull(_cursorIndexOfGenre)) {
            _tmpGenre = null;
          } else {
            _tmpGenre = _cursor.getString(_cursorIndexOfGenre);
          }
          final long _tmpAddedAt;
          _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
          final int _tmpPlayCount;
          _tmpPlayCount = _cursor.getInt(_cursorIndexOfPlayCount);
          final Long _tmpLastPlayedAt;
          if (_cursor.isNull(_cursorIndexOfLastPlayedAt)) {
            _tmpLastPlayedAt = null;
          } else {
            _tmpLastPlayedAt = _cursor.getLong(_cursorIndexOfLastPlayedAt);
          }
          final boolean _tmpIsFavorite;
          final int _tmp;
          _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
          _tmpIsFavorite = _tmp != 0;
          _item_1 = new SongEntity(_tmpId,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpDuration,_tmpThumbnailUrl,_tmpLocalPath,_tmpSourceType,_tmpSourceId,_tmpSourceUrl,_tmpGenre,_tmpAddedAt,_tmpPlayCount,_tmpLastPlayedAt,_tmpIsFavorite);
          _tmpRelation.add(_item_1);
        }
      }
    } finally {
      _cursor.close();
    }
  }
}
