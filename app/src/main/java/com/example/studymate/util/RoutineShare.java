package com.example.studymate.util;

import com.example.studymate.data.entity.ClassSession;
import com.example.studymate.data.entity.Course;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Compact text format for sharing a weekly routine (used by QR code / share / paste-import). */
public final class RoutineShare {
    public static final String HEADER = "StudyMate-ROUTINE-1";

    private RoutineShare() {}

    private static String clean(String s) {
        return s == null ? "" : s.replace("|", "/").replace("\n", " ");
    }

    public static String encode(List<Course> courses, List<ClassSession> sessions) {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        Map<Long, Integer> idx = new HashMap<>();
        for (Course c : courses) {
            idx.put(c.id, idx.size());
            sb.append("C|").append(idx.get(c.id)).append('|').append(clean(c.name)).append('|')
                    .append(clean(c.code)).append('|').append(clean(c.teacher)).append('|')
                    .append(c.color).append('\n');
        }
        for (ClassSession s : sessions) {
            Integer ci = idx.get(s.courseId);
            if (ci == null) continue;
            sb.append("S|").append(ci).append('|').append(s.dayOfWeek).append('|')
                    .append(s.startMin).append('|').append(s.endMin).append('|')
                    .append(clean(s.room)).append('\n');
        }
        return sb.toString();
    }

    public static class Parsed {
        public final List<Course> courses = new ArrayList<>();
        public final List<ClassSession> sessions = new ArrayList<>();
        /** For each session, index into courses. */
        public final List<Integer> sessionCourse = new ArrayList<>();
    }

    public static Parsed decode(String text) {
        if (text == null) return null;
        String[] lines = text.trim().split("\\r?\\n");
        if (lines.length == 0 || !lines[0].trim().equals(HEADER)) return null;
        Parsed p = new Parsed();
        try {
            for (int i = 1; i < lines.length; i++) {
                String[] f = lines[i].split("\\|", -1);
                if (f[0].equals("C") && f.length >= 6) {
                    Course c = new Course();
                    c.name = f[2];
                    c.code = f[3];
                    c.teacher = f[4];
                    c.color = Integer.parseInt(f[5]);
                    p.courses.add(c);
                } else if (f[0].equals("S") && f.length >= 6) {
                    ClassSession s = new ClassSession();
                    s.dayOfWeek = Integer.parseInt(f[2]);
                    s.startMin = Integer.parseInt(f[3]);
                    s.endMin = Integer.parseInt(f[4]);
                    s.room = f[5];
                    p.sessions.add(s);
                    p.sessionCourse.add(Integer.parseInt(f[1]));
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return p;
    }
}
