package com.example.studymate.ui;

import android.net.Uri;
import android.os.Bundle;
import android.print.PdfPrinter;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.studymate.databinding.ActivityDocxViewerBinding;
import com.example.studymate.docs.DocxParser;
import com.example.studymate.docs.ParserFactory;
import com.example.studymate.util.AppExecutors;
import com.example.studymate.util.FileUtil;
import com.example.studymate.util.PdfStorage;

import java.io.File;

/** Reads a .docx as HTML; "Export to PDF" prints the same HTML through Android's print framework. */
public class DocxViewerActivity extends BaseActivity {
    private static final int M_PDF = 1;

    private ActivityDocxViewerBinding b;
    private String bodyHtml;
    private String name;
    private WebView printView; // keep a reference so it is not garbage collected while printing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityDocxViewerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        String u = getIntent().getStringExtra("uri");
        if (u == null) { finish(); return; }
        Uri uri = Uri.parse(u);
        name = getIntent().getStringExtra("name");
        if (name == null) name = FileUtil.displayName(this, uri);
        setupToolbar(b.toolbar, name, true);
        boolean autoExport = getIntent().getBooleanExtra("autoExport", false);

        WebSettings ws = b.webView.getSettings();
        ws.setJavaScriptEnabled(false);
        ws.setAllowFileAccess(false);
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);

        AppExecutors.io(() -> {
            try {
                File f = FileUtil.copyToCache(this, uri, "docx");
                String html = ParserFactory.docx().parse(f);
                AppExecutors.main(() -> {
                    if (isDestroyed()) return;
                    bodyHtml = html;
                    b.loading.setVisibility(View.GONE);
                    b.webView.loadDataWithBaseURL(null, DocxParser.wrap(html, UiUtil.isNight(this)),
                            "text/html", "utf-8", null);
                    if (autoExport) exportPdf();
                });
            } catch (Exception e) {
                AppExecutors.main(() -> {
                    UiUtil.toast(this, "Couldn't read this DOCX: " + e.getMessage());
                    finish();
                });
            }
        });
    }

    private void exportPdf() {
        if (bodyHtml == null) return;
        UiUtil.toast(this, "Creating PDF...");
        printView = new WebView(this);
        printView.getSettings().setJavaScriptEnabled(false);
        printView.setWebViewClient(new WebViewClient() {
            private boolean started;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (started) return;
                started = true;
                String job = FileUtil.baseName(name);
                File out = new File(FileUtil.convertedDir(DocxViewerActivity.this), job + ".pdf");
                try {
                    PdfPrinter.write(view.createPrintDocumentAdapter(job), out, (ok, error) -> {
                        if (ok) AppExecutors.io(() -> PdfStorage.deliver(DocxViewerActivity.this, out));
                        else printWithDialog(view, job); // fallback: old print dialog
                    });
                } catch (Throwable t) {
                    printWithDialog(view, job);
                }
            }
        });
        printView.loadDataWithBaseURL(null, DocxParser.wrap(bodyHtml, false), "text/html", "utf-8", null);
    }

    private void printWithDialog(WebView view, String job) {
        PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
        pm.print(job, view.createPrintDocumentAdapter(job), new PrintAttributes.Builder().build());
        UiUtil.toast(this, "Choose \"Save as PDF\" as the printer");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem m = menu.add(Menu.NONE, M_PDF, Menu.NONE, "Export to PDF");
        m.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == M_PDF) { exportPdf(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
