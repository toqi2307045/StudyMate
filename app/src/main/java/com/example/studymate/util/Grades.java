package com.example.studymate.util;

import android.content.Context;

public final class Grades {
    private Grades() {}

    public static final GradeScale KUET = new GradeScale("kuet", "KUET", "4.00 scale, A+ = 80%+",
            new String[]{"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "D", "F"},
            new double[]{4.00, 3.75, 3.50, 3.25, 3.00, 2.75, 2.50, 2.25, 2.00, 0.00},
            new int[]{80, 75, 70, 65, 60, 55, 50, 45, 40, 0});

    public static final GradeScale NORTH_AMERICA = new GradeScale("na", "North America", "4.0 scale, A = 93%+",
            new String[]{"A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "D-", "F"},
            new double[]{4.0, 3.7, 3.3, 3.0, 2.7, 2.3, 2.0, 1.7, 1.3, 1.0, 0.7, 0.0},
            new int[]{93, 90, 87, 83, 80, 77, 73, 70, 67, 63, 60, 0});

    public static final GradeScale BD_SCHOOL = new GradeScale("bd_school", "Bangladesh school (SSC/HSC)",
            "5.00 scale, A+ = 80%+",
            new String[]{"A+", "A", "A-", "B", "C", "D", "F"},
            new double[]{5.00, 4.00, 3.50, 3.00, 2.00, 1.00, 0.00},
            new int[]{80, 70, 60, 50, 40, 33, 0});

    public static final GradeScale[] ALL = {KUET, NORTH_AMERICA, BD_SCHOOL};

    public static GradeScale current(Context c) {
        String id = Prefs.getString(c, "grade_scale", KUET.id);
        for (GradeScale s : ALL) if (s.id.equals(id)) return s;
        return KUET;
    }

    public static void setCurrent(Context c, GradeScale s) { Prefs.putString(c, "grade_scale", s.id); }
}