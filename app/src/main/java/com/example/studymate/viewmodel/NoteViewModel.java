package com.example.studymate.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.studymate.data.Repository;
import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.Note;

import java.util.List;

public class NoteViewModel extends AndroidViewModel {
    private final Repository repo;
    public final MutableLiveData<String> query = new MutableLiveData<>("");
    public final LiveData<List<Note>> notes;
    public final LiveData<List<Course>> courses;

    public NoteViewModel(@NonNull Application app) {
        super(app);
        repo = Repository.get(app);
        courses = repo.courses();
        notes = Transformations.switchMap(query, repo::searchNotes);
    }

    public void load(long id, Repository.Callback<Note> cb) { repo.getNote(id, cb); }

    public void save(Note n, Repository.Callback<Long> cb) { repo.saveNote(n, cb); }

    public void delete(Note n) { repo.deleteNote(n); }
}
