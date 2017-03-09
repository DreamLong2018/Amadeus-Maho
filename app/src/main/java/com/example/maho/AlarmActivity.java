package com.example.maho.amadeus;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import android.os.Vibrator;

public class AlarmActivity extends AppCompatActivity {
    ImageView connect, cancel;
    TextView status;
    AnimationDrawable logo;
    ImageView imageViewLogo;
    Boolean isPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        connect = (ImageView) findViewById(R.id.imageView_connect);
        status = (TextView) findViewById(R.id.textView_incoming);
        imageViewLogo = (ImageView) findViewById(R.id.imageView_logo);
        imageViewLogo.setImageResource(R.drawable.logo_animation);
        logo = (AnimationDrawable) imageViewLogo.getDrawable();
        logo.start();

        final MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.ringinggateofsteiner);
        mediaPlayer.start();

        final Vibrator v = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);
        v.vibrate(100000);


        connect.setImageResource(R.drawable.connect_unselect);

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPressed) {
                    try {
                        isPressed = true;
                        v.cancel();
                        mediaPlayer.stop();
                        MediaPlayer m = MediaPlayer.create(getApplicationContext(), R.raw.mute);

                        connect.setImageResource(R.drawable.connect_select);

                        m.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mp.start();
                            }
                        });

                        m.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mp.release();
                                Intent intent = new Intent(AlarmActivity.this, MainActivity.class);
                                startActivity(intent);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        }
                }
            }
        });

        imageViewLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(settingIntent);
            }
        });
    }
}