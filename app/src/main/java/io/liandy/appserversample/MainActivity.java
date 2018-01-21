package io.liandy.appserversample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.font.TextAttribute;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by lian_ on 2018/1/17.
 */

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AppServerSample";
    private static final int REQUEST_CODE = 1214;
    private AsyncHttpServer server = new AsyncHttpServer();
    private AsyncServer mAsyncServer = new AsyncServer();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.d(LOG_TAG, "onRequest /");
                try {
                    response.send(getIndexContent());
                } catch (IOException e) {
                    e.printStackTrace();
                    response.code(500).end();
                }
            }
        });
        server.get("/jquery-3.2.1.min.js", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.d(LOG_TAG, "onRequest jquery-3.2.1.min.js fullPath is " + request.getPath());
                try {
                    String fullPath = request.getPath();
                    fullPath = fullPath.replace("%20", " ");
                    String resourceName = fullPath;
                    if (resourceName.startsWith("/")) {
                        resourceName = resourceName.substring(1);
                    }
                    if (resourceName.indexOf("?") > 0) {
                        resourceName = resourceName.substring(0, resourceName.indexOf("?"));
                    }
                    response.setContentType("application/javascript");
                    BufferedInputStream bInputStream = new BufferedInputStream(getAssets().open(resourceName));
                    response.sendStream(bInputStream, bInputStream.available());
                } catch (IOException e) {
                    e.printStackTrace();
                    response.code(404).end();
                }
            }
        });

        server.get("/files", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.d(LOG_TAG, "onRequest /files");
                JSONArray array = new JSONArray();
                File dir = new File(Environment.getExternalStorageDirectory().getPath());
                String[] fileNames = dir.list();
                if (fileNames != null) {
                    for (String fileName: fileNames) {
                        File file = new File(dir, fileName);
                        if (file.exists() && file.isFile() && file.getName().endsWith(".mp4")) {
                            try {
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("name", fileName);
                                jsonObject.put("path", file.getAbsoluteFile());
                                array.put(jsonObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                response.send(array.toString());
            }
        });
        server.get("/files/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String path = request.getPath().replace("/files/", "");
                try {
                    path = URLDecoder.decode(path, "utf-8");
                    Log.d(LOG_TAG, "onRequest /files/* " + path);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                File file = new File(path);
                if (file.exists() && file.isFile()) {
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        response.sendStream(fis, fis.available());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
                response.code(404).send("Not Found");
            }
        });
        server.listen(mAsyncServer, 54321);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length == 2) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                            && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        Log.i(LOG_TAG, "request permission granted!");
                        return;
                    }
                }
        }
        Log.d(LOG_TAG, "request permission denied.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
        }
        if (mAsyncServer != null) {
            mAsyncServer.stop();
        }
    }

    private String getIndexContent() throws IOException {
        BufferedInputStream bInputStream = null;
        try {
            bInputStream = new BufferedInputStream(getAssets().open("index.html"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = 0;
            byte[] tmp = new byte[10240];
            while ((len = bInputStream.read(tmp)) > 0) {
                baos.write(tmp, 0, len);
            }
            return new String(baos.toByteArray(), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (bInputStream != null) {
                try {
                    bInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
