package pony.xcode.media.presenter;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import pony.xcode.media.MediaConfig;
import pony.xcode.media.R;
import pony.xcode.media.bean.MediaFolder;
import pony.xcode.media.bean.MediaBean;
import pony.xcode.media.dialog.PictureLoadingDialog;
import pony.xcode.media.model.LocalMediaSource;
import pony.xcode.media.utils.DateUtils;
import pony.xcode.media.MediaPicker;
import pony.xcode.media.utils.DialogUtils;
import pony.xcode.media.utils.MediaUtil;
import pony.xcode.media.view.MediaPickerView;

import java.io.File;
import java.util.ArrayList;

public class MediaPickerPresenter {

    private Activity activity;
    private MediaPickerView pickerView;
    private LocalMediaSource mLocalMediaSource;

    private static final int PERMISSION_WRITE_EXTERNAL_REQUEST_CODE = 0x00000011;
    private static final int PERMISSION_CAMERA_REQUEST_CODE = 0x00000012;

    private static final int IMAGE_CAPTURE_CODE = 0x00000010;
    private static final int VIDEO_CAPTURE_CODE = 0x00000015;

    private boolean mExternalSetting;
    private boolean isLoadImage = false;

    private boolean isShowTime;

    private int mChooseMode;
    private String mPhotoPath;  //拍照后的路径
    private String mVideoPath; //录制视频后的路径

    private PictureLoadingDialog mLoadingDialog;

    private HideRunnable mHide;

    private class HideRunnable implements Runnable {
        private TextView mTimeTextView;

        HideRunnable(TextView mTimeTextView) {
            this.mTimeTextView = mTimeTextView;
        }

        @Override
        public void run() {
            hideTime(mTimeTextView);
        }
    }

    public MediaPickerPresenter(Activity activity, @NonNull MediaPickerView pickerView) {
        this.activity = activity;
        this.pickerView = pickerView;
        mLocalMediaSource = new LocalMediaSource();
    }

    public void initView() {
        pickerView.onGetIntent();
        pickerView.onInitView();
        pickerView.onInitListener();
        pickerView.onInitImageList();
        pickerView.onCheckExternalPermission();
    }

