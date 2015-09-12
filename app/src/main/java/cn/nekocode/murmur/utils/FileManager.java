package cn.nekocode.murmur.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListener;
import com.thin.downloadmanager.ThinDownloadManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import cn.nekocode.murmur.beans.MurmurBean;

/**
 * Created by nekocode on 2015/4/23 0023.
 */
public final class FileManager {
    private static final String APP_ROOT = "murmur!";
    private static ThinDownloadManager downloadManager = new ThinDownloadManager(3);

    public static boolean isExternalStorageMounted() {
        boolean canRead = Environment.getExternalStorageDirectory().canRead();
        boolean onlyRead = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED_READ_ONLY);
        boolean unMounted = Environment.getExternalStorageState().equals(
                Environment.MEDIA_UNMOUNTED);

        return !(!canRead || onlyRead || unMounted);
    }

    public static String getAppRootPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + APP_ROOT
                + File.separator;
    }

    public static String getAppCachePath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + APP_ROOT
                + File.separator + "cache" + File.separator;
    }

    public static void createAppRootDirs() {
        if (!isExternalStorageMounted()) {
            Log.e("createAppRootDirs", "sdcard unavailiable");
        }

        File dir = new File(getAppRootPath());
        if (!dir.exists()) {
            dir.mkdirs();
        }

        dir = new File(getAppCachePath());
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static boolean isMurmurBuffered() {
        if(downloadingTasks != 0)
            return false;

        String dirPath = getAppRootPath() + "murmurs" + File.separator;
        File dir = new File(dirPath);
        if(dir.exists()) {
            return true;
        }
        return false;
    }

    public static int downloadTotalTasks = 0;
    public static int downloadingTasks = 0;
    public static ProgressDialog progressDialog;
    public static MyCallback.Callback0 callback;
    public static void bufferMurmur(Context context, Map<String, MurmurBean> murmurs, MyCallback.Callback0 callback) {
        if (!isExternalStorageMounted()) {
            Log.e("bufferMurmur", "sdcard unavailiable");
        }

        String dirPath = getAppRootPath() + "murmurs" + File.separator;
        File dir = new File(dirPath);
        if(!dir.exists()) {
            dir.mkdirs();

            FileManager.callback = callback;
            downloadTotalTasks = murmurs.size();
            downloadingTasks = downloadTotalTasks;
            Iterator<MurmurBean> iterator = murmurs.values().iterator();
            while(iterator.hasNext()) {
                MurmurBean murmurBean = iterator.next();
                download(murmurBean.getUrl(), dirPath + murmurBean.getName());
            }

            if(progressDialog == null)
                progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Now buffering some data : " + (downloadTotalTasks-downloadingTasks) + "/" + downloadTotalTasks + "...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }

    public static void download(String url, String filePath) {
        Uri downloadUri = Uri.parse(url);
        Uri destinationUri = Uri.parse(filePath);
        DownloadRequest downloadRequest = new DownloadRequest(downloadUri)
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadListener(new DownloadStatusListener() {
                    @Override
                    public void onDownloadComplete(int id) {
                        downloadingTasks--;
                        progressDialog.setMessage("Now buffering some data : " + (downloadTotalTasks-downloadingTasks) + " / " + downloadTotalTasks + " ...");

                        if(downloadingTasks == 0) {
                            progressDialog.dismiss();
                            progressDialog = null;
                            callback.run();
                        }
                    }

                    @Override
                    public void onDownloadFailed(int id, int i1, String s) {
                        downloadingTasks--;
                        ToastUtils.show("Lost a file!");

                        if(downloadingTasks == 0) {
                            progressDialog.dismiss();
                            progressDialog = null;
                            callback.run();
                        }
                    }

                    @Override
                    public void onProgress(int id, long totalBytes, final int progress) {

                    }
                });

        downloadManager.add(downloadRequest);
    }

    public static void update(final Context context, String url) {
        if(progressDialog == null)
            progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Now Updating...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        final String file = getAppRootPath() + "murmur!.apk";
        Uri downloadUri = Uri.parse(url);
        Uri destinationUri = Uri.parse(file);
        DownloadRequest downloadRequest = new DownloadRequest(downloadUri)
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadListener(new DownloadStatusListener() {
                    @Override
                    public void onDownloadComplete(int id) {
                        installApk(context, file);
                        progressDialog.dismiss();
                        progressDialog = null;
                    }

                    @Override
                    public void onDownloadFailed(int id, int i1, String s) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }

                    @Override
                    public void onProgress(int id, long totalBytes, final int progress) {
                    }
                });

        downloadManager.add(downloadRequest);
    }

    private static void installApk(Context context, String file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(file)), "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    public static boolean saveToAppDir(Context context, String path) {
        if (!isExternalStorageMounted()) {
            return false;
        }

        File file = new File(path);
        String name = file.getName();
        String newPath = getAppRootPath() + name;
        try {
            FileManager.createNewFileInSDCard(context, newPath);
            copyFile(file, new File(newPath));
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    public static File createNewFileInSDCard(Context context, String absolutePath) {
        if (!isExternalStorageMounted()) {
            Log.e("createNewFileInSDCard", "sdcard unavailiable");
            return null;
        }

        if (TextUtils.isEmpty(absolutePath)) {
            return null;
        }

        File file = new File(absolutePath);
        if (file.exists()) {
            return file;
        } else {
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            try {
                if (file.createNewFile()) {
                    return file;
                }
            } catch (IOException e) {
                Log.e("createNewFileInSDCard", e.getMessage());
                return null;

            }

        }
        return null;

    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files == null) {
                return true;
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    private static void copyFile(File sourceFile, File targetFile) throws IOException {
        BufferedInputStream inBuff = null;
        BufferedOutputStream outBuff = null;
        try {
            inBuff = new BufferedInputStream(new FileInputStream(sourceFile));

            outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));

            byte[] b = new byte[1024 * 5];
            int len;
            while ((len = inBuff.read(b)) != -1) {
                outBuff.write(b, 0, len);
            }
            outBuff.flush();
        } finally {
            if (inBuff != null) {
                inBuff.close();
            }
            if (outBuff != null) {
                outBuff.close();
            }
        }
    }
}
