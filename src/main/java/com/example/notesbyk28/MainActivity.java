package com.example.notesbyk28;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private NoteAdapter adapter;
    private NoteDao noteDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final Set<Integer> selectedNoteIds = new HashSet<>();
    private boolean isSelectionMode = false;
    private LinearLayout selectionMenu;
    private TextView textSelectedCount;
    private Toolbar mainToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean isDarkMode = (getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int statusBarColor = ContextCompat.getColor(this, R.color.light_yellow);
        int navigationBarColor = ContextCompat.getColor(this, R.color.cardview_light_background);

        EdgeToEdge.enable(this,
                isDarkMode ? SystemBarStyle.dark(statusBarColor)
                        : SystemBarStyle.light(statusBarColor, statusBarColor),
                isDarkMode ? SystemBarStyle.dark(navigationBarColor)
                        : SystemBarStyle.light(navigationBarColor, navigationBarColor)
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NoteDatabase database = NoteDatabase.getDatabase(this);
        noteDao = database.noteDao();

        selectionMenu = findViewById(R.id.selectionMenu);
        textSelectedCount = findViewById(R.id.textSelectedCount);
        mainToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mainToolbar);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewNotes);
        FloatingActionButton fab = findViewById(R.id.fabAddNote);

        setupWindowInsets();

        findViewById(R.id.btnDeleteSelected).setOnClickListener(v -> deleteSelected());
        findViewById(R.id.btnClearAll).setOnClickListener(v -> clearAllNotes());

        adapter = new NoteAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        noteDao.getAllNotes().observe(this, notes -> adapter.setNotes(notes));

        fab.setOnClickListener(view -> {
            if (isSelectionMode) exitSelectionMode();
            showNoteDialog(null);
        });
    }

    private void setupWindowInsets() {
        View mainView = findViewById(R.id.main);
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewNotes);
        FloatingActionButton fab = findViewById(R.id.fabAddNote);

        int fabBaseMargin = (int) (24 * getResources().getDisplayMetrics().density);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            appBarLayout.setPadding(0, systemBars.top, 0, 0);

            recyclerView.setPadding(
                    recyclerView.getPaddingLeft(),
                    recyclerView.getPaddingTop(),
                    recyclerView.getPaddingRight(),
                    systemBars.bottom
            );

            ViewGroup.MarginLayoutParams fabParams = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            fabParams.bottomMargin = fabBaseMargin + systemBars.bottom;
            fabParams.rightMargin = fabBaseMargin;
            fab.setLayoutParams(fabParams);

            return insets;
        });
    }

    private void toggleSelection(int noteId) {
        if (selectedNoteIds.contains(noteId)) {
            selectedNoteIds.remove(noteId);
        } else {
            selectedNoteIds.add(noteId);
        }

        if (selectedNoteIds.isEmpty()) {
            exitSelectionMode();
        } else {
            if (!isSelectionMode) enterSelectionMode();
            textSelectedCount.setText(selectedNoteIds.size() + " selected");
            adapter.notifyDataSetChanged();
        }
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        selectionMenu.setVisibility(View.VISIBLE);
        mainToolbar.setVisibility(View.GONE);
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedNoteIds.clear();
        selectionMenu.setVisibility(View.GONE);
        mainToolbar.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private void deleteSelected() {
        executorService.execute(() -> {
            if (!selectedNoteIds.isEmpty()) {
                List<Integer> idsToDelete = new ArrayList<>(selectedNoteIds);
                noteDao.deleteByIds(idsToDelete);
            }
            runOnUiThread(this::exitSelectionMode);
        });
    }

    private void clearAllNotes() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All")
                .setMessage("Are you sure you want to delete all notes?")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    executorService.execute(() -> {
                        noteDao.deleteAll();
                        runOnUiThread(this::exitSelectionMode);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNoteDialog(final Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(noteToEdit == null ? "New Note" : "Edit Note");
        final EditText input = new EditText(this);
        if (noteToEdit != null) input.setText(noteToEdit.getNoteText());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) {
                executorService.execute(() -> {
                    if (noteToEdit == null) {
                        noteDao.insert(new Note(text));
                    } else {
                        noteToEdit.setNoteText(text);
                        noteDao.update(noteToEdit);
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {
        private List<Note> mData;

        NoteAdapter(List<Note> data) {
            this.mData = data;
        }

        void setNotes(List<Note> notes) {
            this.mData = notes;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Note current = mData.get(position);
            holder.textView.setText(current.getNoteText());

            boolean isSelected = selectedNoteIds.contains(current.getId());
            holder.itemView.setBackgroundColor(isSelected ? Color.LTGRAY : Color.TRANSPARENT);

            holder.btnEdit.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);
            holder.btnDelete.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);

            holder.itemView.setOnClickListener(v -> {
                if (isSelectionMode) {
                    toggleSelection(current.getId());
                } else {
                    showNoteDialog(current);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                toggleSelection(current.getId());
                return true;
            });

            holder.btnEdit.setOnClickListener(v -> showNoteDialog(current));
            holder.btnDelete.setOnClickListener(v -> executorService.execute(() -> noteDao.delete(current)));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ImageButton btnEdit, btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.textNoteContent);
                btnEdit = itemView.findViewById(R.id.btnEditNote);
                btnDelete = itemView.findViewById(R.id.btnDeleteNote);
            }
        }
    }
}