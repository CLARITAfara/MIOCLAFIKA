package com.mixeo.desktop;

import com.mixeo.common.FileLogger;
import com.mixeo.common.Mp3Metadata;
import com.mixeo.common.RabbitConfig;
import com.mixeo.common.RabbitPublisher;
import com.mixeo.desktop.model.Mp3FileItem;
import com.mixeo.desktop.service.FolderWatcherService;
import com.mixeo.desktop.service.MetadataService;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Equivalent JavaFX de Mixeo.Desktop (WPF / MainWindow.xaml).
 * - program1 : scan dossier + blacklist -> publie les chemins vers la queue mp3.files
 * - program2 : consomme mp3.files -> extrait les metadonnees -> publie vers mp3.metadata
 */
public class MixeoDesktopApp extends Application {

    // Palette reprise du XAML WPF.
    private static final String BG_PRIMARY = "#0F1117";
    private static final String BG_HEADER = "#141720";
    private static final String BG_SURFACE = "#1A1D27";
    private static final String ACCENT_BLUE = "#4F8EF7";
    private static final String ACCENT_GREEN = "#3DD68C";
    private static final String TEXT_PRIMARY = "#E8EAF0";
    private static final String TEXT_MUTED = "#6B7280";
    private static final String BORDER = "#2A2D3E";

    private final FolderWatcherService watcher = new FolderWatcherService();
    private final MetadataService metadataService = new MetadataService();
    private final ObservableList<Mp3FileItem> items = FXCollections.observableArrayList();

    private String selectedFolder = "";

