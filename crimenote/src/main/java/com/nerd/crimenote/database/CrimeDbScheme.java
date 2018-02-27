package com.nerd.crimenote.database;

public class CrimeDbScheme {

    public static final class CrimeTable {

        public static final String NAME = "crimes";

        public static final class Column {
            public static final String UUID = "uuid";
            public static final String TITLE = "title";
            public static final String DATE = "date";
            public static final String SOLVED = "solved";
            public static final String SUSPECT = "suspect";
        }
    }
}
