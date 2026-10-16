package com.example.studymate.viewmodel;

import android.app.Application;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studymate.data.Repository;
import com.example.studymate.data.entity.Course;
import com.example.studymate.data.entity.StudySession;
import com.example.studymate.util.NotificationHelper;
import com.example.studymate.util.Prefs;
import com.example.studymate.util.ReminderScheduler;

import java.util.List;

/** Focus / short break / long break cycle. The ViewModel keeps the timer alive across rotation. */
public class PomodoroViewModel extends AndroidViewModel {
    public static final int FOCUS = 0, SHORT_BREAK = 1, LONG_BREAK = 2;

    private final Repository repo;
    public final LiveData<List<Course>> courses;
    public final LiveData<List<StudySession>> sessions;

    public final MutableLiveData<Long> remainingMs = new MutableLiveData<>();
    public final MutableLiveData<Integer> phase = new MutableLiveData<>(FOCUS);
    public final MutableLiveData<Boolean> running = new MutableLiveData<>(false);
    public final MutableLiveData<Integer> completedFocus = new MutableLiveData<>(0);
    public long courseId = 0;

    private CountDownTimer timer;
    private long phaseStart;

    public PomodoroViewModel(@NonNull Application app) {
        super(app);
        repo = Repository.get(app);
        courses = repo.courses();
        sessions = repo.studySessions();
        remainingMs.setValue(phaseMillis(FOCUS));
    }

    private long phaseMillis(int p) {
        int min = p == FOCUS ? Prefs.focusMin(getApplication())
                : p == SHORT_BREAK ? Prefs.shortBreakMin(getApplication()) : Prefs.longBreakMin(getApplication());
        return min * 60_000L;
    }

    public void start() {
        if (Boolean.TRUE.equals(running.getValue())) return;
        long ms = remainingMs.getValue() == null ? phaseMillis(FOCUS) : remainingMs.getValue();
        if (phaseStart == 0) phaseStart = System.currentTimeMillis();
        running.setValue(true);
        int p = phase.getValue() == null ? FOCUS : phase.getValue();
        ReminderScheduler.schedulePomodoro(getApplication(), ms,
                p == FOCUS ? "Focus session finished" : "Break is over",
                p == FOCUS ? "Time for a break." : "Ready for the next focus session?");
        timer = new CountDownTimer(ms, 500) {
            @Override public void onTick(long left) { remainingMs.setValue(left); }

            @Override public void onFinish() { finishPhase(); }
        }.start();
    }

    public void pause() {
        if (timer != null) timer.cancel();
        running.setValue(false);
        ReminderScheduler.cancelPomodoro(getApplication());
    }

    public void reset() {
        pause();
        phaseStart = 0;
        remainingMs.setValue(phaseMillis(phase.getValue() == null ? FOCUS : phase.getValue()));
    }

    public void skip() {
        pause();
        phaseStart = 0;
        nextPhase();
    }

    private void finishPhase() {
        running.setValue(false);
        int p = phase.getValue() == null ? FOCUS : phase.getValue();
        if (p == FOCUS) {
            int seconds = (int) (phaseMillis(FOCUS) / 1000);
            repo.addStudySession(courseId, phaseStart, seconds);
            completedFocus.setValue((completedFocus.getValue() == null ? 0 : completedFocus.getValue()) + 1);
        }
        ReminderScheduler.cancelPomodoro(getApplication()); // in-app notification replaces the background one
        NotificationHelper.show(getApplication(), NotificationHelper.CH_POMO, 42,
                p == FOCUS ? "Focus session finished" : "Break is over",
                p == FOCUS ? "Time for a break." : "Ready for the next focus session?");
        phaseStart = 0;
        nextPhase();
    }

    private void nextPhase() {
        int p = phase.getValue() == null ? FOCUS : phase.getValue();
        int done = completedFocus.getValue() == null ? 0 : completedFocus.getValue();
        int next;
        if (p == FOCUS) next = (done > 0 && done % 4 == 0) ? LONG_BREAK : SHORT_BREAK;
        else next = FOCUS;
        phase.setValue(next);
        remainingMs.setValue(phaseMillis(next));
    }

    @Override
    protected void onCleared() {
        if (timer != null) timer.cancel();
    }
}
