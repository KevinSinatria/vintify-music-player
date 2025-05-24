module com.example.vintify {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires java.desktop;
    requires transitive jave;
    requires java.sql;
    requires transitive jaco.mp3.player;

    opens com.example.vintify to javafx.fxml;
    exports com.example.vintify;
    exports com.example.vintify.controller;
    opens com.example.vintify.controller to javafx.fxml;
}