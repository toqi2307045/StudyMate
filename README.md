# StudyMate – Academic Management App (Java, Android Studio)

Fully offline Android app (Java, minSdk 26, targetSdk 34). No server, no network permission.

## Open and run
1. Android Studio (Hedgehog or newer) → **File ▸ Open** → select this `StudyMate` folder.
2. Let Gradle sync (needs internet once to download dependencies). If Studio offers an AGP / Gradle upgrade, accept it.
3. Run on a device/emulator with API 26+.

## Architecture
```
ui/         Activities + adapters + custom views (View layer)
viewmodel/  AndroidViewModel classes exposing LiveData (ViewModel layer)
data/       Repository, AppDatabase, DAOs, entities (Room = Model layer)
work/       ReminderWorker (WorkManager)           util/AppExecutors (thread pool)
docs/       Parsers (PPTX, DOCX), renderers, PDF exporters
backup/     JSON / CSV import-export strategies
widget/     Home-screen widget (today's classes)
```
* **MVVM**: Activity → ViewModel (LiveData) → Repository → Room DAO.
* **Background work**: WorkManager one-time requests for class / assignment / pomodoro notifications
  (class reminders re-arm themselves every week) + a fixed thread pool (`AppExecutors`) for disk and document work.
* **Strategy + Factory**: `DocumentParser` (`PptxParser`, `DocxParser`) via `ParserFactory`;
  `PdfExporter` (`PptxPdfExporter`, `ImagePdfExporter`) via `ExporterFactory`;
  `DataFormat` (`JsonFormat`, `CsvFormat`) via `DataFormats`.
* **Offline**: Room database, SharedPreferences, `PdfRenderer`, `PdfDocument`, XmlPullParser – no network code.

## Feature → code map
| # | Feature | Main classes |
|---|---------|--------------|
| 1 | Class routine, reminders, colours | `RoutineActivity`, `ReminderScheduler`, `ReminderWorker`, `CoursesActivity` |
| 2 | Attendance + <75% warning | `AttendanceActivity`, `AttendanceViewModel` |
| 3 | GPA / CGPA + chart | `GpaActivity`, `GpaViewModel`, `Grades`, `LineChartView` |
| 4 | To-do / deadlines | `AssignmentsActivity`, `AssignmentViewModel` |
| 5 | Exam countdown | `ExamsActivity` |
| 6 | Notes + tags + biometric lock | `NotesActivity`, `NoteEditActivity`, `BiometricGate` |
| 7 | Pomodoro + stats chart | `PomodoroActivity`, `PomodoroViewModel`, `BarChartView` |
| 8 | Backup, export/import, dark mode | `SettingsActivity`, `backup/*` |
| 9 | PDF reader | `PdfReaderActivity`, `PdfPageRenderer`, `ZoomImageView`, `PdfLibraryActivity` |
| 10 | PPTX viewer | `PptxViewerActivity`, `PptxParser`, `SlideRenderer` |
| 11 | PPTX → PDF | `PptxPdfExporter` |
| 12 | Course files | `CourseFilesActivity` |
| 13 | DOCX reader | `DocxViewerActivity`, `DocxParser` |
| 14 | DOCX → PDF | HTML + `PrintManager` (print dialog → "Save as PDF") |
| 15 | Image → PDF | `ImageToPdfActivity`, `ImagePdfExporter`, `ImageFilters` |
| Extra | Widget / QR share / scan filters | `RoutineWidgetProvider`, `RoutineShare` + `QrUtil`, `ImageFilters` |

## Notes and limits (good to mention in your report)
* Grade scale is the 4.0 +/- scale in `util/Grades.java` – edit the two arrays to match your university.
* PPTX rendering is approximate: text boxes, pictures and tables are placed by their stored positions; placeholders that
  inherit position from the slide layout use default regions; no charts, SmartArt, shapes, animations or theme fonts/colours.
* DOCX rendering covers headings, bold/italic/underline, bullet + numbered lists, simple tables and images.
* DOCX → PDF uses Android's print system, so the user picks "Save as PDF" in the print dialog.
* QR sharing shows a QR code of a compact text format; a friend can scan it with any scanner and use
  *Import shared routine* (paste the text). Scanning inside the app is not included.
* The Pomodoro session is recorded when the timer finishes while the app process is alive; the background
  notification still fires if the app is closed.
