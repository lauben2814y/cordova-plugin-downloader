var DownloadPlugin = {
    startDownload: function(url, destination, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "DownloadPlugin", "startDownload", [url, destination]);
    },
    pauseDownload: function(downloadId, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "DownloadPlugin", "pauseDownload", [downloadId]);
    },
    resumeDownload: function(downloadId, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "DownloadPlugin", "resumeDownload", [downloadId]);
    },
    cancelDownload: function(downloadId, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "DownloadPlugin", "cancelDownload", [downloadId]);
    }
};

module.exports = DownloadPlugin;