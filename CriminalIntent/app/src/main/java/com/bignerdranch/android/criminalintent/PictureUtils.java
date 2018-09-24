package com.bignerdranch.android.criminalintent;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

public class PictureUtils {
    private static String TAG = "PictureUtils";

    public static Bitmap getScaledBitmap(String path, Activity activity) {
        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay()
                .getSize(size);

        return getScaledBitmap(path, size.x, size.y);
    }

    public static Bitmap getScaledBitmap(String path, int destWidth, int destHeight) {
        // read in the dimensions of the image on disk
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        float srcWidth = options.outWidth;
        float srcHeight = options.outHeight;

        int inSampleSize = 1;
        if (srcHeight > destHeight || srcWidth > destWidth) {
            if (srcWidth > srcHeight) {
                inSampleSize = Math.round(srcHeight / destHeight);
            } else {
                inSampleSize = Math.round(srcWidth / destWidth);
            }
        }

        options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;

        return BitmapFactory.decodeFile(path, options);
    }

    public static class BitmapWithFaces
    {
        public final Bitmap bitmap;
        public final SparseArray<Face> faces;

        public BitmapWithFaces(Bitmap bitmap, SparseArray<Face> Faces)
        {
            this.bitmap = bitmap;
            this.faces = Faces;
        }
    }

    public static BitmapWithFaces MarkFaces(Bitmap bitmap, Context context) {
        final FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> faces = detector.detect(frame);
        Log.d(TAG, "Faces detected: " + String.valueOf(faces.size()));
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        BitmapWithFaces ret = new BitmapWithFaces(mutableBitmap, faces);
        for (int i = 0; i < faces.size(); ++i)
        {
            Face face = faces.valueAt(i);
            Path path = new Path();
            path.moveTo(face.getPosition().x, face.getPosition().y);
            path.lineTo(face.getPosition().x + face.getWidth(), face.getPosition().y);
            path.lineTo(face.getPosition().x + face.getWidth(), face.getPosition().y + face.getHeight());
            path.lineTo(face.getPosition().x, face.getPosition().y + face.getHeight());
            path.close();

            Paint redPaint = new Paint();
            redPaint.setColor(0XFFFF0000);
            redPaint.setStyle(Paint.Style.STROKE);
            redPaint.setStrokeWidth(8.0f);
            canvas.drawPath(path, redPaint);
        }
        return ret;
    }
}
