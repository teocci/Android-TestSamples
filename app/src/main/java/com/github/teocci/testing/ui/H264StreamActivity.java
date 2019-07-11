package com.github.teocci.testing.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.github.teocci.testing.R;
import com.github.teocci.testing.avcodec.AvcEncoder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-Jun-20
 */
public class H264StreamActivity extends Activity implements SurfaceHolder.Callback, PreviewCallback
{
    private final static String TAG = MainActivity.class.getSimpleName();

    private final static String SP_CAM_WIDTH = "cam_width";
    private final static String SP_CAM_HEIGHT = "cam_height";
    private final static String SP_DEST_IP = "dest_ip";
    private final static String SP_DEST_PORT = "dest_port";

    private final static int DEFAULT_FRAME_RATE = 30;
    private final static int DEFAULT_BIT_RATE = 500000;

    private Camera camera;
    private SurfaceHolder previewHolder;

    private byte[] previewBuffer;

    private boolean isStreaming = false;

    private AvcEncoder encoder;
    private DatagramSocket udpSocket;
    private InetAddress address;

    private int port;

    private List<byte[]> encDataList = new ArrayList<>();
    private List<Integer> encDataLengthList = new ArrayList<>();

    private Runnable senderRun = new Runnable()
    {
        @Override
        public void run()
        {
            while (isStreaming) {
                boolean empty = false;
                byte[] encData = null;

                synchronized (encDataList) {
                    if (encDataList.size() == 0) {
                        empty = true;
                    } else
                        encData = encDataList.remove(0);
                }
                if (empty) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                try {
                    DatagramPacket packet = new DatagramPacket(encData, encData.length, address, port);
                    udpSocket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //TODO:
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_h264_stream);

        this.findViewById(R.id.btnCamSize).setOnClickListener(
                v -> showSettingsDlg()
        );

        this.findViewById(R.id.btnStream).setOnClickListener(
                v -> {
                    if (isStreaming) {
                        ((Button) v).setText("Stream");
                        stopStream();
                    } else {
                        showStreamDlg();
                    }
                }
        );

        SurfaceView svCameraPreview = (SurfaceView) this.findViewById(R.id.svCameraPreview);
        this.previewHolder = svCameraPreview.getHolder();
        this.previewHolder.addCallback(this);
    }

    @Override
    protected void onPause()
    {
        this.stopStream();


        if (encoder != null)
            encoder.close();

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_h264_stream, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_settings) return true;

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        this.camera.addCallbackBuffer(this.previewBuffer);

        if (this.isStreaming) {
            if (this.encDataLengthList.size() > 100) {
                Log.e(TAG, "OUT OF BUFFER");
                return;
            }

            byte[] encData = this.encoder.offerEncoder(data);
            if (encData.length > 0) {
                synchronized (this.encDataList) {
                    this.encDataList.add(encData);
                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        stopCamera();
    }

    private void startStream(String ip, int port)
    {
        SharedPreferences sp = this.getPreferences(Context.MODE_PRIVATE);
        int width = sp.getInt(SP_CAM_WIDTH, 0);
        int height = sp.getInt(SP_CAM_HEIGHT, 0);

        this.encoder = new AvcEncoder();
        this.encoder.init(width, height, DEFAULT_FRAME_RATE, DEFAULT_BIT_RATE);

        try {
            this.udpSocket = new DatagramSocket();
            this.address = InetAddress.getByName(ip);
            this.port = port;
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        sp.edit().putString(SP_DEST_IP, ip).commit();
        sp.edit().putInt(SP_DEST_PORT, port).commit();

        this.isStreaming = true;
        Thread thrd = new Thread(senderRun);
        thrd.start();

        ((Button) this.findViewById(R.id.btnStream)).setText("Stop");
        this.findViewById(R.id.btnCamSize).setEnabled(false);
    }

    private void stopStream()
    {
        this.isStreaming = false;

        if (this.encoder != null)
            this.encoder.close();
        this.encoder = null;

        this.findViewById(R.id.btnCamSize).setEnabled(true);
    }

    private void startCamera()
    {

        SharedPreferences sp = this.getPreferences(Context.MODE_PRIVATE);
        int width = sp.getInt(SP_CAM_WIDTH, 0);
        int height = sp.getInt(SP_CAM_HEIGHT, 0);
        if (width == 0) {
            try {
                Camera tmpCam = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                if (tmpCam == null) return;
                Camera.Parameters params = tmpCam.getParameters();
                final List<Size> prevSizes = params.getSupportedPreviewSizes();
                int i = prevSizes.size() - 1;
                width = prevSizes.get(i).width;
                height = prevSizes.get(i).height;
                sp.edit().putInt(SP_CAM_WIDTH, width).commit();
                sp.edit().putInt(SP_CAM_HEIGHT, height).commit();
                tmpCam.release();
                tmpCam = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.previewHolder.setFixedSize(width, height);

        int stride = (int) Math.ceil(width / 16.0f) * 16;
        int cStride = (int) Math.ceil(width / 32.0f) * 16;
        final int frameSize = stride * height;
        final int qFrameSize = cStride * height / 2;

        this.previewBuffer = new byte[frameSize + qFrameSize * 2];

        try {
            camera = Camera.open();
            camera.setPreviewDisplay(this.previewHolder);
            Camera.Parameters params = camera.getParameters();
            params.setPreviewSize(width, height);
            params.setPreviewFormat(ImageFormat.YV12);
            camera.setParameters(params);
            camera.addCallbackBuffer(previewBuffer);
            camera.setPreviewCallbackWithBuffer(this);
            camera.startPreview();
        } catch (IOException e) {
            //TODO:
        } catch (RuntimeException e) {
            //TODO:
        }
    }

    private void stopCamera()
    {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private void showStreamDlg()
    {
        LayoutInflater inflater = this.getLayoutInflater();
        View content = inflater.inflate(R.layout.dialog_stream_status, null);

        SharedPreferences sp = this.getPreferences(Context.MODE_PRIVATE);
        String ip = sp.getString(SP_DEST_IP, "");
        int port = sp.getInt(SP_DEST_PORT, -1);
        if (ip.length() > 0) {
            EditText etIP = (EditText) content.findViewById(R.id.etIP);
            etIP.setText(ip);
            EditText etPort = (EditText) content.findViewById(R.id.etPort);
            etPort.setText(String.valueOf(port));
        }

        AlertDialog.Builder dlgBld = new AlertDialog.Builder(this);
        dlgBld.setTitle(R.string.app_name);
        dlgBld.setView(content);
        dlgBld.setPositiveButton(android.R.string.ok,
                (dialog, which) -> {
                    EditText etIP = (EditText) ((AlertDialog) dialog).findViewById(R.id.etIP);
                    EditText etPort = (EditText) ((AlertDialog) dialog).findViewById(R.id.etPort);
                    String ip1 = etIP.getText().toString();
                    int port1 = Integer.valueOf(etPort.getText().toString());
                    if (ip1.length() > 0 && (port1 >= 0 && port1 <= 65535)) {
                        startStream(ip1, port1);
                    } else {
                        //TODO:
                    }
                });
        dlgBld.setNegativeButton(android.R.string.cancel, null);
        dlgBld.show();
    }

    private void showSettingsDlg()
    {
        if (camera == null) return;
        Camera.Parameters params = camera.getParameters();
        final List<Size> prevSizes = params.getSupportedPreviewSizes();
        String[] choiceStrItems = new String[prevSizes.size()];
        List<String> choiceItems = new ArrayList<>();

        for (Size s : prevSizes) {
            choiceItems.add(s.width + "x" + s.height);
        }
        choiceItems.toArray(choiceStrItems);

        AlertDialog.Builder dlgBld = new AlertDialog.Builder(this);
        dlgBld.setTitle(R.string.app_name);
        dlgBld.setSingleChoiceItems(choiceStrItems, 0, null);
        dlgBld.setPositiveButton(android.R.string.ok,
                (dialog, which) -> {
                    int pos = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    Size s = prevSizes.get(pos);
                    SharedPreferences sp = H264StreamActivity.this.getPreferences(Context.MODE_PRIVATE);
                    sp.edit().putInt(SP_CAM_WIDTH, s.width).commit();
                    sp.edit().putInt(SP_CAM_HEIGHT, s.height).commit();

                    stopCamera();
                    startCamera();
                });
        dlgBld.setNegativeButton(android.R.string.cancel, null);
        dlgBld.show();
    }
}
