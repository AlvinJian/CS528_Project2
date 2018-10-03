package com.bignerdranch.android.criminalintent.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.bignerdranch.android.criminalintent.Crime;

import java.util.Date;
import java.util.UUID;

import com.bignerdranch.android.criminalintent.database.CrimeDbSchema.CrimeTable;

public class CrimeCursorWrapper extends CursorWrapper {
    public CrimeCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Crime getCrime() {
        String uuidString = getString(getColumnIndex(CrimeTable.Cols.UUID));
        String title = getString(getColumnIndex(CrimeTable.Cols.TITLE));
        long date = getLong(getColumnIndex(CrimeTable.Cols.DATE));
        int isSolved = getInt(getColumnIndex(CrimeTable.Cols.SOLVED));
        String suspect = getString(getColumnIndex(CrimeTable.Cols.SUSPECT));
        String img_1URI = getString(getColumnIndex(CrimeTable.Cols.IMG_1));
        String img_2URI = getString(getColumnIndex(CrimeTable.Cols.IMG_2));
        String img_3URI = getString(getColumnIndex(CrimeTable.Cols.IMG_3));
        String img_4URI = getString(getColumnIndex(CrimeTable.Cols.IMG_4));
        int imgPos = getInt(getColumnIndex(CrimeTable.Cols.CURRENT_IMAGE_POSITION));
        int faceId = getInt(getColumnIndex(CrimeTable.Cols.FACE_DETECT_INDEX));

        Crime crime = new Crime(UUID.fromString(uuidString));
        crime.setTitle(title);
        crime.setDate(new Date(date));
        crime.setSolved(isSolved != 0);
        crime.setSuspect(suspect);
        crime.setImg_1(img_1URI);
        crime.setImg_2(img_2URI);
        crime.setImg_3(img_3URI);
        crime.setImg_4(img_4URI);
        crime.setCurrentImagePosition(imgPos);
        crime.setFaceDetectIndex(faceId);

        return crime;
    }
}
