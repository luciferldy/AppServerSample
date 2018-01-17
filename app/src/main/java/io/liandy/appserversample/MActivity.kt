/**
 * Created by lian_ on 2018/1/17.
 */
package io.liandy.appserversample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log

import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.koushikdutta.async.http.server.HttpServerRequestCallback

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Created by lian_ on 2018/1/17.
 */

class MActivity : AppCompatActivity() {

    private val server = AsyncHttpServer()
    private val mAsyncServer = AsyncServer()

    private val indexContent: String
        @Throws(IOException::class)
        get() {
            var bInputStream: BufferedInputStream? = null
            try {
                bInputStream = BufferedInputStream(assets.open("index.html"))
                val baos = ByteArrayOutputStream()
                var len = 0
                val tmp = ByteArray(10240)
                len = bInputStream.read(tmp)
                while (len> 0) {
                    baos.write(tmp, 0, len)
                    len = bInputStream.read(tmp)
                }
                return String(baos.toByteArray(), charset("UTF-8"))
            } catch (e: IOException) {
                e.printStackTrace()
                throw e
            } finally {
                if (bInputStream != null) {
                    try {
                        bInputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        server.get("/") { request, response ->
            try {
                response.send(indexContent)
            } catch (e: IOException) {
                e.printStackTrace()
                response.code(500).end()
            }
        }
        server.get("/files") { request, response ->
            Log.d(LOG_TAG, "onRequest /files")
            val array = JSONArray()
            val dir = File(Environment.getExternalStorageDirectory().path)
            val fileNames = dir.list()
            if (fileNames != null) {
                for (fileName in fileNames) {
                    val file = File(dir, fileName)
                    if (file.exists() && file.isFile && file.name.endsWith(".mp4")) {
                        try {
                            val jsonObject = JSONObject()
                            jsonObject.put("name", fileName)
                            jsonObject.put("path", file.absoluteFile)
                            array.put(jsonObject)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }

                    }
                }
            }
            response.send(array.toString())
        }
        server.get("/files/.*", HttpServerRequestCallback { request, response ->
            var path = request.path.replace("/files/", "")
            Log.d(LOG_TAG, "onRequest /files/* " + path)
            try {
                path = URLDecoder.decode(path, "utf-8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            val file = File(Environment.getExternalStorageDirectory().path, path)
            if (file.exists() && file.isFile) {
                try {
                    val fis = FileInputStream(file)
                    response.sendStream(fis, fis.available().toLong())
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return@HttpServerRequestCallback
            }
            response.code(404).send("Not Found")
        })
        server.listen(mAsyncServer, 54321)
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE -> if (grantResults.size == 2) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(LOG_TAG, "request permission granted!")
                    return
                }
            }
        }
        Log.d(LOG_TAG, "request permission denied.")
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
        mAsyncServer.stop()
    }

    companion object {

        private val LOG_TAG = "AppServerSample"
        private val REQUEST_CODE = 1214
    }
}