    public void checkPermissionAndLoadImages(int mode) {
        mChooseMode = mode;
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasExternalPermission()) { //有权限，加载图片。
                loadImageFromSDCard();
            } else { //没有权限，申请权限。
                requestExternalPermission();
            }
        } else {
            loadImageFromSDCard();
        }
    }

    private void loadImageFromSDCard() {
        mLocalMediaSource.loadImageForSDCard(activity, mChooseMode, new LocalMediaSource.OnCompleteListener() {
            @Override
            public void onPreLoad() {
                showLoading();
            }

            @Override
            public void loadComplete(ArrayList<MediaFolder> folders) {
                hideLoading();
                isLoadImage = true;
                pickerView.onLoadFolders(folders);
            }

            @Override
            public void loadMediaDataError() {
                hideLoading();
                isLoadImage = false;
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean hasExternalPermission() {
        /*android 6.0 以上需要动态申请权限*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestExternalPermission() {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_REQUEST_CODE);
    }

    public void changeTime(TextView tvDatetime, MediaBean image) {
        if (image != null) {
            String time = DateUtils.getImageTime(activity, image.getTime() * 1000);
            tvDatetime.setText(time);
            showTime(tvDatetime);
            if (mHide == null) {
                mHide = new HideRunnable(tvDatetime);
            } else {
                tvDatetime.removeCallbacks(mHide);
            }
            tvDatetime.postDelayed(mHide, 1500);
        }
    }

    /*隐藏时间条*/
    private void hideTime(TextView tvDatetime) {
        if (isShowTime) {
            ObjectAnimator.ofFloat(tvDatetime, "alpha", 1, 0).setDuration(300).start();
            isShowTime = false;
        }
    }

    /*显示时间条*/
    private void showTime(TextView tvDatetime) {
        if (!isShowTime) {
            ObjectAnimator.ofFloat(tvDatetime, "alpha", 0, 1).setDuration(300).start();
            isShowTime = true;
        }
    }

    /*弹出文件夹列表*/
    public void openFolder(final FrameLayout folderLayout, final FrameLayout folderNameLayout) {
        if (folderLayout.getVisibility() != View.VISIBLE) {
            folderNameLayout.setEnabled(false);
            ObjectAnimator animator = ObjectAnimator.ofFloat(folderLayout, "translationY",
                    folderLayout.getHeight(), 0).setDuration(300);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    folderLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    folderNameLayout.setEnabled(true);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    folderNameLayout.setEnabled(true);
                }
            });
            animator.start();
        }
    }

    /*收起文件夹列表*/
    public void closeFolder(final FrameLayout folderLayout, final FrameLayout folderNameLayout) {
        if (folderLayout.getVisibility() != View.GONE) {
            folderNameLayout.setEnabled(false);
            ObjectAnimator animator = ObjectAnimator.ofFloat(folderLayout, "translationY",
                    0, folderLayout.getHeight()).setDuration(300);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    folderLayout.setVisibility(View.GONE);
                    folderNameLayout.setEnabled(true);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    folderNameLayout.setEnabled(true);
                }
            });
            animator.start();
        }
    }

    public void checkPermissionAndCamera() {
        if (hasCameraPermission()) { //有调起相机拍照。
            startCapture();
        } else { //没有权限，申请权限。
            requestCameraPermission();
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQUEST_CODE);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_WRITE_EXTERNAL_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //允许权限，加载图片。
                loadImageFromSDCard();
            } else {
                //拒绝权限，弹出提示框。
                if (permissions.length > 0 && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[0]) || (
                        permissions.length > 1 && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[1]))) {
                    whenExternalPermissionBanned();
                } else {
                    onShouldRequestExternalPermission();
                }
            }
        } else if (requestCode == PERMISSION_CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //允许权限，有调起相机拍照。
                startCapture();
            } else {
                if (permissions.length > 0 && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[0])) {
                    rationaleAppSettings(activity.getString(R.string.mediapicker_banned_camera_permission), false);
                } else {
                    onShowRequestCameraPermission();
                }
            }
        }
    }

    //存储权限已被禁止
    private void whenExternalPermissionBanned() {
        rationaleAppSettings(activity.getString(R.string.mediapicker_banned_external_permissions), true);
    }

    //引导用户到应用设置 打开权限
    private void rationaleAppSettings(String message, final boolean finishWhenCancel) {
        DialogUtils.showDismissPermission(activity, message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mExternalSetting = finishWhenCancel;
                MediaPicker.startAppSettings(activity);
            }
        }, finishWhenCancel);
    }

    //提示用户存储权限允许打开
    private void onShouldRequestExternalPermission() {
        DialogUtils.showDismissPermission(activity, activity.getString(R.string.mediapicker_dismiss_external_permissions), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestExternalPermission();
            }
        }, true);
    }

    //提示用户相机权限允许打开
    private void onShowRequestCameraPermission() {
        DialogUtils.showDismissPermission(activity, activity.getString(R.string.mediapicker_dismiss_camera_permission), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestCameraPermission();
            }
        }, false);
    }

    private void startCapture() {
        if (mChooseMode == MediaConfig.MODE_VIDEO) {
            startVideoCapture();
        } else {
            startImageCapture();
        }
    }

    /*调起相机拍照*/
    private void startImageCapture() {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (captureIntent.resolveActivity(activity.getPackageManager()) != null) {
            try {
                File photoFile = MediaUtil.createImageFile(activity.getApplicationContext());
                if (photoFile != null) {
                    //通过FileProvider创建一个content类型的Uri
                    Uri imageUri = MediaUtil.getProviderUri(activity, photoFile);
                    mPhotoPath = photoFile.getAbsolutePath();
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    activity.startActivityForResult(captureIntent, IMAGE_CAPTURE_CODE);
                }
            } catch (Exception e) {
                mPhotoPath = null;
                DialogUtils.showUnusableCamera(activity);
            }
        } else {
            DialogUtils.showUnusableCamera(activity);
        }
    }

    //录制视频
    private void startVideoCapture() {
        Intent captureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (captureIntent.resolveActivity(activity.getPackageManager()) != null) {
            try {
                File photoFile = MediaUtil.createVideoFile(activity);
                if (photoFile != null) {
                    //通过FileProvider创建一个content类型的Uri
                    Uri videoUri = MediaUtil.getProviderUri(activity, photoFile);
                    mVideoPath = photoFile.getAbsolutePath();
                    Bundle extras = activity.getIntent().getExtras();
                    if (extras != null) {
                        int durationLimit = extras.getInt(MediaPicker.DURATION_LIMIT, 0);
                        if (durationLimit > 0) {
                            captureIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, durationLimit);
                        }
                        captureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, extras.getInt(MediaPicker.VIDEO_QUALITY, MediaConfig.DEFAULT_VIDEO_QUALITY));
                        long sizeLimit = extras.getLong(MediaPicker.SIZE_LIMIT, 0);
                        if (sizeLimit > 0) {  //限制了录制大小 则限制的录制时间将不起作用
                            captureIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, sizeLimit);
                        }
                    }
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
                    captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    activity.startActivityForResult(captureIntent, VIDEO_CAPTURE_CODE);
                }
            } catch (Exception e) {
                mVideoPath = null;
                DialogUtils.showUnusableCamera(activity);
            }
        } else {
            DialogUtils.showUnusableCamera(activity);
        }
    }

    public boolean isLoadImage() {
        return isLoadImage;
    }

    public void onActivityResult(int requestCode, int resultCode) {
        //拍照成功返回路径
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK && mPhotoPath != null) {
            setCaptureResult(mPhotoPath);
        } else if (requestCode == VIDEO_CAPTURE_CODE && resultCode == Activity.RESULT_OK && mVideoPath != null) {
            setCaptureResult(mVideoPath);
        }
    }

    private void showLoading() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new PictureLoadingDialog(activity);
        } else {
            if (mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }
        }
        mLoadingDialog.show();
    }

    private void hideLoading() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
        }
    }

    //拍摄返回
    private void setCaptureResult(String filePath) {
        if (MediaUtil.isFileExists(filePath)) {
            activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    MediaUtil.getProviderUri(activity, new File(filePath))));
            ArrayList<MediaBean> imageBeans = new ArrayList<>();
            imageBeans.add(new MediaBean(filePath));
            setResult(Activity.RESULT_OK, imageBeans, true);
            activity.finish();
        }
    }

    public void setResult(int resultCode, ArrayList<MediaBean> imageBeans,
                          boolean isCameraImage) {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(MediaPicker.SELECT_RESULT, imageBeans);
        intent.putExtra(MediaPicker.IS_CAMERA_IMAGE, isCameraImage);
        activity.setResult(resultCode, intent);
    }

    public void onReStart() {
        if (!isLoadImage && hasExternalPermission()) {
            loadImageFromSDCard();
        } else {
            if (mExternalSetting) {
                whenExternalPermissionBanned();
                mExternalSetting = false;
            }
        }
    }

    public void destroy() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
        if (mLocalMediaSource != null) {
            mLocalMediaSource.release();
        }
        MediaPicker.gcDisplacer();
    }
}
