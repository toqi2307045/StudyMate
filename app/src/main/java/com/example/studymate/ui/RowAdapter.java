package com.example.studymate.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studymate.databinding.ItemRowBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** One generic adapter used by almost every list screen (courses, tasks, exams, notes...). */
public class RowAdapter<T> extends RecyclerView.Adapter<RowAdapter.VH> {

    public interface Binder<T> {
        void bind(ItemRowBinding b, T item);
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemRowBinding b;

        VH(ItemRowBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    private final Binder<T> binder;
    private final List<T> items = new ArrayList<>();
    private Consumer<T> onClick;
    private Consumer<T> onLongClick;

    public RowAdapter(Binder<T> binder) { this.binder = binder; }

    public RowAdapter<T> onClick(Consumer<T> c) { onClick = c; return this; }

    public RowAdapter<T> onLongClick(Consumer<T> c) { onLongClick = c; return this; }

    public void refresh() { notifyDataSetChanged(); }

    public void submit(List<T> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        T item = items.get(position);
        h.b.check.setOnCheckedChangeListener(null);
        h.b.check.setVisibility(android.view.View.GONE);
        h.b.trailing.setText("");
        h.b.title.setPaintFlags(h.b.title.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        h.b.title.setAlpha(1f);
        binder.bind(h.b, item);
        h.b.card.setOnClickListener(onClick == null ? null : v -> onClick.accept(item));
        h.b.card.setOnLongClickListener(onLongClick == null ? null : v -> { onLongClick.accept(item); return true; });
    }

    @Override
    public int getItemCount() { return items.size(); }
}
