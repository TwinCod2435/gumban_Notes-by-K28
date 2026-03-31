package com.example.notesbyk28;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes_table")
public class Note {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "note_text")
    private String noteText;

    public Note(String noteText) {
        this.noteText = noteText;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNoteText() { return noteText; }
    public void setNoteText(String noteText) { this.noteText = noteText; }
}