package com.nerd.criminalintent.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.nerd.criminalintent.Crime;
import com.nerd.criminalintent.database.CrimeDbScheme.CrimeTable.Column;

import java.util.Date;
import java.util.UUID;

public class CrimeCursorWrapper extends CursorWrapper {

    public CrimeCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Crime getCrime() {
        String id = getString(getColumnIndex(Column.UUID));
        String title = getString(getColumnIndex(Column.TITLE));
        long date = getLong(getColumnIndex(Column.DATE));
        int isSolved = getInt(getColumnIndex(Column.SOLVED));
        String suspect = getString(getColumnIndex(Column.SUSPECT));

        Crime crime = new Crime(UUID.fromString(id));
        crime.setTitle(title);
        crime.setDate(new Date(date));
        crime.setSolved(isSolved != 0);
        crime.setSuspect(suspect);

        return crime;
    }
}
