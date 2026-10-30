package com.example.studymate.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.studymate.backup.DataFormat;
import com.example.studymate.backup.DataFormats;
import com.example.studymate.data.Repository;
import com.example.studymate.databinding.ActivityContainerBinding;
import com.example.studymate.util.AppExecutors;
import com.example.studymate.util.FileUtil;
import com.example.studymate.util.Grades;
import com.example.studymate.util.Prefs;
import com.example.studymate.util.PdfStorage;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.example.studymate.util.GradeScale;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends BaseActivity {
    private ActivityContainerBinding b;
    private Repository repo;
    private TextView classLeadSummary, assignLeadSummary;

    private final ActivityResultLauncher<String> exportJson =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> exportTo(uri, DataFormats.json()));
    private final ActivityResultLauncher<String> exportCsv =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> exportTo(uri, DataFormats.csv()));
    private final ActivityResultLauncher<String[]> importPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::confirmImport);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityContainerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Settings & Backup", true);
        repo = Repository.get(this);

        section("Appearance");
        MaterialSwitch dark = switchRow("Dark mode", UiUtil.isNight(this));
        dark.setOnCheckedChangeListener((v, on) -> {
            Prefs.setDarkMode(this, on ? 1 : 0);
            Prefs.applyTheme(this);
        });

        section("Privacy");
        MaterialSwitch lock = switchRow("Lock notes with biometrics / screen lock", Prefs.notesLock(this));
        lock.setOnCheckedChangeListener((v, on) -> Prefs.putBool(this, "notes_lock", on));

        section("Reminders");
        classLeadSummary = buttonRow("Class reminder", classLeadText(), v -> pickLead(true));
        assignLeadSummary = buttonRow("Assignment reminder", assignLeadText(), v -> pickLead(false));

        section("Firebase Cloud Sync");
        if (com.example.studymate.firebase.FirebaseSync.isSignedIn()) {
            com.google.firebase.auth.FirebaseUser user = com.example.studymate.firebase.FirebaseSync.currentUser();
            String email = (user != null && user.getEmail() != null) ? user.getEmail() : "Guest User (Anonymous)";
            buttonRow("Signed in as " + email, "Syncs automatically across devices", null);
            buttonRow("Sync to Cloud Now", "Upload local study data to Cloud Firestore", v -> {
                com.example.studymate.firebase.FirebaseSync.pushToCloud(this, new com.example.studymate.firebase.FirebaseSync.SyncCallback() {
                    @Override public void onSuccess() { UiUtil.toast(SettingsActivity.this, "Data synced to Cloud!"); }
                    @Override public void onError(String error) { UiUtil.toast(SettingsActivity.this, error); }
                });
            });
            buttonRow("Pull from Cloud", "Restore study data from Cloud Firestore", v -> {
                com.example.studymate.firebase.FirebaseSync.pullFromCloud(this, new com.example.studymate.firebase.FirebaseSync.SyncCallback() {
                    @Override public void onSuccess() { UiUtil.toast(SettingsActivity.this, "Data pulled from Cloud!"); }
                    @Override public void onError(String error) { UiUtil.toast(SettingsActivity.this, error); }
                });
            });
            buttonRow("Sign Out", "Disconnect from Firebase Cloud account", v -> {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                UiUtil.toast(this, "Signed out");
                recreate();
            });
        } else {
            buttonRow("Sign In / Register for Cloud Sync", "Connect with Firebase to back up data & sync devices",
                    v -> startActivity(new Intent(this, LoginActivity.class)));
        }

        section("Backup");
        buttonRow("Export as JSON", "All routine, attendance, grades, tasks, exams, notes and study data", v -> exportJson.launch("StudyMate_backup.json"));
        buttonRow("Export as CSV", "Sectioned CSV you can open in a spreadsheet", v -> exportCsv.launch("StudyMate_backup.csv"));
        buttonRow("Import backup (JSON or CSV)", "Replaces the data currently in the app", v -> importPicker.launch(new String[]{"*/*"}));
        section("GPA scale");
        gpaScaleSummary = buttonRow("Grading scale", gpaScaleText(), v -> pickGradeScale());
        section("Saved PDFs");
        saveFolderSummary = buttonRow("Folder for merged & converted PDFs", PdfStorage.describe(this),
                v -> folderPicker.launch(null));
        buttonRow("Reset to default folder", PdfStorage.defaultLabel(), v -> {
            PdfStorage.resetToDefault(this);
            saveFolderSummary.setText(PdfStorage.describe(this));
            UiUtil.toast(this, "PDFs will be saved to " + PdfStorage.describe(this));
        });
        section("Document tools");
        buttonRow("Open document tools", "PDF reader, PPTX/DOCX viewers, converters, image to PDF",
                v -> startActivity(new Intent(this, DocumentToolsActivity.class)));

        section("About");
        buttonRow("StudyMate 1.0", "Works fully offline - all data stays on this device", null);
    }
    private String gpaScaleText() {
        GradeScale s = Grades.current(this);
        return s.name + " · " + s.summary;
    }

    private void pickGradeScale() {
        GradeScale cur = Grades.current(this);
        String[] labels = new String[Grades.ALL.length];
        int checked = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = Grades.ALL[i].name + " (" + Grades.ALL[i].summary + ")";
            if (Grades.ALL[i].id.equals(cur.id)) checked = i;
        }
        new MaterialAlertDialogBuilder(this).setTitle("Grading scale")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    GradeScale s = Grades.ALL[which];
                    if (!s.id.equals(cur.id)) {
                        Grades.setCurrent(this, s);
                        repo.regradeAll(s);
                        UiUtil.toast(this, "Grading scale: " + s.name);
                    }
                    gpaScaleSummary.setText(gpaScaleText());
                    d.dismiss();
                }).setNegativeButton("Cancel", null).show();
    }
    private String classLeadText() { return Prefs.classLeadMin(this) + " minutes before each class"; }

    private String assignLeadText() { return Prefs.assignLeadHours(this) + " hours before the due time"; }

    private void pickLead(boolean forClass) {
        int[] values = forClass ? new int[]{5, 10, 15, 30} : new int[]{1, 6, 12, 24};
        String unit = forClass ? " minutes" : " hours";
        String[] labels = new String[values.length];
        int cur = forClass ? Prefs.classLeadMin(this) : Prefs.assignLeadHours(this);
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i] + unit;
            if (values[i] == cur) checked = i;
        }
        new MaterialAlertDialogBuilder(this).setTitle(forClass ? "Remind me before class" : "Remind me before due")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    Prefs.putInt(this, forClass ? "class_lead_min" : "assign_lead_h", values[which]);
                    repo.rescheduleAll();
                    classLeadSummary.setText(classLeadText());
                    assignLeadSummary.setText(assignLeadText());
                    d.dismiss();
                }).setNegativeButton("Cancel", null).show();
    }

    // ---------- backup ----------
    private void exportTo(Uri uri, DataFormat format) {
        if (uri == null) return;
        AppExecutors.io(() -> {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                Map<String, List<Map<String, Object>>> data = repo.snapshotBlocking();
                format.write(data, os);
                AppExecutors.main(() -> UiUtil.toast(this, "Exported " + format.extension().toUpperCase() + " backup"));
            } catch (Exception e) {
                AppExecutors.main(() -> UiUtil.toast(this, "Export failed: " + e.getMessage()));
            }
        });
    }

    private void confirmImport(Uri uri) {
        if (uri == null) return;
        UiUtil.confirm(this, "Replace all data?", "Importing replaces everything currently stored in the app.", "Import", () -> {
            String name = FileUtil.displayName(this, uri);
            AppExecutors.io(() -> {
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    DataFormat format = DataFormats.forName(name);
                    Map<String, List<Map<String, Object>>> data = format.read(in);
                    repo.restoreBlocking(data);
                    AppExecutors.main(() -> UiUtil.toast(this, "Backup imported"));
                } catch (Exception e) {
                    AppExecutors.main(() -> UiUtil.toast(this, "Import failed: " + e.getMessage()));
                }
            });
        });
    }
    private TextView saveFolderSummary;
    private TextView gpaScaleSummary;
    private final ActivityResultLauncher<Uri> folderPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri == null) return;
                PdfStorage.setCustomTree(this, uri);
                saveFolderSummary.setText(PdfStorage.describe(this));
                UiUtil.toast(this, "PDFs will be saved to " + PdfStorage.describe(this));
            });

    // ---------- tiny UI builders ----------
    private void section(String title) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        t.setPadding(0, UiUtil.dp(this, 20), 0, UiUtil.dp(this, 4));
        b.content.addView(t);
    }

    private MaterialSwitch switchRow(String label, boolean checked) {
        MaterialSwitch s = new MaterialSwitch(this);
        s.setText(label);
        s.setChecked(checked);
        s.setPadding(0, UiUtil.dp(this, 8), 0, UiUtil.dp(this, 8));
        b.content.addView(s, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return s;
    }

    /** Returns the summary TextView so callers can update it. */
    private TextView buttonRow(String title, String summary, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, UiUtil.dp(this, 10), 0, UiUtil.dp(this, 10));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(16);
        TextView s = new TextView(this);
        s.setText(summary);
        s.setAlpha(0.7f);
        row.addView(t);
        row.addView(s);
        if (click != null) {
            row.setClickable(true);
            row.setOnClickListener(click);
            android.util.TypedValue tv = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
            row.setBackgroundResource(tv.resourceId);
        }
        b.content.addView(row);
        return s;
    }
}
