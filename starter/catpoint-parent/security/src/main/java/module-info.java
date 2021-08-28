module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires com.google.gson;
    requires com.google.common;
    requires java.desktop;
    requires java.prefs;
    requires java.sql;
    requires com.miglayout.swing;
    opens com.udacity.catpoint.security.data to com.google.gson;
}