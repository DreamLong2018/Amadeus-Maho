package com.example.maho.amadeus;

/*
 * Big thanks to https://github.com/RIP95 aka Emojikage
 */

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    final String TAG = "Amadeus";
    final int REQUEST_PERMISSION_RECORD_AUDIO = 1;
    TextView subtitles;
    ImageView kurisu, alarm;
    AnimationDrawable animation;
    Handler handler;
    Boolean isLoop = false;
    Boolean isSpeaking = false;
    ArrayList<VoiceLine> voiceLines = new ArrayList<>();
    int shaman_girls = -1;
    Random randomgen = new Random();
    SharedPreferences sharedPreferences;
    private SpeechRecognizer sr;
    Boolean isPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        kurisu = (ImageView) findViewById(R.id.imageView_kurisu);
        kurisu.setImageResource(R.drawable.kurisu9a);
        alarm = (ImageView) findViewById(R.id.imageView_logo_small);
        alarm.setImageResource(R.drawable.amadeus_icon_smaller);
        subtitles = (TextView) findViewById(R.id.textView_subtitles);
        ImageView imageViewSubtitles = (ImageView) findViewById(R.id.imageView_subtitles);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (!sharedPreferences.getBoolean("show_subtitles", false)) {
            imageViewSubtitles.setVisibility(View.INVISIBLE);
        }

        handler = new Handler();
        setupLines();
        speak(voiceLines.get(0));

        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION_RECORD_AUDIO);
        }

        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new listener());

        final Runnable loop = new Runnable() {
            @Override
            public void run() {
                if (isLoop) {
                    speak(voiceLines.get(randomgen.nextInt(voiceLines.size())));
                    handler.postDelayed(this, 5000 + randomgen.nextInt(5) * 1000);
                }
            }
        };

        alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPressed) {
                    try {
                        isPressed = true;
                        MediaPlayer m = MediaPlayer.create(getApplicationContext(), R.raw.mute);

                        alarm.setImageResource(R.drawable.amadeus_icon_smaller);

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
                                Intent intent = new Intent(MainActivity.this, AlarmActivity.class);
                                startActivity(intent);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        kurisu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= 23) {
                    MainActivity host = (MainActivity) view.getContext();

                    int permissionCheck = ContextCompat.checkSelfPermission(host,
                            Manifest.permission.RECORD_AUDIO);

                    /* Input while loop producing bugs and mixes with output */
                    if (!isLoop && !isSpeaking) {
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            promptSpeechInput();
                        } else {
                            speak(new VoiceLine(R.raw.daga_kotowaru, Mood.EXCITED, R.string.line_but_i_refuse));
                        }
                    }

                } else if (!isLoop && !isSpeaking) {
                    promptSpeechInput();
                }
            }
        });


        kurisu.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!isLoop && !isSpeaking) {
                    isLoop = true;
                    handler.post(loop);
                } else {
                    handler.removeCallbacks(loop);
                    isLoop = false;
                }
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (sr != null)
            sr.destroy();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        isLoop = false;
        super.onStop();
    }

    @Override
    protected void onPause() {
        isLoop = false;
        super.onPause();
    }


    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");

        sr.startListening(intent);
    }

    public void speak(VoiceLine line) {
        try {
            MediaPlayer m = MediaPlayer.create(getApplicationContext(), line.getId());
            final Visualizer v = new Visualizer(m.getAudioSessionId());

            if (sharedPreferences.getBoolean("show_subtitles", false)) {
                subtitles.setText(line.getSubtitle());
            }

            Resources res = getResources();
            animation = (AnimationDrawable) Drawable.createFromXml(res, res.getXml(line.getMood()));

            if (m.isPlaying()) {
                m.stop();
                m.release();
                v.setEnabled(false);
                m = new MediaPlayer();
            }

            m.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    isSpeaking = true;
                    mp.start();
                    v.setEnabled(true);
                }
            });

            m.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isSpeaking = false;
                    mp.release();
                    v.setEnabled(false);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            kurisu.setImageDrawable(animation.getFrame(0));
                        }
                    });
                }
            });


            v.setEnabled(false);
            v.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            v.setDataCaptureListener(
                    new Visualizer.OnDataCaptureListener() {
                        public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                            int sum = 0;
                            for (int i = 1; i < bytes.length; i++) {
                                sum += bytes[i] + 128;
                            }
                            // The normalized volume
                            final float normalized = sum / (float) bytes.length;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (normalized > 50) {
                                        // Todo: Maybe choose sprite based on previous choice and volume instead of random
                                        kurisu.setImageDrawable(animation.getFrame((int) Math.ceil(Math.random() * 2)));
                                    } else {
                                        kurisu.setImageDrawable(animation.getFrame(0));
                                    }
                                }
                            });
                        }

                        public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, true, false);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void answerSpeech(String input) {
        List<String> greeting = Arrays.asList("ハロー", "おはよう", "こんにちは", "こんばんは");
        Log.e(TAG, input);
        Random randomGen = new Random();
        if (input.contains("Lolly") || input.contains("maho") || input.contains("child") || input.contains("kid") || input.contains("short") || input.contains("midget")) {
            switch (randomGen.nextInt(5)) {
                case 0:
                    speak(voiceLines.get(2));
                    break;
                case 1:
                    speak(voiceLines.get(3));
                    break;
                case 2:
                    speak(voiceLines.get(4));
                    break;
                case 3:
                    speak(voiceLines.get(5));
                    break;
                case 4:
                    speak(voiceLines.get(6));
                    break;
                case 5:
                    speak(voiceLines.get(7));
                    break;
            }
        } else if (input.contains("hello") || input.contains("good morning") || input.contains("hi") || input.contains("hey")) {
            switch (randomGen.nextInt(4)) {
                case 0:
                    speak(voiceLines.get(0));
                    break;
                case 1:
                    speak(voiceLines.get(1));
                    break;
                case 2:
                    speak(voiceLines.get(8));
                    break;
                case 3:
                    speak(voiceLines.get(9));
                    break;
            }
        } else if (input.contains("Dio")) {
            shaman_girls += 1;
            if (shaman_girls < 5) {
                switch (randomGen.nextInt(1)) {
                    case 0:
                        speak(voiceLines.get(10));
                        break;
                }
            } else {
                switch (shaman_girls) {
                    case 5:
                        speak(new VoiceLine(R.raw.kono_na, Mood.ANGRY, R.string.line_Leskinen_awesome));
                        break;
                    case 6:
                        speak(new VoiceLine(R.raw.something_slap, Mood.ANGRY, R.string.line_Leskinen_nice));
                        break;
                    case 7:
                        speak(new VoiceLine(R.raw.jojo, Mood.SMUG, R.string.line_Leskinen_oh_no));
                        break;
                    case 8:
                        speak(new VoiceLine(R.raw.kono_dio_da, Mood.SMUG, R.string.line_Leskinen_shaman));
                        break;
                    case 9:
                        speak(new VoiceLine(R.raw.muda, Mood.ANGRY, R.string.line_Leskinen_holy_cow));
                        shaman_girls = 0;
                        break;
                }
            }
        } else if (input.contains("nice") || input.contains("good") || input.contains("we did it")) {
            switch (randomGen.nextInt(3)) {
                case 0:
                    speak(voiceLines.get(14));
                    break;
                case 1:
                    speak(voiceLines.get(15));
                    break;
                case 2:
                    speak(voiceLines.get(16));
                    break;
            }
        } else {
            speak(voiceLines.get(9 + randomGen.nextInt(3)));
        }

    }

    private void setupLines() {
        voiceLines.add(new VoiceLine(R.raw.hello, Mood.HAPPY, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.welcome_back, Mood.HAPPY, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.i_get_this_everywhere_about_age, Mood.ANNOYED, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.im_an_adult, Mood.ANGRY, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.who_are_you_calling_a_loli, Mood.ANGRY, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.what_you_are_not_talking_to_me_are_you, Mood.ANGRY, R.string.line_hello));//5
        voiceLines.add(new VoiceLine(R.raw.you_must_be_amazed, Mood.SIDED_WORRIED, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.i_know_that, Mood.ANNOYED, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.ill_answer_anything, Mood.WINKING, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.my_names_maho, Mood.EXCITED, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.huh, Mood.DISAPPOINTED, R.string.line_hello));//10
        voiceLines.add(new VoiceLine(R.raw.whats_that_look, Mood.DISAPPOINTED, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.u_say_something, Mood.DISAPPOINTED, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.huh, Mood.SIDED_WORRIED, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.we_did_it, Mood.WINKING, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.thanks, Mood.HAPPY, R.string.line_hello));//15
        voiceLines.add(new VoiceLine(R.raw.got_that_right, Mood.WINKING, R.string.line_hello));
        voiceLines.add(new VoiceLine(R.raw.sorry1, Mood.SAD, R.string.line_hello));
    }

    private class Mood {
        static final int HAPPY = R.drawable.kurisu_9;
        static final int EXCITED = R.drawable.kurisu_6;
        static final int ANNOYED = R.drawable.kurisu_7;
        static final int ANGRY = R.drawable.kurisu_10;
        /* TODO: How should we name this mood?.. */
        static final int BLUSH = R.drawable.kurisu_12;
        static final int SAD = R.drawable.kurisu_3;
        static final int NORMAL = R.drawable.kurisu_2;
        static final int SLEEPY = R.drawable.kurisu_1;
        static final int WINKING = R.drawable.kurisu_5;
        static final int DISAPPOINTED = R.drawable.kurisu_8;
        static final int SMUG = R.drawable.kurisu_4;
        static final int SIDED_WORRIED = R.drawable.kurisu_15;
        static final int SIDED_NORMAL = R.drawable.kurisu_17;
    }

    private class listener implements RecognitionListener {
        final String TAG = "Amadeus.listener";

        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
        }

        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
        }

        public void onRmsChanged(float rmsdB) {
            Log.d(TAG, "onRmsChanged");
        }

        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived");
        }

        public void onEndOfSpeech() {
            Log.d(TAG, "onEndofSpeech");
        }

        public void onError(int error) {
            Log.d(TAG, "error " + error);
            sr.cancel();
            speak(voiceLines.get(13));
        }

        public void onResults(Bundle results) {
            String input = "";
            Log.d(TAG, "onResults " + results);
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            input += data.get(0);
            answerSpeech(input);
        }

        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults");
        }

        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent " + eventType);
        }

    }

}
