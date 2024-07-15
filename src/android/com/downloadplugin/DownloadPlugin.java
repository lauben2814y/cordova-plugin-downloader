package com.example.downloadplugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import java.util.HashMap;
import java.util.Map;

public class DownloadPlugin extends CordovaPlugin {

    private DownloadManager downloadManager;
    private Map<Long, DownloadTask> downloadTasks;
    private BroadcastReceiver downloadReceiver;

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        for (DownloadTask task : downloadTasks.values()) {
            downloadManager.pauseDownload(task.downloadId);
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        for (DownloadTask task : downloadTasks.values()) {
            downloadManager.resumeDownload(task.downloadId);
        }
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        downloadManager = (DownloadManager) cordova.getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        downloadTasks = new HashMap<>();
        registerDownloadReceiver();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("startDownload")) {
            String url = args.getString(0);
            String destination = args.getString(1);
            this.startDownload(url, destination, callbackContext);
            return true;
        } else if (action.equals("pauseDownload")) {
            long downloadId = args.getLong(0);
            this.pauseDownload(downloadId, callbackContext);
            return true;
        } else if (action.equals("resumeDownload")) {
            long downloadId = args.getLong(0);
            this.resumeDownload(downloadId, callbackContext);
            return true;
        } else if (action.equals("cancelDownload")) {
            long downloadId = args.getLong(0);
            this.cancelDownload(downloadId, callbackContext);
            return true;
        }
        return false;
    }

    // Implement methods: startDownload, pauseDownload, resumeDownload, cancelDownload
    // ...
    private void startDownload(String url, String destination, CallbackContext callbackContext) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDestinationUri(Uri.parse(destination));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setAllowedOverRoaming(false);
        request.setTitle("Downloading file");

        long downloadId = downloadManager.enqueue(request);
        DownloadTask task = new DownloadTask(downloadId, callbackContext, url, destination);
        downloadTasks.put(downloadId, task);

        callbackContext.success(String.valueOf(downloadId));
    }

    private void pauseDownload(long downloadId, CallbackContext callbackContext) {
        if (downloadTasks.containsKey(downloadId)) {
            downloadManager.pauseDownload(downloadId);
            callbackContext.success("Download paused");
        } else {
            callbackContext.error("Download not found");
        }
    }

    private void resumeDownload(long downloadId, CallbackContext callbackContext) {
        if (downloadTasks.containsKey(downloadId)) {
            downloadManager.resumeDownload(downloadId);
            callbackContext.success("Download resumed");
        } else {
            callbackContext.error("Download not found");
        }
    }

    private void cancelDownload(long downloadId, CallbackContext callbackContext) {
        if (downloadTasks.containsKey(downloadId)) {
            downloadManager.remove(downloadId);
            downloadTasks.remove(downloadId);
            callbackContext.success("Download canceled");
        } else {
            callbackContext.error("Download not found");
        }
    }

    private void updateDownloadStatus(DownloadTask task) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(task.downloadId);
        Cursor cursor = downloadManager.query(query);

        if (cursor.moveToFirst()) {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
            int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

            double progress = (bytesTotal > 0) ? (bytesDownloaded * 100.0 / bytesTotal) : 0;

            JSONObject statusUpdate = new JSONObject();
            try {
                statusUpdate.put("status", getStatusString(status));
                statusUpdate.put("progress", progress);
                statusUpdate.put("bytesDownloaded", bytesDownloaded);
                statusUpdate.put("bytesTotal", bytesTotal);
                task.callbackContext.success(statusUpdate);
            } catch (JSONException e) {
                task.callbackContext.error("Error creating status update");
            }
        }
        cursor.close();
    }

    private String getStatusString(int status) {
        switch (status) {
            case DownloadManager.STATUS_PAUSED:
                return "paused";
            case DownloadManager.STATUS_PENDING:
                return "pending";
            case DownloadManager.STATUS_RUNNING:
                return "running";
            case DownloadManager.STATUS_SUCCESSFUL:
                return "completed";
            case DownloadManager.STATUS_FAILED:
                return "failed";
            default:
                return "unknown";
        }
    }

    private void registerDownloadReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                DownloadTask task = downloadTasks.get(downloadId);
                if (task != null) {
                    updateDownloadStatus(task);
                }
            }
        };
        cordova.getActivity().registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void updateDownloadStatus(DownloadTask task) {
        // Implement status update logic
        // ...
    }

    private class DownloadTask {

        long downloadId;
        CallbackContext callbackContext;
        String url;
        String destination;

        DownloadTask(long downloadId, CallbackContext callbackContext, String url, String destination) {
            this.downloadId = downloadId;
            this.callbackContext = callbackContext;
            this.url = url;
            this.destination = destination;
        }
    }
}
