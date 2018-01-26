package com.nerd.criminalintent;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.nerd.criminalintent.database.CrimeBaseHelper;
import com.nerd.criminalintent.database.CrimeCursorWrapper;
import com.nerd.criminalintent.database.CrimeDbScheme.CrimeTable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CrimeLab {

    private static CrimeLab sCrimeLab;
    private SQLiteDatabase mDatabase;
    private Context mContext;

    private CrimeLab(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new CrimeBaseHelper(mContext)
                .getWritableDatabase();
    }

    public static CrimeLab get(Context context) {
        if (null == sCrimeLab) {
            sCrimeLab = new CrimeLab(context);
        }
        return sCrimeLab;
    }

    public void add(Crime crime) {
        ContentValues values = getContentValues(crime);
        mDatabase.insert(CrimeTable.NAME, null, values);
    }

    public void remove(Crime crime) {
        mDatabase.delete(CrimeTable.NAME,
                CrimeTable.Column.UUID + "=?",
                new String[]{crime.getId().toString()});
    }

    public void updateCrime(Crime crime) {
        String id = crime.getId().toString();
        ContentValues values = getContentValues(crime);
        mDatabase.update(CrimeTable.NAME, values,
                CrimeTable.Column.UUID + " =? ",
                new String[]{id});
    }

    public Crime getCrime(UUID id) {
        CrimeCursorWrapper cursor = queryCrimes(
                CrimeTable.Column.UUID + "=?",
                new String[]{id.toString()}
        );

        try {
            if (0 == cursor.getCount()) {
                return null;
            }
            cursor.moveToFirst();
            return cursor.getCrime();
        } finally {
            cursor.close();
        }
    }

    public List<Crime> getCrimes() {
        List<Crime> crimes = new ArrayList<>();
        CrimeCursorWrapper cursor = queryCrimes(null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            crimes.add(cursor.getCrime());
            cursor.moveToNext();
        }
        cursor.close();
        return crimes;
    }

    public File getPhotoFile(Crime crime) {
        File externalFilesDir = mContext
                .getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (null == externalFilesDir) {
            return null;
        }

        return new File(externalFilesDir, crime.getPhotoFileName());
    }

    private ContentValues getContentValues(Crime crime) {
        ContentValues values = new ContentValues();
        values.put(CrimeTable.Column.UUID, crime.getId().toString());
        values.put(CrimeTable.Column.TITLE, crime.getTitle());
        values.put(CrimeTable.Column.DATE, crime.getDate().getTime());
        values.put(CrimeTable.Column.SOLVED, crime.isSolved() ? 1 : 0);
        values.put(CrimeTable.Column.SUSPECT, crime.getSuspect());
        return values;
    }

    /**
     * @param whereClause 从句
     */
    @SuppressLint("Recycle")
    private CrimeCursorWrapper queryCrimes(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                CrimeTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null);
        return new CrimeCursorWrapper(cursor);
    }
}
