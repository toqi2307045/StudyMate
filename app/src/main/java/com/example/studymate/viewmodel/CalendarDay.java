package com.example.studymate.viewmodel;

import java.time.LocalDate;

public class CalendarDay {
    public final LocalDate date;
    public final boolean isCurrentMonth;
    public boolean isSelected;
    public boolean isToday;
    public int status; // 0 = none, 1 = class, 2 = assignment, 3 = both

    public CalendarDay(LocalDate date, boolean isCurrentMonth, boolean isSelected, boolean isToday, int status) {
        this.date = date;
        this.isCurrentMonth = isCurrentMonth;
        this.isSelected = isSelected;
        this.isToday = isToday;
        this.status = status;
    }
}
