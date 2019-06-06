/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.moodymusic;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.CAMERA_SERVICE;
import static androidx.constraintlayout.motion.utils.Oscillator.TAG;


public class FacialMoodDetectionFragment extends Fragment {

    private TextureView cameraPreview;
    private String cameraId;
    protected CameraDevice cameraFront;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    protected static String mood;
    private Button tpButton;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private Handler camBackgroundHandler;
    private HandlerThread camBackgroundThread;

    final FirebaseVisionFaceDetectorOptions highAccuracyOpts =
            new FirebaseVisionFaceDetectorOptions.Builder()
                    .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                    .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                    .build();

    FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(highAccuracyOpts);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View viewer = inflater.inflate(R.layout.fragment_facial_mood_detection, container, false);
        tpButton = viewer.findViewById(R.id.takePictureButton);
        tpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewer) {
                takePicture();
            }
        });
        return viewer;
    }

    //Issues with creating the preview after the fragment is loaded, tried to create it after view is loaded but
    //still not working
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        cameraPreview = view.findViewById(R.id.tView);
        cameraPreview.setSurfaceTextureListener(textureListener);
    }


    protected static int getRotationCompensation(String cameraId, Activity activity, Context context)
            throws CameraAccessException {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SENSOR_ORIENTATION);
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        int result;
        switch (rotationCompensation) {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                Log.e(TAG, "Bad rotation value: " + rotationCompensation);
        }
        return result;
    }

    private int getOrientation(int rotation) {
        return (ORIENTATIONS.get(rotation) + +270) % 360;
    }

    //Surface Texture Listener to create a preview for the camera via the texture layout in the corresponding
    // XML file. If the view is available, the camera opens
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    //Callback interface for camera functionality. When front camera is opened, attempt to create a preview session. If camera
    // is disconnected, close the front camera. On an error, close and nullify the front camera to reset.
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraFront = camera;
            createCameraPreviewSession();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraFront.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraFront.close();
            cameraFront = null;
        }
    };
    //Capture callback interface configured to recreate the camera preview session upon the successful capture of a picture
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            createCameraPreviewSession();
        }
    };

    //Method to open the camera. First check if camera permission is granted, and request it if not
    //Then, access the camera ([1] signifies the front camera), retrieve its characteristics to configure the stream config. map
    //necessary for creating a capture session. Image dimensions are set according to the output size to correctly fit the picture
    private void openCamera() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
        CameraManager manager = (CameraManager) this.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //Method to create a camera preview so that the user can see what facial expression they are making before taking
    // a picture with the application. Assign the surface texture that was deemed available in the initialization of the
    //texture listener. Create a capture request using the methods provided by the Google Samples files
    protected void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = cameraPreview.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraFront.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraFront.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == cameraFront) {
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                }


                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getContext(), "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //Modified version of the Google Samples code to take a picture if the camera is open and a preview session has
    //been successfully created. Image reader is fed basic parameters, as well as the prefered image format for emotion
    //detection in Firebase, with a maximum image number set to 1 to allow the proper functionality after a picture is
    //taken. 
    protected void takePicture() {
        if (null == cameraFront) {
            Log.e(TAG, "cameraFront is null");
            return;
        }
        CameraManager manager = (CameraManager) this.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            int width = 480;
            int height = 360;
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(cameraPreview.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraFront.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Set rotation of camera if necessary based on orientation retrieved
            final int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    image = reader.acquireLatestImage();
                    //Send image to determine method to analyze via firebase
                    determine(image);
                }
            };
            reader.setOnImageAvailableListener(readerListener, camBackgroundHandler);
            //recreate preview session so user can take a new picture if mood was incorrect, or has changed
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreviewSession();
                }
            };
            cameraFront.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, camBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, camBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //Method to determine emotion/mood detection via Firebase calls. Converts image to Firebase image for analysis
    //On successful conversion, image added to Firebase vision face list (Firebase allows for multiple face processing)
    //Image then analyzed and necessary probabilities computed to determine mood with 90% accuracy threshold.
    private void determine(Image image){
        FirebaseVisionImage image2 = null;
        try {
            image2 = FirebaseVisionImage.fromMediaImage(image, getRotationCompensation(cameraId, getActivity(), getContext()));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Task<List<FirebaseVisionFace>> result =
                detector.detectInImage(image2)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        for (FirebaseVisionFace face : faces) {

                                            float smileProb = face.getSmilingProbability();
                                            float rightEyeOpenProb = face.getRightEyeOpenProbability();

                                            if (smileProb > .90 && rightEyeOpenProb > .90) {
                                                mood = "Happy";
                                            } else if (smileProb < .90 && rightEyeOpenProb > .90) {
                                                mood = "Angry";
                                            } else if (smileProb < .90 && rightEyeOpenProb < .90) {
                                                mood = "Sad";
                                            }
                                            //Unable to link mood output to Spotify, so display mood instead
                                            new AlertDialog.Builder(getContext())
                                                    .setTitle("Mood Determined:")
                                                    .setMessage(mood)
                                                    .show();
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getContext(), "Mood was not Determined", Toast.LENGTH_SHORT).show();
                                    }
                                });
    }

    //background thread handlers for camera class to avoid crashing
    protected void startBackgroundThread() {
        camBackgroundThread = new HandlerThread("Camera Background");
        camBackgroundThread.start();
        camBackgroundHandler = new Handler(camBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        camBackgroundThread.quitSafely();
        try {
            camBackgroundThread.join();
            camBackgroundThread = null;
            camBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //On resume method included to avoid crashing when user switches between fragments
    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (cameraPreview.isAvailable()) {
            openCamera();
        } else {
            cameraPreview.setSurfaceTextureListener(textureListener);
        }
    }
    //On pause method included to avoid crashing when user switches between fragments
    @Override
    public void onPause() {
        stopBackgroundThread();
        super.onPause();
    }
    }
