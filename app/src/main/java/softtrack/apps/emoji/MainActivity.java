package softtrack.apps.emoji;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    public ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    public Task<Text> result;
    public Button startBtn;
    public PreviewView previewView;
    public ImageView activeEmoji;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{ Manifest.permission.CAMERA }, PackageManager.PERMISSION_GRANTED);
        initialize();
//        startBtn = findViewById(R.id.startBtn);
//        startBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                initialize();
//            }
//        });

    }

    public void initialize() {
        previewView = findViewById(R.id.previewView);
        activeEmoji = findViewById(R.id.activeEmoji);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getApplicationContext()),  new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
                        if (mediaImage != null) {
                            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                            detectFaces();
                        }
                        imageProxy.close();
                    }
                });
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(((LifecycleOwner) this), cameraSelector, preview, imageCapture, imageAnalysis);
            } catch(ExecutionException | InterruptedException e){

            }
        }, ContextCompat.getMainExecutor(MainActivity.this));
    }

    public void detectFaces() {
        FaceDetector faceDetector = new
                FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                .build();
        if(!faceDetector.isOperational()){
            new AlertDialog.Builder(this).setMessage("Could not set up the face detector!").show();
            return;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable=true;

        Bitmap myBitmap = previewView.getBitmap();

        Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
        SparseArray<Face> faces = faceDetector.detect(frame);
        Log.d("debug", "Num faces = " + faces.size());
        if (faces.size() >= 1) {
            activeEmoji.setVisibility(View.VISIBLE);
        } else {
             activeEmoji.setVisibility(View.GONE);
        }
    }

}