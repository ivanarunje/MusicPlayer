package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.squti.androidwaverecorder.WaveRecorder;
import com.jlibrosa.audio.JLibrosa;
import com.jlibrosa.audio.exception.FileFormatNotSupportedException;
import com.jlibrosa.audio.wavFile.WavFileException;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener{

    private TextView tvSongName, tvCurrentTime, tvFinalTime;
    private ImageButton btnStart;
    private SeekBar seekBar;
    private MediaPlayer player;
    private Handler myHandler;
    private Interpreter interpreter;
    private JLibrosa jLibrosa;
    private WaveRecorder wr;

    private String current, predictedLabel;
    private String filepath = "";
    private Integer startStop = 1;
    private ArrayList<String> dataList;
    private Long start, finish;

    private static final Integer SELECT_NEXT = 1;
    private static final Integer SELECT_PREVIOUS = 2;
    private static final Integer SEEK_TIME = 5000;
    private static final String[] modelLabels = {"backward","forward", "next", "play", "previous", "stop"};

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataList = (ArrayList<String>) getIntent().getSerializableExtra("paths");
        current = getIntent().getStringExtra("current");

        ImageButton btnPrevious = findViewById(R.id.previous);
        ImageButton btnNext = findViewById(R.id.next);
        ImageButton btnForward = findViewById(R.id.forward);
        ImageButton btnBackward = findViewById(R.id.backward);
        ImageButton btnRecord = findViewById(R.id.btnRecord);
        ImageButton btnPlaylist = findViewById(R.id.btnPlaylist);

        btnStart = findViewById(R.id.start);
        tvSongName = findViewById(R.id.tvSongName);
        tvCurrentTime = findViewById(R.id.tvStartTime);
        tvFinalTime = findViewById(R.id.tvFinishTime);
        seekBar = findViewById(R.id.seekbar);
        seekBar.setClickable(false);

        tvSongName.setText(current.substring(current.lastIndexOf("/") + 1).trim());
        tvSongName.setSelected(true);
        player = MediaPlayer.create(this, Uri.parse(current));
        tvFinalTime.setText(getFormat(player.getDuration()));
        tvCurrentTime.setText(getFormat(player.getCurrentPosition()));
        seekBar.setProgress(player.getCurrentPosition());
        seekBar.setMax(player.getDuration());
        myHandler = new Handler();
        myHandler.postDelayed(UpdateSongTime,100);
        start();

        for (ImageButton imageButton : Arrays.asList(btnPrevious, btnNext, btnStart, btnForward, btnBackward, btnPlaylist)) {
            imageButton.setOnClickListener(this);
        }
        btnRecord.setOnTouchListener(this);

        jLibrosa = new JLibrosa();

        try {
            interpreter = new Interpreter(loadModelFile(), null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ContextWrapper cn = new ContextWrapper(getApplicationContext());
        filepath = cn.getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString();
        wr = new WaveRecorder(filepath+"/record.wav");


        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                selectSong(SELECT_NEXT);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser)
                    player.seekTo(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case(R.id.previous):
                selectSong(SELECT_PREVIOUS);
                break;

            case(R.id.next):
                selectSong(SELECT_NEXT);
                break;

            case(R.id.start):
                start();
                break;

            case(R.id.forward):
                forward();
                break;

            case(R.id.backward):
                backward();
                break;

            case(R.id.btnPlaylist):
                startNewActivity();
                break;
        }
    }

    private void start(){
        if(startStop == 1) {
            startStop = 0;
            btnStart.setBackgroundResource(R.drawable.pause);
            player.start();
        }
        else
        {
            startStop = 1;
            btnStart.setBackgroundResource(R.drawable.play);
            player.pause();
        }
    }

    private void selectSong(int prevOrNext)
    {
        int newIndex = 0;

        String newPath = "";
        for (int i=0; i<dataList.size(); i++){

            if(current.equals(dataList.get(i))){
                if (prevOrNext == SELECT_NEXT) {
                    newIndex = i + 1;
                    if (newIndex >= dataList.size())
                        newIndex = 0;
                }
                else
                {
                    newIndex = i-1;
                    if (newIndex < 0)
                        newIndex = dataList.size() - 1;
                }
                newPath = dataList.get(newIndex);
            }
        }
        current = newPath;
        player.stop();
        player.reset();
        tvSongName.setText(current.substring(current.lastIndexOf("/") + 1).trim());

        try {
            player.setDataSource(newPath);
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.start();
        seekBar.setMax(player.getDuration());
        startStop = 0;
        btnStart.setBackgroundResource(R.drawable.pause);
    }

    private void forward(){
        if((player.getCurrentPosition()+SEEK_TIME)<=player.getDuration()){
            player.seekTo(player.getCurrentPosition() + SEEK_TIME);
        }else{
            player.seekTo(player.getDuration());
        }
    }

    private void backward(){
        if((player.getCurrentPosition()-SEEK_TIME)>0){
            player.seekTo(player.getCurrentPosition()-SEEK_TIME);
        }else{
            player.seekTo(0);
        }
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            int current = player.getCurrentPosition();
            seekBar.setProgress(current);
            tvCurrentTime.setText(getFormat(current));
            tvFinalTime.setText(getFormat(player.getDuration()));
            myHandler.postDelayed(this, 100);
        }
    };

    private void startNewActivity(){
        Intent intent = new Intent(this, StorageList.class);
        myHandler.removeCallbacks(UpdateSongTime);
        player.release();
        player = null;
        startActivity(intent);
    }

    private String getFormat(int startTime)
    {
        String time = "";
        time = TimeUnit.MILLISECONDS.toMinutes((long) startTime)+":"+(TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                        startTime)));
        return time;
    }

    private MappedByteBuffer loadModelFile() throws IOException{
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("modelTeachable800.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long decLen = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, decLen);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (v.getId() == R.id.btnRecord) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                start = System.currentTimeMillis();
                wr.startRecording();
                Toast.makeText(v.getContext(), "Rec started....", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP) {
                finish = System.currentTimeMillis();
                wr.stopRecording();
                Toast.makeText(v.getContext(), "Rec finished!", Toast.LENGTH_SHORT).show();
                if((finish - start)<1000){
                    Toast.makeText(v.getContext(), "Too short!", Toast.LENGTH_SHORT).show();
                    return true;
                }
                try {
                    float[][] buff = jLibrosa.loadAndReadStereo(filepath + "/record.wav", 16000, 1);
                    float[][] mfccValues = jLibrosa.generateMFCCFeatures(buff[0], 16000, 13);
                    predictLabel(mfccValues);
                } catch (IOException | FileFormatNotSupportedException | WavFileException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }

    private void predictLabel(float[][] mfccValues){
        DecimalFormat df = new DecimalFormat("#.##");
        float[][][][] input = new float[1][32][13][1];

        for(int i=0; i<32;i++){
            for (int j=0;j<13;j++)
            {
                input[0][i][j][0]=mfccValues[j][i];
            }
        }
        float[][] output = new float[1][6];
        interpreter.run(input, output);
        Log.d("TEST", "******** - PREDICTIONS - *****");
        for(int i=0;i<6;i++)
            Log.d("TEST", modelLabels[i]+" - " + df.format((output[0][i] * 100))+ " %");
        predictedLabel = getLabelString(output);
        Log.d("TEST", "Predicted word =>  " + predictedLabel);
        micAction(predictedLabel.toLowerCase());
    }

    private String getLabelString(float[][] modelOutput){
        float max = 0.0f;
        int position = 0;
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < 6; j++) {
                if (modelOutput[i][j] > max)
                {
                    max = modelOutput[i][j];
                    position = j;
                }
            }
        }
        return modelLabels[position];
    }

    private void micAction(String label){
        switch(label){
            case "backward":
                backward();
                break;
            case "forward":
                forward();
                break;
            case "next":
                selectSong(SELECT_NEXT);
                break;
            case "previous":
                selectSong(SELECT_PREVIOUS);
                break;
            case "play":
                if (startStop == 1)
                    start();
                break;
            case "stop":
                if (startStop == 0)
                    start();
                break;
        }
    }
}