package android.print;

import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import java.io.File;

/** Lives in android.print because LayoutResultCallback / WriteResultCallback have package-private constructors. */
public final class PdfPrinter {
    public interface Callback {
        void onDone(boolean ok, String error);
    }

    private PdfPrinter() {}

    public static void write(PrintDocumentAdapter adapter, File out, Callback cb) {
        PrintAttributes attrs = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                .setMinMargins(new PrintAttributes.Margins(400, 400, 400, 400))
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        adapter.onStart();
        adapter.onLayout(null, attrs, new CancellationSignal(), new PrintDocumentAdapter.LayoutResultCallback() {
            @Override
            public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                ParcelFileDescriptor pfd;
                try {
                    pfd = ParcelFileDescriptor.open(out, ParcelFileDescriptor.MODE_READ_WRITE
                            | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
                } catch (Exception e) {
                    adapter.onFinish();
                    cb.onDone(false, e.getMessage());
                    return;
                }
                adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, pfd, new CancellationSignal(),
                        new PrintDocumentAdapter.WriteResultCallback() {
                            @Override
                            public void onWriteFinished(PageRange[] pages) {
                                close(pfd);
                                adapter.onFinish();
                                cb.onDone(true, null);
                            }

                            @Override
                            public void onWriteFailed(CharSequence error) {
                                close(pfd);
                                adapter.onFinish();
                                cb.onDone(false, String.valueOf(error));
                            }

                            @Override
                            public void onWriteCancelled() {
                                close(pfd);
                                adapter.onFinish();
                                cb.onDone(false, "cancelled");
                            }
                        });
            }

            @Override
            public void onLayoutFailed(CharSequence error) {
                adapter.onFinish();
                cb.onDone(false, String.valueOf(error));
            }

            @Override
            public void onLayoutCancelled() {
                adapter.onFinish();
                cb.onDone(false, "cancelled");
            }
        }, null);
    }

    private static void close(ParcelFileDescriptor pfd) {
        try { pfd.close(); } catch (Exception ignored) { }
    }
}