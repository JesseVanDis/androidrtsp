package com.jessevandis.rtspserver;

import android.Manifest;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.pedro.encoder.input.video.Camera1ApiManager;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtplibrary.base.Camera1Base;
import com.pedro.rtsp.rtsp.VideoCodec;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;
import com.pedro.rtspserver.RtspServerCamera1;

import java.lang.reflect.Field;
import java.util.List;

/**
 * More documentation see:
 * {@link com.pedro.rtplibrary.base.Camera1Base}
 * {@link com.pedro.rtplibrary.rtsp.RtspCamera1}
 */
public class MainActivity extends AppCompatActivity
        implements ConnectCheckerRtsp, View.OnClickListener, SurfaceHolder.Callback {

    private RtspServerCamera1 m_rtspCamera1;
    private Button m_toggleRtspButton;
    private Button m_toggleTorchButton;
    private TextView m_rtspUrlLabel;
    private boolean m_torchEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_axample);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 100);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, 101);
        }

        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        m_toggleRtspButton = findViewById(R.id.b_start_stop);
        m_toggleRtspButton.setOnClickListener(this);

        m_toggleTorchButton = findViewById(R.id.toggle_torch);
        m_toggleTorchButton.setOnClickListener(v -> {
            toggleTorch();
        });

        Button switchCamera = findViewById(R.id.switch_camera);
        switchCamera.setOnClickListener(this);
        m_rtspUrlLabel = findViewById(R.id.tv_url);
        m_rtspCamera1 = new RtspServerCamera1(surfaceView, this, 8080);

        List<Camera.Size> videoSizes = m_rtspCamera1.getResolutionsBack();
        List<int[]> supportedFps = m_rtspCamera1.getSupportedFps();
        int b = m_rtspCamera1.getBitrate();
        int w =  m_rtspCamera1.getStreamWidth();
        int h =  m_rtspCamera1.getStreamHeight();

        /*
        Log.i("Video", "supported video sizes:");
        for(int i=0; i<videoSizes.size(); i++)
        {
            Log.i("Video", "width: " + videoSizes.get(i).width + ", height: " + videoSizes.get(i).height);
        }
        Log.i("Video", "supported fps:");
        for(int i=0; i<supportedFps.size(); i++)
        {
            for(int j=0; j<supportedFps.get(i).length; j++)
            {
                Log.i("Video", "at index " + i + " fps : " + supportedFps.get(i)[j]);
            }
        }

        m_rtspCamera1.prepareVideo(320, 240, 20, 1228800, 0);
        */


        m_rtspCamera1.setVideoCodec(VideoCodec.H265);
        m_rtspCamera1.setReTries(10);
        surfaceView.getHolder().addCallback(this);
    }

    private void toggleTorch()
    {
        try {
            Field cameraManagerField = Camera1Base.class.getDeclaredField("cameraManager");
            cameraManagerField.setAccessible(true);
            Camera1ApiManager cameraManager = (Camera1ApiManager)cameraManagerField.get(m_rtspCamera1);


            Field cameraField = Camera1ApiManager.class.getDeclaredField("camera");
            cameraField.setAccessible(true);
            android.hardware.Camera camera = (android.hardware.Camera)cameraField.get(cameraManager);
            android.hardware.Camera.Parameters parameters = camera.getParameters();

            if(m_torchEnabled)
            {
                parameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_OFF);
            }
            else
            {
                parameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_TORCH);
            }
            m_torchEnabled = !m_torchEnabled;
            camera.setParameters(parameters);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    @Override
    public void onConnectionStartedRtsp(@NotNull String rtspUrl) {
    }
    */

    @Override
    public void onConnectionSuccessRtsp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionFailedRtsp(final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (m_rtspCamera1.reTry(5000, reason)) {
                    Toast.makeText(MainActivity.this, "Retry", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(MainActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
                            .show();
                    m_rtspCamera1.stopStream();
                    m_toggleRtspButton.setText(R.string.start_button);
                }
            }
        });
    }

    @Override
    public void onNewBitrateRtsp(final long bitrate) {

    }

    @Override
    public void onDisconnectRtsp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthErrorRtsp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
                m_rtspCamera1.stopStream();
                m_toggleRtspButton.setText(R.string.start_button);
            }
        });
    }

    @Override
    public void onAuthSuccessRtsp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.b_start_stop:
                if (!m_rtspCamera1.isStreaming()) {
                    if (m_rtspCamera1.isRecording()
                            || m_rtspCamera1.prepareAudio() && m_rtspCamera1.prepareVideo()) {
                        m_toggleRtspButton.setText(R.string.stop_button);
                        m_rtspCamera1.startStream();
                        m_rtspUrlLabel.setText(m_rtspCamera1.getEndPointConnection());
                    } else {
                        Toast.makeText(this, "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    m_toggleRtspButton.setText(R.string.start_button);
                    m_rtspCamera1.stopStream();
                    m_rtspUrlLabel.setText("");
                }
                break;
            case R.id.switch_camera:
                try {
                    m_rtspCamera1.switchCamera();
                } catch (CameraOpenException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        m_rtspCamera1.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (m_rtspCamera1.isStreaming()) {
            m_rtspCamera1.stopStream();
            m_toggleRtspButton.setText(getResources().getString(R.string.start_button));
            m_rtspUrlLabel.setText("");
        }
        m_rtspCamera1.stopPreview();
    }
}
