package pony.xcode.media.model;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.database.Cursor;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import pony.xcode.media.MediaConfig;
import pony.xcode.media.R;
import pony.xcode.media.bean.MediaFolder;
import pony.xcode.media.bean.MediaBean;
import pony.xcode.media.utils.MediaUtil;
import pony.xcode.media.utils.StringUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LocalMediaSource implements Handler.Callback {

    private WeakReference<Activity> mActivityWeak;
    private static final int MSG_QUERY_MEDIA_SUCCESS = 0;
    private static final int MSG_QUERY_MEDIA_ERROR = -1;

    private static final String NOT_GIF = "!='image/gif'";  //过滤掉gif图

    private int mChooseMode;
    private Handler mHandler;
    private OnCompleteListener mCompleteListener;

    /**
     * 从SDCard加载图片
     */
    public void loadImageForSDCard(final Activity activity, final int mode, final OnCompleteListener listener) {
        //由于扫描图片是耗时的操作，所以要在子线程处理。
        mActivityWeak = new WeakReference<>(activity);
        mChooseMode = mode;
        mHandler = new Handler(Looper.getMainLooper(), this);
        mCompleteListener = listener;
        mCompleteListener.onPreLoad();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ArrayList<MediaBean> mediaBeans;
                    if (mChooseMode == MediaConfig.MODE_VIDEO) { //获取视频
                        mediaBeans = queryVideos();
                    } else {  //默认为获取图片
                        mediaBeans = queryImages();
                    }
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_QUERY_MEDIA_SUCCESS, splitFolder(mediaBeans)));
                } catch (Exception e) {
                    mHandler.sendEmptyMessage(MSG_QUERY_MEDIA_ERROR);
                }
            }
        });
    }

    private String[] getImageProjection() {
        return new String[]{
                MediaStore.Images.ImageColumns._ID, //id
                MediaStore.Images.ImageColumns.DATA,   //路径
                MediaStore.Images.ImageColumns.MIME_TYPE,  //mime type
                MediaStore.Images.ImageColumns.DATE_ADDED, //添加的时间
                MediaStore.Images.ImageColumns.DISPLAY_NAME //名称
        };
    }

    private String getImageSelection() {
        return MediaStore.Images.ImageColumns.SIZE + ">0"
                + " AND " + MediaStore.Images.ImageColumns.MIME_TYPE + NOT_GIF;
    }

    private String getImageOrderBy() {
        return MediaStore.Images.ImageColumns._ID + " DESC";
    }

    //查询所有图片
    private ArrayList<MediaBean> queryImages() {
        ArrayList<MediaBean> imageMedias = new ArrayList<>();
        Activity activity = mActivityWeak.get();
        if (activity == null) {
            return imageMedias;
        }
        String[] projection = getImageProjection();
        Cursor cursor = activity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, getImageSelection(), null, getImageOrderBy());
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (mActivityWeak.get() == null) {
                    break;
                }
                String path = cursor.getString(cursor.getColumnIndex(projection[1]));
                if (!MediaUtil.isFileExists(path)) {  //文件不存在跳过
                    continue;
                }
                //过滤未下载完成或者不存在的文件
                if (!TextUtils.equals(MediaUtil.getExtensionName(path), "downloading")) {
                    String mimeType = cursor.getString(cursor.getColumnIndex(projection[2]));
                    long time = cursor.getLong(cursor.getColumnIndex(projection[3]));
                    String name = cursor.getString(cursor.getColumnIndex(projection[4]));
                    MediaBean mediaBean = new MediaBean();
                    mediaBean.setId(cursor.getColumnIndex(projection[0]));
                    mediaBean.setPath(path);
                    mediaBean.setMimeType(mimeType);
                    mediaBean.setName(name);
                    mediaBean.setTime(time);
                    imageMedias.add(mediaBean);
                }
            }
            cursor.close();
        }
        return imageMedias;
    }

    @SuppressLint("InlinedApi")
    private String[] getVideoProjection() {
        return new String[]{
                MediaStore.Video.VideoColumns._ID, //id
                MediaStore.Video.VideoColumns.DATA,   //路径
                MediaStore.Video.VideoColumns.MIME_TYPE,  //mime type
                MediaStore.Video.VideoColumns.DATE_ADDED, //添加的时间
                MediaStore.Video.VideoColumns.DISPLAY_NAME, //名称
                MediaStore.Video.VideoColumns.DURATION,   //时长
                MediaStore.Video.VideoColumns.SIZE,    //大小
        };
    }

    //只查询size大于0的视频
    private String getVideoSelection() {
        return MediaStore.Video.VideoColumns.SIZE + ">0";
    }

    private String getVideoOrderBy() {
        return MediaStore.Video.VideoColumns._ID + " DESC";
    }

    private ArrayList<MediaBean> queryVideos() {
        ArrayList<MediaBean> videoMedias = new ArrayList<>();
        Activity activity = mActivityWeak.get();
        if (activity == null) {
            return videoMedias;
        }
        String[] projection = getVideoProjection();
        Cursor cursor = activity.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, getVideoSelection(), null, getVideoOrderBy());
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (mActivityWeak.get() == null) {
                    break;
                }
                long duration = cursor.getLong(cursor.getColumnIndex(projection[5]));
                long size = cursor.getLong(cursor.getColumnIndex(projection[6]));
                if (duration == 0 || size <= 0) {
                    // 时长如果为0，就当做损坏的视频处理过滤掉、视频大小为0过滤掉
                    continue;
                }
                String path = cursor.getString(cursor.getColumnIndex(projection[1]));
                if (!MediaUtil.isFileExists(path)) {  //文件不存在跳过
                    continue;
                }
                if (!TextUtils.equals(MediaUtil.getExtensionName(path), "downloading")) {
                    String mimeType = cursor.getString(cursor.getColumnIndex(projection[2]));
                    long time = cursor.getLong(cursor.getColumnIndex(projection[3]));
                    String name = cursor.getString(cursor.getColumnIndex(projection[4]));
                    MediaBean mediaBean = new MediaBean();
                    mediaBean.setId(cursor.getColumnIndex(projection[0]));
                    mediaBean.setPath(path);
                    mediaBean.setMimeType(mimeType);
                    mediaBean.setName(name);
                    mediaBean.setTime(time);
                    mediaBean.setDuration(duration);
                    mediaBean.setSize(size);
                    videoMedias.add(mediaBean);
                }
            }
            cursor.close();
        }
        return videoMedias;
    }

    /**
     * 把图片按文件夹拆分，第一个文件夹保存所有的图片
     */
    private ArrayList<MediaFolder> splitFolder(ArrayList<MediaBean> images) {
        ArrayList<MediaFolder> folders = new ArrayList<>();
        Activity activity = mActivityWeak.get();
        if (activity == null) return folders;
        folders.add(new MediaFolder(activity.getString(mChooseMode == 2 ? R.string.mediapicker_allvideos :
                R.string.mediapicker_allpictures), images));
        if (images != null && !images.isEmpty()) {
            int size = images.size();
            for (int i = 0; i < size; i++) {
                String path = images.get(i).getPath();
                String folderName = getFolderName(path);
                if (StringUtils.isNotEmptyString(folderName)) {
                    MediaFolder folder = getFolder(folderName, folders);
                    folder.addImage(images.get(i));
                }
            }
        }
        return folders;
    }

    /**
     * 根据图片路径，获取图片文件夹名称
     */
    private static String getFolderName(String path) {
        if (StringUtils.isNotEmptyString(path)) {
            String[] strings = path.split(File.separator);
            if (strings.length >= 2) {
                return strings[strings.length - 2];
            }
        }
        return "";
    }

    private static MediaFolder getFolder(String name, List<MediaFolder> folders) {
        if (!folders.isEmpty()) {
            int size = folders.size();
            for (int i = 0; i < size; i++) {
                MediaFolder folder = folders.get(i);
                if (TextUtils.equals(folder.getName(), name)) {
                    return folder;
                }
            }
        }
        MediaFolder newFolder = new MediaFolder(name);
        folders.add(newFolder);
        return newFolder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (mCompleteListener == null) return false;
        switch (msg.what) {
            case MSG_QUERY_MEDIA_SUCCESS:
                mCompleteListener.loadComplete((ArrayList<MediaFolder>) msg.obj);
                break;
            case MSG_QUERY_MEDIA_ERROR:
                mCompleteListener.loadMediaDataError();
                break;
        }
        return false;
    }

    public void release() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    public interface OnCompleteListener {
        void onPreLoad();

        void loadComplete(ArrayList<MediaFolder> folders);

        void loadMediaDataError();
    }
}
