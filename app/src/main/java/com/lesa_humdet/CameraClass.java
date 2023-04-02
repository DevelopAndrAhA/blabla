package com.lesa_humdet;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
/*import com.google.mlkit.vision.face.FaceDetectorOptions;*/
import com.lesa_humdet.tflite.AutoFitTextureView;
import com.lesa_humdet.tflite.BorderedText;
import com.lesa_humdet.tflite.ImageUtils;
import com.lesa_humdet.tflite.MultiBoxTracker;
import com.lesa_humdet.tflite.OverlayView;
import com.lesa_humdet.tflite.SimilarityClassifier;
import com.lesa_humdet.tflite.TFLiteObjectDetectionAPIModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.content.Context.WINDOW_SERVICE;


public class CameraClass{
    SharedPreferences mSettings;
    SharedPreferences.Editor editor;

    ConstraintLayout layout = null;
    WindowManager windowManager = null;
    AutoFitTextureView texture;
    String TAG = "MyService";
    private final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private Size imageDimension;
    protected CaptureRequest.Builder captureRequestBuilder;
    private String cameraId = "0";
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    private ImageReader imageReader;

    private Handler mBackgroundHandler;


    // Face detector
    private FaceDetector faceDetector;
    OverlayView trackingOverlay;
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private final float TEXT_SIZE_DIP = 10;
    private BorderedText borderedText;
    private MultiBoxTracker tracker;
    private SimilarityClassifier detector;
    private final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";
    private final int TF_OD_API_INPUT_SIZE = 112;
    private final boolean TF_OD_API_IS_QUANTIZED = false;
    private Integer sensorOrientation;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap portraitBmp = null;
    private Bitmap faceBmp = null;
    private Matrix frameToCropTransform;
    private  final boolean MAINTAIN_ASPECT = false;
    private Matrix cropToFrameTransform;
    private  final Size previewSize = new Size(640, 480);
    private int[] rgbBytes = null;
    private byte[][] yuvBytes = new byte[3][];
    private int yRowStride;
    private Runnable imageConverter;
    private Runnable postInferenceCallback;
    private long timestamp = 0;
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean computingDetection = false;


    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private String[] array;
    Conf conf = new Conf();
    double lat=0,lng=0;
    SaveNewFace saveNewFace = null;
    CameraManager manager=null;
    CameraCharacteristics characteristics = null;
    Context context;
    public CameraClass(Context context) {
        this.context = context;
    }

