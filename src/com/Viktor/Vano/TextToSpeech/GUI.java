package com.Viktor.Vano.TextToSpeech;
import com.Viktor.Vano.TextToSpeech.Files.BooleanFile;
import com.Viktor.Vano.TextToSpeech.Files.IntegerFile;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class GUI extends Application {
    private final String version = "20260110";
    private int port = 7775;
    private boolean progressBarPresent = false;
    private final int width = 450;
    private final int height = 150;
    private TextToSpeechServer textToSpeechServer;
    public int audioIndex = 0;
    private ArrayList<Mixer.Info> audios;
    private final ComboBox<String> comboBox = new ComboBox<>();

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage stage)
    {
        Pane pane = new Pane();

        Scene scene = new Scene(pane, width, height);

        stage.setTitle("Text To Speech " + version);
        stage.setScene(scene);
        stage.show();
        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
        stage.setMaxWidth(stage.getWidth());
        stage.setMaxHeight(stage.getHeight());
        stage.setResizable(false);

        createDirectoryIfNotExist("res");

        try{
            port = IntegerFile.loadIntegerFromFile("tts_port.txt", port);
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        try{
            progressBarPresent = BooleanFile.loadBooleanFromFile("tts_progress_bar_present.txt", progressBarPresent);
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        Label labelPort = new Label("Port: " + port);
        labelPort.setFont(Font.font("Arial", 24));
        labelPort.setLayoutX(160);
        labelPort.setLayoutY(80);
        pane.getChildren().add(labelPort);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setLayoutX(30);
        progressBar.setLayoutY(120);
        progressBar.setPrefWidth(400);
        progressBar.setStyle("-fx-accent: red");
        if(progressBarPresent)
        {
            pane.getChildren().add(progressBar);
        }

        pane.setOnMouseClicked(event -> {
            if(pane.getChildren().contains(progressBar))
            {
                pane.getChildren().remove(progressBar);
                progressBarPresent = false;
            }
            else
            {
                pane.getChildren().add(progressBar);
                progressBarPresent = true;
            }
            BooleanFile.saveBooleanToFile("tts_progress_bar_present.txt", progressBarPresent);
        });

        try
        {
            Image icon = new Image(getClass().getResourceAsStream("tts.jpg"));
            stage.getIcons().add(icon);
            System.out.println("Icon loaded on the first attempt...");
        }catch(Exception e)
        {
            try
            {
                Image icon = new Image("tts.jpg");
                stage.getIcons().add(icon);
                System.out.println("Icon loaded on the second attempt...");
            }catch(Exception e1)
            {
                System.out.println("Icon failed to load...");
            }
        }

        setupAudioDevices();

        addAudioDevicesToComboBox();
        comboBox.getSelectionModel().select(audioIndex);
        comboBox.setLayoutX(30);
        comboBox.setLayoutY(40);
        pane.getChildren().add(comboBox);

        comboBox.setOnAction(event -> {
            int selectedIndex = comboBox.getSelectionModel().getSelectedIndex();
            if(selectedIndex != -1)
            {
                IntegerFile.saveIntegerToFile("tts_audio.txt", selectedIndex);
                System.out.println("\n\nUsing audio output device [" + selectedIndex + "]:");
                Mixer.Info selectedInfo = audios.get(selectedIndex);
                System.out.println(String.format("Name: [%s]\nDescription: [%s]", selectedInfo.getName(), selectedInfo.getDescription()));
                System.out.println("\n\n");
                updateAudioDevice(audios.get(selectedIndex));  // Update the server with the new device
            }
        });

        Button buttonRefresh = new Button("Refresh device list");
        buttonRefresh.setLayoutX(30);
        buttonRefresh.setLayoutY(10);
        buttonRefresh.setOnAction(event -> {
            clearAudioDevicesFromComboBox();
            addAudioDevicesToComboBox();
            audioIndex = 0;
            comboBox.getSelectionModel().select(0);
        });
        pane.getChildren().add(buttonRefresh);

        textToSpeechServer = new TextToSpeechServer(port, audios.get(audioIndex));
        textToSpeechServer.start();
    }

    public void setupAudioDevices()
    {
        // Param for playback (input) device.
        Line.Info playbackLine = new Line.Info(SourceDataLine.class);
        audios = filterDevices(playbackLine);

        System.out.println("\n\nFound audio devices:\n");
        for(int i = 0; i < audios.size(); i++)
        {
            Mixer.Info info = audios.get(i);
            System.out.println(String.format("Index [%s]\nName [%s]\nDescription [%s]", i, info.getName(), info.getDescription()));
            System.out.println("\n");
        }

        try{
            audioIndex = IntegerFile.loadIntegerFromFile("tts_audio.txt", audioIndex);
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        if(audios.size()-1 < audioIndex)
        {
            audioIndex = 0;
        }

        System.out.println("\n\nUsing audio output device [" + audioIndex + "]:");
        Mixer.Info info = audios.get(audioIndex);
        System.out.println(String.format("Name: [%s]\nDescription: [%s]", info.getName(), info.getDescription()));
        System.out.println("\n\n");
    }

    private void addAudioDevicesToComboBox()
    {
        for (Mixer.Info mixerInfo : audios) {
            comboBox.getItems().add(mixerInfo.getName());
        }
    }

    private void clearAudioDevicesFromComboBox()
    {
        comboBox.getItems().clear();
    }

    public void updateAudioDevice(Mixer.Info newAudioDevice) {
        // Logic to update the audio device
        // This might involve restarting the playback line with the new device
        textToSpeechServer.setMixerInfo(newAudioDevice);
    }

    private static ArrayList<Mixer.Info> filterDevices(final Line.Info supportedLine) {
        ArrayList<Mixer.Info> result = new ArrayList<>();

        List<Mixer.Info> audioInfo = Arrays.asList(AudioSystem.getMixerInfo());
        ArrayList<Mixer.Info> infos = new ArrayList<>(audioInfo);
        for (Mixer.Info info : infos) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (mixer.isLineSupported(supportedLine)) {
                result.add(info);
            }
        }
        return result;
    }

    @Override
    public void stop() throws Exception
    {
        super.stop();
        textToSpeechServer.stopServer();
    }

    public static void createDirectoryIfNotExist(String directoryName)
    {
        File file = new File(directoryName);
        if(file.mkdir())
            System.out.println("New directory \"" + directoryName + "\" was created.");
        else
            System.out.println("Directory \"" + directoryName + "\" already exists.");
    }
}
