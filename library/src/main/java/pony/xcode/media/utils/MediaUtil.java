package pony.xcode.media.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.os.EnvironmentCompat;

import pony.xcode.media.MediaConfig;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MediaUtil {

    /**
     * Java文件操作 获取文件扩展名
     */
    public static String getExtensionName(String filename) {
        if (filename != null && filename.length() > 0) {
            int dot = filename.lastIndexOf('.');
            if (dot > -1 && dot < filename.length() - 1) {
                return filename.substring(dot + 1);
            }
        }
        return "";
    }

    /**
     * 检查图片是否存在。ContentResolver查询处理的数据有可能文件路径并不存在。
     */
    public static boolean isFileExists(String filePath) {
        if (TextUtils.isEmpty(filePath)) return false;
        File file = new File(filePath);
        return file.exists() && file.length() > 0;
    }

    public static File createImageFile(Context context) {
        return createCaptureFile(context, 1, getCreateFileName("IMG_", MediaConfig.JPEG));
    }

    public static File createVideoFile(Context context) {
        return createCaptureFile(context, 2, getCreateFileName("VID_", MediaConfig.MP4));
    }

    /**
     * 文件根目录
     */
    @Nullable
    private static File getRootDirFile(Context context, int type) {
        if (type == 2) {
            return context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        }
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    private static File createCaptureFile(Context context, int type, String child) {
        String state = Environment.getExternalStorageState();
        File parent = state.equals(Environment.MEDIA_MOUNTED) ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) : getRootDirFile(context, type);
        if (parent == null) return null;
        try {
            boolean isCreated;
            if (!parent.exists()) {
                isCreated = parent.mkdirs();
            } else {
                isCreated = true;
            }
            if (!isCreated) return null;
            File file = new File(parent, child);
            if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(file))) {
                return null;
            }
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    public static Uri getProviderUri(Context context, File file) {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = context.getPackageName() + ".provider";
            uri = FileProvider.getUriForFile(context, authority, file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    @SuppressWarnings("WeakerAccess")
    public static String getCreateFileName(String prefix, String suffix) {
        long millis = System.currentTimeMillis();
        return prefix + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(millis) + suffix;
    }
}
