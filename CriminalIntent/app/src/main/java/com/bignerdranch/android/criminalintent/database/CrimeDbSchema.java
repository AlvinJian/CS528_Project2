package com.bignerdranch.android.criminalintent.database;

public class CrimeDbSchema {
    public static final class CrimeTable {
        public static final String NAME = "crimes";

        public static final class Cols {
            public static final String UUID = "uuid";
            public static final String TITLE = "title";
            public static final String DATE = "date";
            public static final String SOLVED = "solved";
            public static final String SUSPECT = "suspect";
            public static final String IMG_1 = "img_1";
            public static final String IMG_2 = "img_2";
            public static final String IMG_3 = "img_3";
            public static final String IMG_4 = "img_4";

        }
    }
}
