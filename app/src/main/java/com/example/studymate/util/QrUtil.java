package com.example.studymate.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

public final class QrUtil {
    private QrUtil() {}

    public static Bitmap encode(String text, int size) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++)
                bmp.setPixel(x, y, m.get(x, y) ? Color.BLACK : Color.WHITE);
        return bmp;
    }
}
