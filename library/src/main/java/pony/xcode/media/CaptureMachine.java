package pony.xcode.media;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import pony.xcode.media.utils.DialogUtils;
import pony.xcode.media.utils.MediaUtil;

//拍照或录制视频
public class CaptureMachine {

    private Activity mActivity;
    private String mFilePath;
    private int mRequestCode;

    private CaptureMachine(Activity activity) {
        this.mActivity = activity;
    }

    public static CaptureMachine from(Activity activity) {
        return new CaptureMachine(activity);
    }

    //调起系统拍照
    public void startImageCapture(int requestCode) {
        startImageCapture(requestCode, null);
    }

    public void startImageCapture(int requestCode, @Nullable OnCaptureListener l) {
        this.mRequestCode = requestCode;
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (captureIntent.resolveActivity(mActivity.getPackageManager()) != null) {
            try {
                File photoFile = MediaUtil.createImageFile(mActivity.getApplicationContext());
                if (photoFile != null) {
                    //通过FileProvider创建一个content类型的Uri
                    Uri imageUri = MediaUtil.getProviderUri(mActivity, photoFile);
                    mFilePath = photoFile.getAbsolutePath();
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    mActivity.startActivityForResult(captureIntent, requestCode);
                }
            } catch (Exception e) {
                mFilePath = null;
                if (l != null) {
                    l.onCaptureFailure();
                } else {
                    DialogUtils.showUnusableCamera(mActivity);
                }
            }
        } else {
            if (l != null) {
                l.onCaptureFailure();
            } else {
                DialogUtils.showUnusableCamera(mActivity);
            }
        }
    }

    //调起系统相机录像
    public void startVideoCapture(int requestCode, @NonNull VideoConfig config) {
        startVideoCapture(requestCode, config, null);
    }

    public void startVideoCapture(int requestCode, @NonNull VideoConfig config, @Nullable OnCaptureListener l) {
        this.mRequestCode = requestCode;
        Intent captureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (captureIntent.resolveActivity(mActivity.getPackageManager()) != null) {
            try {
                File photoFile = MediaUtil.createVideoFile(mActivity);
                if (photoFile != null) {
                    //通过FileProvider创建一个content类型的Uri
                    Uri videoUri = MediaUtil.getProviderUri(mActivity, photoFile);
                    mFilePath = photoFile.getAbsolutePath();
//                    Bundle extras = mActivity.getIntent().getExtras();
                    if (config.getDurationLimit() > 0) {
                        captureIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, config.getDurationLimit());
                    }
                    if (config.getSizeLimit() > 0) {
                        captureIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, config.getSizeLimit());
                    }
                    captureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, config.getQuality());
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
                    captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    mActivity.startActivityForResult(captureIntent, requestCode);
                }
            } catch (Exception e) {
                mFilePath = null;
                if (l != null) {
                    l.onCaptureFailure();
                } else {
                    DialogUtils.showUnusableCamera(mActivity);
                }
            }
        } else {
            if (l != null) {
                l.onCaptureFailure();
            } else {
                DialogUtils.showUnusableCamera(mActivity);
            }
        }
    }

    //在onActivityResult方法里回调
    @Nullable
    public String getFilePath(int requestCode, int resultCode) {
        if (requestCode == mRequestCode && resultCode == Activity.RESULT_OK && MediaUtil.isFileExists(mFilePath)) {
            return new File(mFilePath).getAbsolutePath();
        }
        return null;
    }
}
