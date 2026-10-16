package com.example.studymate.ui;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studymate.data.entity.AttendanceRecord;
import com.example.studymate.data.entity.Course;
import com.example.studymate.databinding.ActivityListBinding;
import com.example.studymate.databinding.ItemAttendanceBinding;
import com.example.studymate.util.TimeUtil;
import com.example.studymate.viewmodel.AttendanceViewModel;
import com.example.studymate.viewmodel.AttendanceViewModel.CourseAttendance;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendanceActivity extends BaseActivity {
    private static final String[] STATUS = {"Present", "Absent", "Late"};

    private ActivityListBinding b;
    private AttendanceViewModel vm;
    private final List<CourseAttendance> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Attendance", true);
        b.fab.setVisibility(View.GONE);
        b.empty.setText("Add courses first, then mark attendance here.");
        vm = new ViewModelProvider(this).get(AttendanceViewModel.class);

        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ItemAttendanceBinding ib = ItemAttendanceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new RecyclerView.ViewHolder(ib.getRoot()) { };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
                bindItem(ItemAttendanceBinding.bind(h.itemView), items.get(position));
            }

            @Override
            public int getItemCount() { return items.size(); }
        };
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);

        vm.items.observe(this, list -> {
            items.clear();
            items.addAll(list);
            adapter.notifyDataSetChanged();
            b.empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void bindItem(ItemAttendanceBinding ib, CourseAttendance a) {
        Course c = a.course;
        ib.dot.setBackgroundColor(c.color);
        ib.name.setText(c.label());
        double pct = a.percent();
        ib.percent.setText(a.total() == 0 ? "–" : String.format(Locale.getDefault(), "%.0f%%", pct));
        ib.percent.setTextColor(a.belowThreshold() ? 0xFFD32F2F : (a.total() == 0 ? Color.GRAY : 0xFF388E3C));
        ib.progress.setProgress((int) Math.round(pct));
        ib.stats.setText(a.attended() + " of " + a.total() + " classes attended  ·  Present " + a.present
                + "  Late " + a.late + "  Absent " + a.absent);
        if (a.belowThreshold()) {
            ib.warning.setVisibility(View.VISIBLE);
            int need = a.classesNeeded();
            ib.warning.setText("⚠ Below 75% – attend the next " + need + " class" + (need == 1 ? "" : "es") + " in a row to recover");
        } else {
            ib.warning.setVisibility(View.GONE);
        }
        ib.btnPresent.setOnClickListener(v -> { vm.markToday(c.id, AttendanceRecord.PRESENT); UiUtil.toast(this, "Marked present today"); });
        ib.btnLate.setOnClickListener(v -> { vm.markToday(c.id, AttendanceRecord.LATE); UiUtil.toast(this, "Marked late today"); });
        ib.btnAbsent.setOnClickListener(v -> { vm.markToday(c.id, AttendanceRecord.ABSENT); UiUtil.toast(this, "Marked absent today"); });
        ib.btnHistory.setOnClickListener(v -> showHistory(c));
    }

    private void showHistory(Course c) {
        vm.history(c.id, list -> {
            MaterialAlertDialogBuilder d = new MaterialAlertDialogBuilder(this).setTitle(c.label());
            if (list.isEmpty()) {
                d.setMessage("No attendance recorded yet.");
            } else {
                String[] rows = new String[list.size()];
                for (int i = 0; i < rows.length; i++) {
                    rows[i] = TimeUtil.formatDate(list.get(i).date) + "  —  " + STATUS[list.get(i).status];
                }
                d.setItems(rows, (dlg, i) -> UiUtil.confirm(this, "Delete record?", rows[i], "Delete",
                        () -> vm.delete(list.get(i))));
            }
            d.setNeutralButton("Add past date", (dlg, w) -> pickDate(c));
            d.setNegativeButton("Close", null);
            d.show();
        });
    }

    private void pickDate(Course c) {
        LocalDate now = LocalDate.now();
        new DatePickerDialog(this, (dp, y, m, day) -> {
            long millis = LocalDate.of(y, m + 1, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            new MaterialAlertDialogBuilder(this).setTitle(TimeUtil.formatDate(millis))
                    .setItems(STATUS, (dlg, which) -> vm.mark(c.id, millis, which)).show();
        }, now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth()).show();
    }
}
