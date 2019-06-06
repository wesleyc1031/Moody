/*
 * Copyright 2017 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.example.moodymusic;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.speech_to_text.v1.websocket.BaseRecognizeCallback;

import java.io.IOException;
import java.io.InputStream;

public class VocalMoodDetectionFragment extends Fragment {

    private MicrophoneHelper microphoneHelper;
    private MicrophoneInputStream capture;
    private boolean listening = false;
    SpeechToText speechService = initSpeechToTextService();
    ImageButton recordVoice;
    private String mood = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View viewer = inflater.inflate(R.layout.fragment_vocal_mood_detection, container, false);
        TextView textView = viewer.findViewById(R.id.voiceTextBox);
        recordVoice = viewer.findViewById(R.id.recordVoice);
        microphoneHelper = new MicrophoneHelper(getActivity());
        recordVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!listening) {
                    // Update the icon background
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recordVoice.setBackgroundColor(Color.GREEN);
                        }
                    });
                    capture = microphoneHelper.getInputStream(true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                speechService.recognizeUsingWebSocket(getRecognizeOptions(capture),
                                        new MicrophoneRecognizeDelegate());
                            } catch (Exception e) {
                                showError(e);
                            }
                        }
                    }).start();

                    listening = true;
                } else {
                    // Update the icon background
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recordVoice.setBackgroundColor(Color.LTGRAY);
                        }
                    });
                    microphoneHelper.closeInputStream();
                    listening = false;
                }
            }
        });
        return viewer;
    }

    //instantiates a new speech to text service including API Key provided via IBM
    //IAM options generates a new token for each user so we do not have to reauthenticate
    private SpeechToText initSpeechToTextService() {
        IamOptions options = new IamOptions.Builder()
                .apiKey("e4cMYVoDJWquCBRPhf0J8ZKnV5RUF96CfoL93nU-wFDa")
                .build();
        SpeechToText service = new SpeechToText(options);
        service.setEndPoint("https://stream.watsonplatform.net/speech-to-text/api");
        return service;
    }

    //Sets recognition options for transcription
    private RecognizeOptions getRecognizeOptions(InputStream captureStream) {
        return new RecognizeOptions.Builder()
                .audio(captureStream)
                .contentType(ContentType.OPUS.toString())
                .model("en-US_BroadbandModel")
                .interimResults(true)
                .inactivityTimeout(2000)
                .build();
    }

    //Display mood via text upon determination since we cannot send to Spotify for music streaming
    private void showOutput(final String text){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(getContext())
                        .setTitle("Mood Determined: ")
                        .setMessage(mood)
                        .show();
            }
        });
    }

    private void showError(Exception e) {
        e.printStackTrace();
    }
    
    //Class necessary for recognition and transcription. If results are not null the output is assigned to mood
    private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback {
        @Override
        public void onTranscription(SpeechRecognitionResults speechResults) {
            System.out.println(speechResults);
            if (speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
                mood = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                showOutput(mood);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recordVoice.setBackgroundColor(Color.LTGRAY);
                    }
                });
                microphoneHelper.closeInputStream();
                listening = false;
            }
        }

        @Override
        public void onError(Exception e) {
            try {
                // This is critical to avoid hangs
                // (see https://github.com/watson-developer-cloud/android-sdk/issues/59)
                capture.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            showError(e);
            enableMicButton();
        }

        //when recording is finished, re-enable microphone button for use
        @Override
        public void onDisconnected() {
            enableMicButton();
        }
    }
    //enables microphone for use
    private void enableMicButton() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recordVoice.setEnabled(true);
            }
        });
    }

}

