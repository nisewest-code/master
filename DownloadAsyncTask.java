package com.alegangames.master.download;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.alegangames.master.events.DownloadEvent;
import com.alegangames.master.util.files.FileUtil;
import com.alegangames.master.util.files.FormatTextUtil;
import com.alegangames.master.util.files.MemoryManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

public class DownloadAsyncTask extends AsyncTask<Void, DownloadEvent, Boolean> {

    public static final String TAG = DownloadAsyncTask.class.getSimpleName();

    public static final int MIN_UPDATE_INTERVAL = 200;

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_CANCELED = 1;
    public static final int STATUS_MEMORY_ERROR = 3;
    public static final int STATUS_NETWORK_ERROR = 4;
    public static final int STATUS_UNKNOWN_ERROR = 5;

    private int mTaskStatus = STATUS_UNKNOWN_ERROR;

    private String mUrlString;
    private File mFile;
    private String mNewName;
    private String mPathSave;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private WeakReference<Context> mContextWeakReference;
    private MutableLiveData<DownloadEvent> mDownloadEventLiveData;

    private int mRequestCode;

    private HttpURLConnection mHttpURLConnection;

    public DownloadAsyncTask(Context context, String newName, String pathSave, String url, int requestCode, MutableLiveData<DownloadEvent> downloadEventLiveData) {
        mContextWeakReference = new WeakReference<>(context);
        mNewName = newName;
        mPathSave = pathSave;
        mRequestCode = requestCode;
        mDownloadEventLiveData = downloadEventLiveData;
        mUrlString = url;
    }

    @Override
    protected void onPreExecute() {
        if (mDownloadEventLiveData != null)
            mDownloadEventLiveData.postValue(new DownloadEvent(0, "", "", "", mRequestCode));
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        int count;

        try {
            URL url = new URL(mUrlString);
            Log.d(TAG, "doInBackground: URL " + url);

            mHttpURLConnection = (HttpURLConnection) url.openConnection();
            mHttpURLConnection.setRequestMethod("GET");
            mHttpURLConnection.setConnectTimeout(10_000);
            mHttpURLConnection.setReadTimeout(30_000);
            mHttpURLConnection.setInstanceFollowRedirects(true);
            mHttpURLConnection.setUseCaches(false);
            mHttpURLConnection.setDoInput(true);
            mHttpURLConnection.connect();

            //???????????????? ?????????? ??????????????
            if (mHttpURLConnection.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                //????????????
                mTaskStatus = STATUS_NETWORK_ERROR;
                return false;
            }

            long lengthOfFile = mHttpURLConnection.getContentLength();

            mInputStream = new BufferedInputStream(mHttpURLConnection.getInputStream());
            String path = url.getPath();

            //???????????????????? ??????????
//            mFileExt = FileUtil.getExtensionFile(path);

            //???????????? ?????? ??????????
            String fileName = FileUtil.getFileName(path);

            //???????????????? ?????? ???????????????????????? ??????????
            if (mNewName != null && !mNewName.isEmpty()) {
                fileName = mNewName;
            }

            Log.d(TAG, "doInBackground: File Name " + fileName);

            //???????????????? ???? ?????????????????????? ?????????????? ????????????
            if (MemoryManager.externalMemoryAvailable() && MemoryManager.getEnoughMemory(lengthOfFile, MemoryManager.getAvailableExternalMemorySizeLong(mContextWeakReference.get()))) {
                //???????????????? ???????????????????? ?????????????? ?????????? ???????????????????? ????????????????????
                Log.d(TAG, "doInBackground: ExternalStorage is available");
            } else {
                Log.d(TAG, "doInBackground: ExternalStorage is not available");
                mTaskStatus = STATUS_MEMORY_ERROR;
                return false;
            }

            //?????????????????? ???????????????????? ?????? ????????????????????
            if (mContextWeakReference.get() != null) {
                File dir = new File(mContextWeakReference.get().getExternalFilesDir(null).getAbsolutePath() + mPathSave);
                Log.d(TAG, "doInBackground: mDir " + dir);

                //???????? ???????????????????? ???? ????????????????????, ?????????????? ????
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                //?????????????? ????????
                mFile = new File(dir, fileName);
                Log.d(TAG, "doInBackground: mFile " + mFile);

                //???????????????????? ????????
                mOutputStream = new FileOutputStream(mFile);
                byte data[] = new byte[1024];
                long total = 0;
                int progress = 0;

                long lastUpdate = System.currentTimeMillis();

                while ((count = mInputStream.read(data)) != -1) {
                    total += count;
                    if (isCancelled()) {
                        Log.d(TAG, "doInBackground: isCancelled() " + isCancelled());
                        mTaskStatus = STATUS_CANCELED;
                        return false;
                    }
                    //???????????????????? ????????????????, ???????? ?????????????? ???????????????? ????????????????????
                    if (lengthOfFile != 0 && total != 0) {
                        int newProgress = (int) ((total * 100) / lengthOfFile);
                        if (newProgress > progress && System.currentTimeMillis() - lastUpdate > MIN_UPDATE_INTERVAL) {
                            lastUpdate = System.currentTimeMillis();
                            progress = newProgress;
                            publishProgress(new DownloadEvent(
                                    progress,
                                    progress + "%",
                                    FormatTextUtil.getFileSize(lengthOfFile),
                                    FormatTextUtil.getFileSize(total), mRequestCode));
                        }
                    }
                    mOutputStream.write(data, 0, count);
                }
            }

            if (mFile != null) {
                //?????????????????? ???????????????? ????????????
                if (mContextWeakReference.get() != null)
                    MediaScannerConnection.scanFile(mContextWeakReference.get(), new String[]{mFile.toString()}, null, null);
                //?????????????? ???????????????? ??????????
//                FileUtil.onDeleteHiddenFilesMac(mDir);
            }
        } catch (SocketTimeoutException | UnknownHostException | SSLException e) {
            e.printStackTrace();
            mTaskStatus = STATUS_NETWORK_ERROR;
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            mTaskStatus = STATUS_UNKNOWN_ERROR;
            return false;
        } finally {
            onCleanResource();
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(DownloadEvent... events) {
        Log.d(TAG, "onProgressUpdate: " + events[0].percent);
        if (mDownloadEventLiveData != null)
            mDownloadEventLiveData.postValue(events[0]);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        Log.d(TAG, "onPostExecute: result " + result);
        Log.d(TAG, "onPostExecute: mTaskStatus " + mTaskStatus);
        if (result) {
            mTaskStatus = STATUS_SUCCESS;
        }
        if (mDownloadEventLiveData != null)
            mDownloadEventLiveData.postValue(new DownloadEvent(mFile, mTaskStatus, mRequestCode));
    }

    @Override
    protected void onCancelled() {
        Log.d(TAG, "onCancelled");
        mTaskStatus = STATUS_CANCELED;
        if (mDownloadEventLiveData != null)
            mDownloadEventLiveData.postValue(new DownloadEvent(mFile, mTaskStatus, mRequestCode));
    }

    private void onCleanResource() {
        //?????????????? ?? ?????????????????? ????????????
        try {
            if (mInputStream != null)
                mInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (mOutputStream != null)
                mOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (mOutputStream != null)
                mOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (mHttpURLConnection != null)
                mHttpURLConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}