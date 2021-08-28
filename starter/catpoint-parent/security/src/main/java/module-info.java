module security {
    requires image;
    requires miglayout;
    requires com.google.gson;
    requires com.google.common;
    requires java.desktop;
    requires java.prefs;
    requires java.sql;
    opens com.udacity.catpoint.security.data to com.google.gson;
}