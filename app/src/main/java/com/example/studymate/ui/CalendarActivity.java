package com.example.studymate.ui;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studymate.databinding.ActivityCalendarBinding;
import com.example.studymate.databinding.ItemCalendarDayBinding;
import com.example.studymate.util.TimeUtil;
import com.example.studymate.viewmodel.CalendarDay;
import com.example.studymate.viewmodel.CalendarViewModel;
import com.example.studymate.viewmodel.CalendarViewModel.Entry;

import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends BaseActivity {
    private static final int M_TODAY = 1;

    private ActivityCalendarBinding b;
    private CalendarViewModel vm;
    private RowAdapter<Entry> adapter;
    private MonthGridAdapter monthAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Calendar", true);
        vm = new ViewModelProvider(this).get(CalendarViewModel.class);

        monthAdapter = new MonthGridAdapter();
        b.rvMonthGrid.setLayoutManager(new GridLayoutManager(this, 7));
        b.rvMonthGrid.setAdapter(monthAdapter);

        b.btnPrevMonth.setOnClickListener(v -> vm.prevMonth());
        b.btnNextMonth.setOnClickListener(v -> vm.nextMonth());

        vm.currentMonth.observe(this, month -> {
            if (month != null) {
                String monthName = month.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
                b.tvMonthTitle.setText(monthName + " " + month.getYear());
            }
        });

        vm.monthDays.observe(this, monthAdapter::submitList);

        adapter = new RowAdapter<>((r, e) -> {
            r.strip.setBackgroundColor(e.color);
            r.title.setText(e.title);
            r.subtitle.setText(e.subtitle);
            r.trailing.setText(e.trailing);
            if (e.type == Entry.ASSIGNMENT) {
                r.check.setVisibility(View.VISIBLE);
                r.check.setChecked(e.done);
                r.check.setOnCheckedChangeListener((cb, checked) -> vm.setDone(e.assignment, checked));
                if (e.done) {
                    r.title.setPaintFlags(r.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    r.title.setAlpha(0.5f);
                }
            }
        });
        adapter.onClick(e -> startActivity(new Intent(this,
                e.type == Entry.CLASS ? RoutineActivity.class : e.type == Entry.ASSIGNMENT ? AssignmentsActivity.class : ExamsActivity.class)));
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);
        b.empty.setText("Nothing scheduled for this day 🎉");

        vm.date.observe(this, d -> b.dateTitle.setText(TimeUtil.formatDate(d)));
        vm.entries.observe(this, list -> {
            adapter.submit(list);
            int classes = 0, tasks = 0, examsCount = 0;
            for (Entry e : list) {
                if (e.type == Entry.CLASS) classes++;
                else if (e.type == Entry.ASSIGNMENT) tasks++;
                else if (e.type == Entry.EXAM) examsCount++;
            }
            b.summary.setText(classes + (classes == 1 ? " class" : " classes") + "  ·  "
                    + tasks + (tasks == 1 ? " assignment" : " assignments") + "  ·  "
                    + examsCount + (examsCount == 1 ? " exam" : " exams"));

            int typeCount = (classes > 0 ? 1 : 0) + (tasks > 0 ? 1 : 0) + (examsCount > 0 ? 1 : 0);
            int status = CalendarViewModel.STATUS_NONE;
            if (typeCount >= 2) {
                status = CalendarViewModel.STATUS_MULTIPLE;
            } else if (classes > 0) {
                status = CalendarViewModel.STATUS_CLASS_ONLY;
            } else if (tasks > 0) {
                status = CalendarViewModel.STATUS_ASSIGNMENT_ONLY;
            } else if (examsCount > 0) {
                status = CalendarViewModel.STATUS_EXAM_ONLY;
            }

            int color = CalendarViewModel.getStatusColor(status);
            b.statusStrip.setBackgroundColor(color);

            b.empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        addMenu(menu, M_TODAY, "Go to today");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == M_TODAY) {
            long today = TimeUtil.startOfDay(System.currentTimeMillis());
            vm.date.setValue(today);
            vm.currentMonth.setValue(java.time.ZonedDateTime.now());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class MonthGridAdapter extends RecyclerView.Adapter<MonthGridAdapter.VH> {
        private final List<CalendarDay> data = new ArrayList<>();

        public void submitList(List<CalendarDay> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCalendarDayBinding binding = ItemCalendarDayBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CalendarDay day = data.get(position);
            holder.b.tvDayNumber.setText(String.valueOf(day.date.getDayOfMonth()));

            int bgColor = 0x00000000;
            int textColor;

            if (UiUtil.isNight(holder.itemView.getContext())) {
                textColor = day.isCurrentMonth ? 0xFFFFFFFF : 0x4DFFFFFF;
            } else {
                textColor = day.isCurrentMonth ? 0xFF000000 : 0x4D000000;
            }

            if (day.status == CalendarViewModel.STATUS_CLASS_ONLY) {
                bgColor = 0x332196F3;
                textColor = 0xFF1976D2;
            } else if (day.status == CalendarViewModel.STATUS_ASSIGNMENT_ONLY) {
                bgColor = 0x33FF9800;
                textColor = 0xFFE65100;
            } else if (day.status == CalendarViewModel.STATUS_EXAM_ONLY) {
                bgColor = 0x33E53935;
                textColor = 0xFFC62828;
            } else if (day.status == CalendarViewModel.STATUS_MULTIPLE) {
                bgColor = 0x339C27B0;
                textColor = 0xFF7B1FA2;
            }

            holder.b.statusBg.setBackgroundColor(bgColor);
            holder.b.tvDayNumber.setTextColor(textColor);

            if (day.isSelected) {
                holder.b.cardDay.setStrokeWidth(UiUtil.dp(CalendarActivity.this, 2));
            } else {
                holder.b.cardDay.setStrokeWidth(0);
            }

            holder.itemView.setOnClickListener(v -> {
                vm.date.setValue(day.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemCalendarDayBinding b;
            VH(ItemCalendarDayBinding binding) {
                super(binding.getRoot());
                this.b = binding;
            }
        }
    }
}
