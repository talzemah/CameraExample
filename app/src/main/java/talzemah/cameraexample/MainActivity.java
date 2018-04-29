package talzemah.cameraexample;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final int TIME_INTERVAL_BETWEEN_IMAGES = 4000;

    private Preview preview;
    private Timer cameraTimer;
    private Camera camera;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // todo check if necessary ??
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        preview = new Preview(this, (SurfaceView) findViewById(R.id.surfaceView));
        preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ((FrameLayout) findViewById(R.id.layout)).addView(preview);

        // todo check if necessary ??
        preview.setKeepScreenOn(true);


        preview.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                takePictureContinuously();
            }
        });

        Toast.makeText(this, getString(R.string.take_photo_help), Toast.LENGTH_LONG).show();
    }

    private void takePictureContinuously() {
        cameraTimer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                camera.takePicture(shutterCallback, rawCallback, jpegCallback);
            }
        };

        cameraTimer.scheduleAtFixedRate(task, 0, TIME_INTERVAL_BETWEEN_IMAGES);
    }

    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
            // Log.d(TAG, "onShutter");
        }
    };

    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            // Log.d(TAG, "onPictureTaken - raw");
        }
    };

    // Invoke when jpeg image is ready.
    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

            new SaveImageTask().execute(data);
            resetCamera();
            Log.d(TAG, "onPictureTaken - jpeg");
        }
    };

    private void resetCamera() {
        camera.startPreview();
        preview.setCamera(camera);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If there is more than one camera, select the rear-camera
        int numCams = Camera.getNumberOfCameras();
        if (numCams > 0) {
            try {
                camera = Camera.open(0);
                camera.startPreview();
                preview.setCamera(camera);

            } catch (RuntimeException ex) {
                Toast.makeText(this, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop the timer from take picture continuously.
        if (cameraTimer != null) {
            cameraTimer.cancel();
            cameraTimer = null;
        }

        // Stop the camera activity.
        if (camera != null) {
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera = null;
        }
    }


    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {

            FileOutputStream outputStream;

            // Write data (jpeg image) to SD Card.
            try {
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                File appDirectory = new File(externalStorageDirectory.getAbsolutePath() + "/CameraExample");
                appDirectory.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File imageFile = new File(appDirectory, fileName);

                outputStream = new FileOutputStream(imageFile);
                outputStream.write(data[0]);
                outputStream.flush();
                outputStream.close();

                Log.d(TAG, "Save image, write [" + data.length + "] bytes to: " + imageFile.getAbsolutePath());

                refreshGallery(imageFile);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }

        // Show the capture image in gallery.
        private void refreshGallery(File file) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(file));
            sendBroadcast(mediaScanIntent);

            Log.d(TAG, "Add image to gallery");
        }
    }

} // End activity.