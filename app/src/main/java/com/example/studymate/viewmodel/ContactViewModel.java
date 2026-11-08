package com.example.studymate.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studymate.data.Repository;
import com.example.studymate.data.entity.Contact;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContactViewModel extends AndroidViewModel {
    private final Repository repo;
    private final LiveData<List<Contact>> all;
    /** 0 = all, 1 = teachers, 2 = classmates. */
    public final MutableLiveData<Integer> filter = new MutableLiveData<>(0);
    public final MutableLiveData<String> query = new MutableLiveData<>("");
    public final MediatorLiveData<List<Contact>> visible = new MediatorLiveData<>();

    public ContactViewModel(@NonNull Application app) {
        super(app);
        repo = Repository.get(app);
        all = repo.contacts();
        visible.addSource(all, l -> recompute());
        visible.addSource(filter, f -> recompute());
        visible.addSource(query, q -> recompute());
    }

    private static boolean has(String s, String needle) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void recompute() {
        List<Contact> list = all.getValue();
        Integer f = filter.getValue();
        if (list == null || f == null) return;
        String q = query.getValue();
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<Contact> out = new ArrayList<>();
        for (Contact c : list) {
            if (f == 1 && c.type != Contact.TEACHER) continue;
            if (f == 2 && c.type != Contact.CLASSMATE) continue;
            if (!needle.isEmpty() && !(has(c.name, needle) || has(c.rollOrDesignation, needle)
                    || has(c.phone, needle) || has(c.email, needle))) continue;
            out.add(c);
        }
        visible.setValue(out);
    }

    public void save(Contact c, Uri newPhoto, boolean removePhoto) { repo.saveContact(c, newPhoto, removePhoto); }

    public void delete(Contact c) { repo.deleteContact(c); }
}