    private Label folderText;
    private Label fileCountText;
    private Label statusText;
    private Label titleVal;
    private Label artistVal;
    private Label albumVal;
    private Label genreVal;
    private Label durationVal;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_PRIMARY + ";");

        VBox top = new VBox(buildHeader(), buildToolbar(stage));
        root.setTop(top);
        root.setCenter(buildTable());
        root.setBottom(new VBox(buildDetailPanel(), buildStatusBar()));

        Scene scene = new Scene(root, 980, 680);
        stage.setTitle("Mixeo — Scan répertoire");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(540);
        stage.show();

        // Timer auto toutes les 60 s (= DispatcherTimer 1 min en WPF).
        Timeline timer = new Timeline(new KeyFrame(Duration.minutes(1), e -> scanNow()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        // Demarre le worker program2 au chargement (= Loaded -> StartProgram2 en WPF).
        startProgram2();
    }

    // ─── UI ───

    private Region buildHeader() {
        Circle dot = new Circle(5, Color.web(ACCENT_BLUE));
        Label brand = styledLabel("M I X E O", 15, FontWeight.BOLD, TEXT_PRIMARY);
        Label sub = styledLabel("  /  Scan répertoire", 13, FontWeight.NORMAL, TEXT_MUTED);
        HBox box = new HBox(10, dot, brand, sub);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 20, 0, 20));
        box.setPrefHeight(56);
        box.setStyle("-fx-background-color: " + BG_HEADER + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");
        return box;
    }

    private Region buildToolbar(Stage stage) {
        Button selectBtn = new Button("📁  Choisir un répertoire");
        selectBtn.setStyle(primaryButtonStyle(ACCENT_BLUE));
        selectBtn.setOnMouseEntered(e -> selectBtn.setStyle(primaryButtonStyle("#3A7AE8")));
        selectBtn.setOnMouseExited(e -> selectBtn.setStyle(primaryButtonStyle(ACCENT_BLUE)));
        selectBtn.setOnAction(e -> onSelectFolder(stage));

        folderText = styledLabel("Aucun répertoire sélectionné", 13, FontWeight.NORMAL, TEXT_MUTED);
        HBox folderBox = new HBox(folderText);
        folderBox.setAlignment(Pos.CENTER_LEFT);
        folderBox.setPadding(new Insets(0, 12, 0, 12));
        folderBox.setStyle("-fx-background-color: " + BG_PRIMARY + "; -fx-border-color: " + BORDER
                + "; -fx-border-radius: 6; -fx-background-radius: 6;");
        HBox.setHgrow(folderBox, Priority.ALWAYS);

        Label countLabel = styledLabel("Fichiers : ", 12, FontWeight.NORMAL, TEXT_MUTED);
        fileCountText = styledLabel("0", 12, FontWeight.SEMI_BOLD, ACCENT_BLUE);
        HBox badge = new HBox(countLabel, fileCountText);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(6, 12, 6, 12));
        badge.setStyle("-fx-background-color: #1E2130; -fx-border-color: " + BORDER
                + "; -fx-border-radius: 6; -fx-background-radius: 6;");

        HBox toolbar = new HBox(14, selectBtn, folderBox, badge);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 20, 12, 20));
        toolbar.setStyle("-fx-background-color: " + BG_SURFACE + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");
        return toolbar;
    }

    private TableView<Mp3FileItem> buildTable() {
        TableView<Mp3FileItem> table = new TableView<>(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: " + BG_PRIMARY + "; -fx-control-inner-background: #1E2130;"
                + " -fx-table-cell-border-color: " + BORDER + ";");

        table.getColumns().add(column("TITRE", "title", 240));
        table.getColumns().add(column("ARTISTE", "artist", 160));
        table.getColumns().add(column("ALBUM", "album", 160));
        table.getColumns().add(column("GENRE", "genre", 100));
        table.getColumns().add(column("ANNÉE", "year", 70));
        table.getColumns().add(column("DURÉE", "duration", 80));

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> showDetail(sel));
        return table;
    }

    private TableColumn<Mp3FileItem, ?> column(String header, String property, double width) {
        TableColumn<Mp3FileItem, Object> col = new TableColumn<>(header);
        col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        return col;
    }

    private Region buildDetailPanel() {
        titleVal = detailValue(TEXT_PRIMARY);
        artistVal = detailValue(TEXT_PRIMARY);
        albumVal = detailValue(TEXT_PRIMARY);
        genreVal = detailValue(TEXT_PRIMARY);
        durationVal = detailValue(ACCENT_GREEN);

        HBox panel = new HBox(8,
                detailKey("Titre"), titleVal, spacer(),
                detailKey("Artiste"), artistVal, spacer(),
                detailKey("Album"), albumVal, spacer(),
                detailKey("Genre"), genreVal, spacer(),
                detailKey("Durée"), durationVal);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(14, 20, 14, 20));
        panel.setStyle("-fx-background-color: " + BG_HEADER + "; -fx-border-color: " + BORDER + "; -fx-border-width: 1 0 0 0;");
        clearDetail();
        return panel;
    }

    private Region buildStatusBar() {
        statusText = styledLabel("Prêt — choisissez un répertoire pour commencer.", 11, FontWeight.NORMAL, "#4B5160");
        Label auto = styledLabel("Scan auto toutes les 60 s", 11, FontWeight.NORMAL, "#4B5160");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(statusText, spacer, auto);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 20, 0, 20));
        bar.setPrefHeight(28);
        bar.setStyle("-fx-background-color: #0C0E14; -fx-border-color: " + BORDER + "; -fx-border-width: 1 0 0 0;");
        return bar;
    }

    // ─── Logique (program1 + program2) ───

    private void onSelectFolder(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            selectedFolder = dir.getAbsolutePath();
            folderText.setText(selectedFolder);
            folderText.setTextFill(Color.web(TEXT_PRIMARY));
            scanNow();
        }
    }

    private void scanNow() {
        if (selectedFolder == null || selectedFolder.isBlank()) {
            return;
        }
        setStatus("Scan en cours…");

        List<Mp3FileItem> files = watcher.scanFolder(selectedFolder);
        items.setAll(files);
        fileCountText.setText(String.valueOf(files.size()));
        setStatus("Scan terminé — " + files.size() + " fichier(s) trouvé(s). Dernier scan : "
                + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        clearDetail();

        // program1 : publier les chemins vers RabbitMQ (en arriere-plan).
        Thread publisher = new Thread(() -> {
            FileLogger.log("program1", "--- Started folder scan: " + selectedFolder + " ---");
            for (Mp3FileItem file : files) {
                File f = new File(file.getAbsolutePath());
                if (f.exists()) {
                    try {
                        FileLogger.log("Program1", "[RABBITMQ] Requesting publish for file path: " + file.getAbsolutePath());
                        RabbitPublisher.publishMessage(RabbitConfig.QUEUE_FILES, file.getAbsolutePath());
                        FileLogger.log("Program1", "[RABBITMQ] Successfully requested publish for: " + file.getAbsolutePath());
                    } catch (Exception ex) {
                        FileLogger.log("program1", "Error publishing " + file.getAbsolutePath() + ": " + ex.getMessage());
                    }
                }
            }
            FileLogger.log("program1", "--- Folder scan complete. Found " + files.size() + " files. ---");
        });
        publisher.setDaemon(true);
        publisher.start();
    }

    private void startProgram2() {
        Thread worker = new Thread(() -> {
            FileLogger.log("program2", "Program 2 worker starting...");
            try {
                ConnectionFactory factory = RabbitConfig.createConnectionFactory();
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                channel.queueDeclare(RabbitConfig.QUEUE_FILES, false, false, false, null);
                channel.queueDeclare(RabbitConfig.QUEUE_METADATA, false, false, false, null);

                DeliverCallback callback = (consumerTag, delivery) -> {
                    String path = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    FileLogger.log("Program2", "[RABBITMQ] Consume from queue: " + path);

                    if (new File(path).exists()) {
                        try {
                            Mp3Metadata meta = metadataService.extract(path);
                            FileLogger.log("program2", "Extracted metadata for: " + path
                                    + " (Title: '" + meta.getTitle() + "', Artist: '" + meta.getArtist() + "')");

                            RabbitPublisher.publishJson(RabbitConfig.QUEUE_METADATA, meta);
                            FileLogger.log("Program2", "[RABBITMQ] Publish metadata to " + RabbitConfig.QUEUE_METADATA + " for: " + path);

                            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        } catch (Exception ex) {
                            FileLogger.log("Program2", "[RABBITMQ] NACK for " + path + ": " + ex.getMessage());
                            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                        }
                    } else {
                        FileLogger.log("Program2", "[RABBITMQ] ACK (file not found, skipping): " + path);
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    }
                };

                channel.basicConsume(RabbitConfig.QUEUE_FILES, false, callback, consumerTag -> { });
                FileLogger.log("program2", "Program 2 worker is listening on queue: " + RabbitConfig.QUEUE_FILES);
            } catch (Exception ex) {
                FileLogger.log("program2", "Program 2 worker failed to start: " + ex.getMessage());
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void showDetail(Mp3FileItem sel) {
        if (sel == null) {
            clearDetail();
            return;
        }
        String path = sel.getAbsolutePath();
        if (path == null || path.isEmpty() || !new File(path).exists()) {
            return;
        }
        try {
            Mp3Metadata meta = metadataService.extract(path);
            titleVal.setText(blankToDash(meta.getTitle()));
            albumVal.setText(blankToDash(meta.getAlbum()));
            artistVal.setText(blankToDash(meta.getArtist()));
            genreVal.setText(blankToDash(meta.getGenre()));
            durationVal.setText(meta.getDuration() + " s");
        } catch (Exception ex) {
            clearDetail();
        }
    }

    private void clearDetail() {
        titleVal.setText("—");
        artistVal.setText("—");
        albumVal.setText("—");
        genreVal.setText("—");
        durationVal.setText("—");
    }

    private void setStatus(String message) {
        Platform.runLater(() -> statusText.setText(message));
    }

    // ─── Helpers UI ───

    private static Label styledLabel(String text, double size, FontWeight weight, String color) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", weight, size));
        l.setTextFill(Color.web(color));
        return l;
    }

    private Label detailKey(String text) {
        return styledLabel(text, 11, FontWeight.NORMAL, TEXT_MUTED);
    }

    private Label detailValue(String color) {
        return styledLabel("—", 13, FontWeight.NORMAL, color);
    }

    private Region spacer() {
        Region r = new Region();
        r.setMinWidth(16);
        return r;
    }

    private static String primaryButtonStyle(String bg) {
        return "-fx-background-color: " + bg + "; -fx-text-fill: white; -fx-font-size: 13;"
                + " -fx-font-weight: bold; -fx-padding: 9 18 9 18; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    private static String blankToDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