    public void start() {
        mSettings = context.getSharedPreferences(new Conf().getShared_pref_name(), Context.MODE_PRIVATE);
        editor = mSettings.edit();
        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        layout = (ConstraintLayout) inflater.inflate(R.layout.camera_view, null);
        texture = (AutoFitTextureView) layout.findViewById(R.id.texture);
        trackingOverlay = (OverlayView) layout.findViewById(R.id.tracking_overlay);
        int lang = mSettings.getInt(conf.getLANG(), 2);
        if (lang == conf.getRU()) {
            array = context.getResources().getStringArray(R.array.app_lang_ru);
        } else if (lang == conf.getEN()) {
            array = context.getResources().getStringArray(R.array.app_lang_en);
        } else if (lang == conf.getAR()) {
            array = context.getResources().getStringArray(R.array.app_lang_ar);
        }
        saveNewFace = new SaveNewFace(context);
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, final int width, final int height) {
                openCamera(cameraId);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

            }

        });
        layout.setOnTouchListener(new View.OnTouchListener() {
                                      float dX, dY;

                                      @Override
                                      public boolean onTouch(View view, MotionEvent motionEvent) {

                                          switch (motionEvent.getAction()) {

                                              case MotionEvent.ACTION_DOWN:

                                                  dX = view.getX() - motionEvent.getRawX();
                                                  dY = view.getY() - motionEvent.getRawY();
                                                  break;

                                              case MotionEvent.ACTION_MOVE:

                                                  view.animate()
                                                          .x(motionEvent.getRawX() + dX)
                                                          .y(motionEvent.getRawY() + dY)
                                                          .setDuration(0)
                                                          .start();
                                                  break;
                                              default:
                                                  return false;
                                          }
                                          return false;
                                      }
                                  }
        );
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                // Make the underlying application window visible through any transparent parts
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.RIGHT;
        p.dimAmount = (float) 0.0;
        windowManager.addView(layout, p);
        Button button2 = layout.findViewById(R.id.button2);
        button2.setText(array[0]);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout.setVisibility(View.GONE);
            }
        });
        /*FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();


        FaceDetector detector = FaceDetection.getClient(options);

        faceDetector = detector;*/


        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(location!=null){
                    lat = location.getLatitude();
                    lng = location.getLongitude();
                }
            }
            @Override
            public void onProviderDisabled(String provider) {
            }
            @Override
            public void onProviderEnabled(String provider) {
            }
            @Override
            public void onStatusChanged(String provider, int status,Bundle extras) {
            }
        });
    }

    LocationManager locationManager = null;




    private void openCamera(String cameraId) {
        manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            onPreviewSizeChosen(previewSize,90);
            manager.openCamera(cameraId, stateCallback, null);
            jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
        } catch (CameraAccessException e) {
            layout.setVisibility(View.GONE);
            Log.e(TAG, e.toString()+"openCamera [openCamera]");
        }
    }


    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            try{
                cameraDevice.close();
                cameraDevice = null;
            }catch (Exception e){
                Log.e(TAG, e.toString()+"[CameraDevice.StateCallback]");
            }
        }
    };
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = this.texture.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);


            backgroundThread = new HandlerThread("ImageListener");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
            mBackgroundHandler = backgroundHandler;
            imageReader =
                    ImageReader.newInstance(
                            640, 480, ImageFormat.YUV_420_888, 2);



            imageReader.setOnImageAvailableListener(readerListener, backgroundHandler);
            captureRequestBuilder.addTarget(imageReader.getSurface());



            cameraDevice.createCaptureSession(
                    Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            //The camera is already closed
                            if (null == cameraDevice) {
                                return;
                            }
                            // When the session is ready, we start displaying the preview.
                            cameraCaptureSessions = cameraCaptureSession;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(context, "Configuration change", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (Exception e) {
            Log.e(TAG, e.toString()+"createCameraPreview");
        }
    }
    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.toString()+"updatePreview");
            String str = e.toString();
            if(str.contains("CameraAccessException: CAMERA_ERROR (3)")){
                layout.setVisibility(View.GONE);
            }

        }
    }

    public void setContext(Context context) {
        this.context = context;
    }
    Size[] jpegSizes = null;
    int width = 640;
    int height = 480;

    protected void takePicture(long currTimestamp) throws CameraAccessException {
        if (jpegSizes != null && jpegSizes.length > 0) {
            width = jpegSizes[0].getWidth();
            height = jpegSizes[0].getHeight();
        }

        ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        List<Surface> outputSurfaces = new ArrayList<>();
        outputSurfaces.add(reader.getSurface());
        outputSurfaces.add(new Surface(texture.getSurfaceTexture()));
        CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(reader.getSurface());
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation) + 90);
        captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
        File dir = new File(Environment.getExternalStorageDirectory() + File.separator + "DCIM" + File.separator + "HUMDET");
        if (!dir.exists()) {
            dir.mkdir();
        }
        final File file = new File(dir, currTimestamp + ".jpg");
        ImageReader.OnImageAvailableListener readerListener = (r) -> {
            Image image = r.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            try {
                save(bytes, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            image.close();
        };
        reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
        try {
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updateResults(long currTimestamp, final List<SimilarityClassifier.Recognition> mappedRecognitions, Bitmap crop) {
        try {
            tracker.trackResults(mappedRecognitions, currTimestamp);
            trackingOverlay.postInvalidate();
            computingDetection = false;

            if (!mappedRecognitions.isEmpty()) {
                SimilarityClassifier.Recognition rec = mappedRecognitions.get(0);
                if (rec.getExtra() != null && (rec.getTitle() == null || rec.getTitle().isEmpty())) {
                    detector.register(currTimestamp + "", rec);
                    String dateStr = new Date().toLocaleString().split(" ")[0];
                    if (!rec.getDate().equals(dateStr) && rec.getDate().isEmpty()) {
                        if (crop != null) {
                            ExecutorService executor = Executors.newFixedThreadPool(2);
                            executor.submit(() -> {
                                try {
                                    saveNewFace.setLat(lat);
                                    saveNewFace.setLng(lng);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                saveNewFace.setUsername(currTimestamp + "");
                                saveNewFace.setTitle(currTimestamp + "");
                                float mas[][] = (float[][]) rec.getExtra();
                                StringBuilder masToSend = new StringBuilder();
                                for (int i = 0; i < mas[0].length; i++) {
                                    masToSend.append(mas[0][i]);
                                    if (i != mas[0].length - 1) {
                                        masToSend.append(",");
                                    }
                                }
                                saveNewFace.setCrop(masToSend.toString());
                            });
                            /*executor.submit(() -> {
                                try {
                                    takePicture(currTimestamp);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            });
                            executor.shutdown();*/
                        }
                    } else if (rec.getDate().isEmpty()) {
                        if (crop != null) {
                            /*ExecutorService executor = Executors.newSingleThreadExecutor();
                            executor.submit(() -> {
                                try {
                                    takePicture(currTimestamp);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            });
                            executor.shutdown();*/
                        }
                    }
                } else if (rec.getExtra() != null) {
                    detector.register(currTimestamp + "", rec);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void save(byte[] bytes, File file) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        bitmap = resizeImage(bitmap, 640, true);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] cropByteArray = stream.toByteArray();
        OutputStream output = new FileOutputStream(file);
        output.write(cropByteArray);
        output.close();
        String path = compressImage(file.getAbsolutePath());
        File tmpFile  = new File(path);
        saveNewFace.setLargePohto(tmpFile);
        if(isOnline()){
            saveNewFace.execute();
        }else{
            Toast.makeText(context,array[35],Toast.LENGTH_LONG).show();
        }
    }



    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(context);


        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            context.getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (final Exception e) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            previewWidth = size.getWidth();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            previewHeight = size.getHeight();
        }

        sensorOrientation = rotation - getScreenOrientation();
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.RGB_565);//RGB_565 ARGB_8888


        int targetW, targetH;
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth;
            targetW = previewHeight;
        }
        else {
            targetW = previewWidth;
            targetH = previewHeight;
        }
        int cropW = (int) (targetW / 2.0);
        int cropH = (int) (targetH / 2.0);

        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.RGB_565);

        portraitBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565);
        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.RGB_565);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropW, cropH,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);


        Matrix frameToPortraitTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        targetW, targetH,
                        sensorOrientation, MAINTAIN_ASPECT);



        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        String str = tracker.draw(canvas);
                        if(str!=null){
                            //То снимок и шлем на сервак
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    protected int getScreenOrientation() {
        switch (windowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }



    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                Log.d("Buffer at size", i+ buffer.capacity()+"");
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }


    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }


    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    public synchronized void onResume() {
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }
    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }


    final List<SimilarityClassifier.Recognition> mappedRecognitions = new LinkedList<SimilarityClassifier.Recognition>();

    private void takePicture(boolean isPortrait) {

        // Создаем объект для обработки изображения
        ImageReader imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        SurfaceTexture texture = new SurfaceTexture(0);
        // Устанавливаем размеры SurfaceTexture для отображения превью
        texture.setDefaultBufferSize(previewWidth, previewHeight);

        // Создаем Surface для отображения превью
        Surface surface = new Surface(texture);

        try {
            // Создаем объект запроса захвата изображения
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            // Устанавливаем Surface для отображения превью и Surface для обработки изображения
            captureBuilder.addTarget(surface);
            captureBuilder.addTarget(imageReader.getSurface());

            // Устанавливаем автоматический режим управления камерой
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Устанавливаем ориентацию изображения
            int rotation = windowManager.getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            // Создаем сессию захвата изображения
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        // Запускаем захват изображения
                        session.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Ошибка создания сессии захвата изображения");
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    private boolean onFacesDetected(long currTimestamp, List<Face> faces) {
        Thread recThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                final Canvas canvas = new Canvas(cropCopyBitmap);
                final Paint paint = new Paint();
                paint.setColor(Color.TRANSPARENT);
                paint.setStyle(Style.STROKE);
                paint.setStrokeWidth(2.0f);

                int sourceW = rgbFrameBitmap.getWidth();
                int sourceH = rgbFrameBitmap.getHeight();
                int targetW = portraitBmp.getWidth();
                int targetH = portraitBmp.getHeight();
                Matrix transform = createTransform(
                        sourceW,
                        sourceH,
                        targetW,
                        targetH,
                        sensorOrientation);
                final Canvas cv = new Canvas(portraitBmp);
                cv.drawBitmap(rgbFrameBitmap, transform, null);
                final Canvas cvFace = new Canvas(faceBmp);

                for (Face face : faces) {
                    final RectF boundingBox = new RectF(face.getBoundingBox());
                    final boolean goodConfidence = true;
                    if (boundingBox != null && goodConfidence) {
                        cropToFrameTransform.mapRect(boundingBox);
                        RectF faceBB = new RectF(boundingBox);
                        transform.mapRect(faceBB);
                        cv.drawRect(faceBB, paint);
                        float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                        float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                        Matrix matrix = new Matrix();
                        matrix.postTranslate(-faceBB.left, -faceBB.top);
                        matrix.postScale(sx, sy);
                        cvFace.drawBitmap(portraitBmp, matrix, null);
                        canvas.drawRect(faceBB, paint);
                        final String[] label = {""};
                        final float[] confidence = {-1f};
                        final Integer[] color = {Color.BLUE};
                        final Object[] extra = {null};
                        Bitmap crop = null;
                        try{
                            crop = Bitmap.createBitmap(portraitBmp,
                                    (int) faceBB.left,
                                    (int) faceBB.top,
                                    (int) faceBB.width(),
                                    (int) faceBB.height());
                        }catch (Exception e){e.printStackTrace();}
                        if(crop!=null){
                            final long startTime = SystemClock.uptimeMillis();
                            final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmp, true);
                            if (resultsAux.size() > 0) {
                                SimilarityClassifier.Recognition result = resultsAux.get(0);
                                extra[0] = result.getExtra();
                                float conf = result.getDistance();
                                if (conf < 1.0f) {
                                    confidence[0] = conf;
                                    label[0] = result.getTitle();
                                    if (result.getId().equals("0")) {
                                        color[0] = Color.TRANSPARENT;
                                    }else {
                                        color[0] = Color.TRANSPARENT;
                                    }
                                }
                            }
                            final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition("0", label[0], confidence[0], boundingBox,"");
                            result.setColor(color[0]);
                            result.setExtra(extra[0]);
                            result.setCrop(croppedBitmap);
                            result.setLocation(boundingBox);
                            mappedRecognitions.add(result);
                        }
                    }
                }
                updateResults(currTimestamp, mappedRecognitions, Bitmap.createBitmap(cropCopyBitmap));
            }

        });
        recThread.start();

        return true;
    }

    private Matrix createTransform(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);
            matrix.postRotate(applyRotation);
        }
        if (applyRotation != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }
        return matrix;
    }

    ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(final ImageReader reader) {
            if (previewWidth == 0 || previewHeight == 0) {
                return;
            }
            if (rgbBytes == null) {
                rgbBytes = new int[previewWidth * previewHeight];
            }
            try {
                final Image image = reader.acquireLatestImage();
                if (image == null) {
                    return;
                }
                final Image.Plane[] planes = image.getPlanes();
                fillBytes(planes, yuvBytes);
                yRowStride = planes[0].getRowStride();
                final int uvRowStride = planes[1].getRowStride();
                final int uvPixelStride = planes[1].getPixelStride();

                imageConverter =
                        new Runnable() {
                            @Override
                            public void run() {
                                ImageUtils.convertYUV420ToARGB8888(
                                        yuvBytes[0],
                                        yuvBytes[1],
                                        yuvBytes[2],
                                        previewWidth,
                                        previewHeight,
                                        yRowStride,
                                        uvRowStride,
                                        uvPixelStride,
                                        rgbBytes);
                            }
                        };

                postInferenceCallback =
                        new Runnable() {
                            @Override
                            public void run() {
                                image.close();
                            }
                        };

                onResume();
            } catch (final Exception e) {
                return;
            }
        }
    };
    final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(context, array[20], Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };



    public Bitmap resizeImage(Bitmap realImage, float maxImageSize,boolean filter) {
        float ratio = Math.min((float) maxImageSize / realImage.getWidth(),(float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());
        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,height, filter);
        return newBitmap;
    }
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }




    public String compressImage(String imageUri) {

        String filePath = getRealPathFromURI(imageUri);
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

//      max Height and width values of the compressed image is taken as 816x612

        float maxHeight = 816.0f;
        float maxWidth = 612.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

        options.inJustDecodeBounds = false;

        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        try {
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            Log.e(TAG, e.toString()+"[compressImage]");
        }

        FileOutputStream out = null;
        String filename = getFilename();
        try {
            out = new FileOutputStream(filename);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);

        } catch (FileNotFoundException e) {
            Log.e(TAG, e.toString()+"[compressImage]");
        }
        File delFile = new File(filePath);
        delFile.delete();
        return filename;

    }

    public String getFilename() {
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "DCIM" + File.separator +"HUMDET");
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
        return uriSting;

    }

    private String getRealPathFromURI(String contentURI) {
        Uri contentUri = Uri.parse(contentURI);
        Cursor cursor = context.getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

}