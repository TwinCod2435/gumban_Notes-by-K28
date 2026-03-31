package com.example.notesbyk28;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NoteDao {
    @Insert
    void insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("SELECT * FROM notes_table ORDER BY id DESC")
    LiveData<List<Note>> getAllNotes(); // LiveData notifies the UI on changes automatically

    @Query("DELETE FROM notes_table WHERE id IN (:ids)")
    void deleteByIds(List<Integer> ids);

    @Query("DELETE FROM notes_table")
    void deleteAll();
}