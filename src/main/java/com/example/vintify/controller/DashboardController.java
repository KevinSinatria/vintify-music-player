package com.example.vintify.controller;

import com.example.vintify.Koneksi;
import com.google.common.base.Stopwatch;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.MultimediaInfo;
import jaco.mp3.player.MP3Player;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class DashboardController implements Initializable {
    // Deklarasi variabel general di kelas ini
    private String selectedSong = null;
    private static boolean isFileExists = false;
    private MP3Player player = new MP3Player();
    private boolean isSongPlay = false;
    private Stopwatch currentPosition = Stopwatch.createUnstarted();
    private Double longCurrentPositionInMs = null;
    private String formattedCurrentPosition = null;
    private Double currentMaxDurations = null;


    @FXML
    private Button btnAddMusic;

    @FXML
    private ImageView playPauseBtn;

    @FXML
    private Label currentPositionLabel;

    @FXML
    private Slider currentPositionSlider;

    @FXML
    private Slider currentVolumeSlider;
    private int currentValueOfVolumeSlider;

    @FXML
    private Label currentVolumeLabel;

    @FXML
    private ListView<String> songListView;

    @FXML
    private Label songLabel;

    @FXML
    private Label endDurationLabel;

    private class TestingTimerTask extends TimerTask {
        public static int i = 0;

        @Override
        public void run() {
            System.out.println("Timer ran:" + ++i);
        }
    }

    private class currentPositionTask extends TimerTask {
        @Override
        public void run() {
            longCurrentPositionInMs = Double.valueOf(currentPosition.elapsed(TimeUnit.MILLISECONDS));
            formattedCurrentPosition = msToMinutesSeconds(longCurrentPositionInMs);

            Platform.runLater(() -> {
                currentPositionLabel.setText(formattedCurrentPosition);
                currentPositionSlider.setValue((double) longCurrentPositionInMs);
            });
        }
    }

    @FXML
    private void onBtnAddMusicClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih file lagu (MP3)");
        fileChooser.setInitialDirectory(new File("C:/Users/Kevin"));

        // Hanya MP3 yang bisa di select
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Audio Files", "*.mp3"));

        List<File> selectedFile = fileChooser.showOpenMultipleDialog(btnAddMusic.getScene().getWindow());
        if (selectedFile != null) {
            for (File file : selectedFile) {
                Encoder encoder = new Encoder();

                String title = file.getName();
                String fileName = file.getName();
                String filePath = null;
                long duration = 0;
                long fileSize = file.length();

                try {
                    MultimediaInfo mi = encoder.getInfo(file);
                    duration = mi.getDuration();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (file.getName().toLowerCase().endsWith(".mp3")) {
                    // Proses menyimpan file musik mp3 ke dalam directory resource
                    try {
                        File targetFile = getFile(file);
                        filePath = targetFile.getAbsolutePath();

                        Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Error");
                        alert.setContentText("Gagal menyimpan data, error: " + e.getMessage());
                        alert.show();
                    }

                    // Proses menyimpan data ke database
                    if (!isFileExists) {
                        try {
                            Connection conn = Koneksi.getConnect();
                            String query = "INSERT INTO songs (title, filename, filepath, duration, filesize) VALUES (?, ?, ?, ?, ?)";
                            PreparedStatement pst = conn.prepareStatement(query);
                            pst.setString(1, title);
                            pst.setString(2, fileName);
                            pst.setString(3, filePath);
                            pst.setLong(4, duration);
                            pst.setLong(5, fileSize);

                            int resultQuery = pst.executeUpdate();
                            if (resultQuery > 0) {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Success");
                                alert.setHeaderText("Berhasil");
                                alert.setContentText("Data berhasil di upload!");
                                alert.show();

                                this.setListsSongsData();
                            } else {
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Error");
                                alert.setHeaderText("Error");
                                alert.setContentText("Gagal mengupload data!");
                                alert.show();
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Error");
                            alert.setContentText("Gagal mengupload data ke database: " + e.getMessage());
                            alert.show();
                        }
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error");
                    alert.setContentText("File yang diupload harus berekstensi mp3");
                    alert.show();
                }
            }
        }
    }

    private static File getFile(File file) {
        File targetDir = new File("src/main/resources/com/example/vintify/assets/musics");
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }

        File targetFile = new File(targetDir, file.getName());

        if (targetFile.exists()) {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Peringatan");
            alert.setHeaderText("Peringatan");
            alert.setContentText("File sudah ada, apakah anda ingin menimpanya?");
            alert.showAndWait();
            isFileExists = true;
        } else {
            isFileExists = false;
        }

        return targetFile;
    }

    @FXML
    public void onSongListViewClick() {
        selectedSong = String.valueOf(songListView.getSelectionModel().getSelectedItem());
        songLabel.setText(selectedSong);
        playPauseBtn.setImage(new Image(getClass().getResource("/com/example/vintify/assets/images/pause.png").toString()));
        player.stop();
        player.getPlayList().clear();

        // Menjadikan lagu yang dipilih menjadi lagu yang pertama saat playlist di mulai
        player.addToPlayList(new File("src/main/resources/com/example/vintify/assets/musics/" + selectedSong));

        currentPosition.reset();
        isSongPlay = true;
        player.play();
        currentPosition.start();

        try {
            Connection conn = Koneksi.getConnect();
            String sql = "SELECT SUM(duration) as total_duration FROM songs";
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                currentMaxDurations = rs.getDouble("total_duration");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        currentPositionSlider.setMin(0);
        currentPositionSlider.setMax(currentMaxDurations);
        endDurationLabel.setText(String.valueOf(msToMinutesSeconds(currentMaxDurations)));


        // Fungsi setInterval (js) di java dengan menggunakan timer
        Timer timer = new Timer();
        TimerTask task = new currentPositionTask();
        timer.schedule(task, 0, 100);

        // Membuat playlist
        try {
            Connection conn = Koneksi.getConnect();
            String sql = "SELECT filepath FROM songs";
//            PreparedStatement pst = conn.prepareStatement(sql);
//            pst.setString(1, selectedSong);
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            player.setRepeat(true);

            while (rs.next()) {
                player.addToPlayList(new File(rs.getString("filepath")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onPreviousBtnClick() {
        if (!isSongPlay) {
            player.play();
        }
        player.skipBackward();
        playPauseBtn.setImage(new Image(getClass().getResource("/com/example/vintify/assets/images/pause.png").toString()));
        isSongPlay = true;
    }

    @FXML
    public void onNextBtnClick() {
        if (!isSongPlay) {
            player.play();
            currentPosition.reset();
            currentPosition.start();
        }
        player.skipForward();
        playPauseBtn.setImage(new Image(getClass().getResource("/com/example/vintify/assets/images/pause.png").toString()));
        isSongPlay = true;
    }

    @FXML
    public void onPlayPauseBtnClick() {
        if (selectedSong == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error");
            alert.setContentText("Silakan pilih file yang ingin diputar terlebih dahulu.");
            alert.show();
        } else {
            if (!isSongPlay) {
                player.play();
                playPauseBtn.setImage(new Image(getClass().getResource("/com/example/vintify/assets/images/pause.png").toString()));
                isSongPlay = true;
                if (!player.isPaused()) {
                    currentPosition.start();
                }
            } else {
                player.pause();
                playPauseBtn.setImage(new Image(getClass().getResource("/com/example/vintify/assets/images/play-button-arrowhead.png").toString()));
                isSongPlay = false;
                if (player.isPaused()) {
                    currentPosition.stop();
                }
            }
        }
    }

    @FXML
    public void onCurrentVolumeSlider() {
        currentVolumeSlider.valueProperty().addListener((
                observable, oldValue, newValue) -> {
            try {
                String cmd = "src/main/java/com/example/vintify/controller/nircmd.exe setsysvolume " + newValue.intValue();
                Runtime.getRuntime().exec(cmd);
            } catch (RuntimeException | IOException e) {
                throw new RuntimeException(e);
            }

            currentVolumeLabel.setText((newValue.intValue() * 100) / 65535 + "%");
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.setListsSongsData();

        currentVolumeSlider.setMin(0);
        currentVolumeSlider.setMax(65535);
        currentVolumeSlider.setValue(65535);

        currentValueOfVolumeSlider = (int) currentVolumeSlider.getValue();
        try {
            String cmd = "src/main/java/com/example/vintify/controller/nircmd.exe setsysvolume " + currentValueOfVolumeSlider;
            Runtime.getRuntime().exec(cmd);
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException(e);
        }
        currentVolumeLabel.setText(currentValueOfVolumeSlider / 65535 * 100 + "%");
    }

    private void setListsSongsData() {
        // Memastikan list item kosong sebelum di isi oleh data
        songListView.getItems().clear();
        // Menampilkan title dari songs yang ada di database
        try {
            Connection conn = Koneksi.getConnect();
            String sql = "SELECT title FROM songs";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                String title = rs.getString("title");

                songListView.getItems().add(title);
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error");
            alert.setContentText("Gagal menampilkan data, error: " + e.getMessage());
            alert.show();
        }
    }

    private String msToMinutesSeconds(Double ms) {
        long minutes = (long) (ms / (1000 * 60));
        long seconds = (long) ((ms % (1000 * 60)) / 1000);
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        return formattedTime;
    }
}
