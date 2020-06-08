package pony.xcode.media;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import pony.xcode.media.adapter.ImagePagerAdapter;
import pony.xcode.media.adapter.MediaBasePagerAdapter;
import pony.xcode.media.adapter.OnPagerItemClickListener;
import pony.xcode.media.adapter.VideoPagerAdapter;
import pony.xcode.media.bean.MediaBean;
import pony.xcode.media.presenter.MediaPreviewPresenter;
import pony.xcode.media.view.MediaPreviewView;

import java.util.ArrayList;
import java.util.List;

/*图片预览页面*/
public class MediaPreviewActivity extends MediaBaseActivity implements MediaPreviewView {
    private FrameLayout mContainer;
    private Toolbar mToolBar;
    private FrameLayout mConfirmLayout;
    private TextView mConfirmWidget;
    private ViewPager mViewPager;
    private LinearLayout mBottomWidget;
    private TextView mSelectTextView;

    private boolean mShowBar = true;

    //tempImages和tempSelectImages用于图片列表数据的页面传输。
    //之所以不要Intent传输这两个图片列表，因为要保证两位页面操作的是同一个列表数据，同时可以避免数据量大时，
    // 用Intent传输发生的错误问题。
    private static List<MediaBean> tempMediaItems;
    private static List<MediaBean> tempSelectItems;

    private List<MediaBean> mMediaItemList = new ArrayList<>();
    private List<MediaBean> mSelected = new ArrayList<>();

    private int mChooseMode = MediaConfig.MODE_IMAGE;
    private boolean mSingleChoice;
    private int mMaxCount;

    private Drawable mSelectDrawable, mUnSelectDrawable;

    private MediaPreviewPresenter mPresenter;
    private boolean mConfirmed = false;

    public static void openActivity(Activity activity, int mode, List<MediaBean> images,
                                    List<MediaBean> selectImages, boolean isSingle,
                                    int maxSelectCount, int position) {
        tempMediaItems = images;
        tempSelectItems = selectImages;
        Intent intent = new Intent(activity, MediaPreviewActivity.class);
        intent.putExtras(dataPackages(mode, images, selectImages, isSingle, maxSelectCount, position));
        activity.startActivityForResult(intent, MediaPicker.PREVIEW_RESULT_CODE);
    }

    private static Bundle dataPackages(int mode, List<MediaBean> images, List<MediaBean> selectImages, boolean isSingle,
                                       int maxSelectCount, int position) {
        tempMediaItems = images;
        tempSelectItems = selectImages;
        Bundle bundle = new Bundle();
        bundle.putInt(MediaPicker.CHOOSE_MODE, mode);
        bundle.putInt(MediaPicker.MAX_SELECT_COUNT, maxSelectCount);
        bundle.putBoolean(MediaPicker.IS_SINGLE, isSingle);
        bundle.putInt(MediaPicker.POSITION, position);
        return bundle;
    }

    @Override
    protected int bindContentView() {
        return R.layout.mediaselector_activity_media_preview;
    }

    @Override
    protected void setup(@Nullable Bundle savedInstanceState) {
        mPresenter = MediaPreviewPresenter.from(this);
        mPresenter.initView();
    }

    @Override
    public void onGetIntent() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mChooseMode = bundle.getInt(MediaPicker.CHOOSE_MODE, MediaConfig.MODE_IMAGE);
            mMaxCount = bundle.getInt(MediaPicker.MAX_SELECT_COUNT, 0);
            mSingleChoice = bundle.getBoolean(MediaPicker.IS_SINGLE, false);
        }
        if (tempMediaItems != null) {
            mMediaItemList.addAll(tempMediaItems);
            tempMediaItems = null;
        }
        if (tempSelectItems != null) {
            mSelected.addAll(tempSelectItems);
            tempSelectItems = null;
        }
    }

    @Override
    public void onInitView() {
        mContainer = findViewById(R.id.mContainer);
        mToolBar = findViewById(R.id.toolBar);
        mConfirmLayout = findViewById(R.id.mConfirmLayout);
        mConfirmWidget = findViewById(R.id.mConfirmTv);
        mViewPager = findViewById(R.id.mViewPager);
        mBottomWidget = findViewById(R.id.llBottom);
        mSelectTextView = findViewById(R.id.mSelectTextView);
        mSelectDrawable = ContextCompat.getDrawable(this, R.drawable.mediaselector_icon_selected);
        mUnSelectDrawable = ContextCompat.getDrawable(this, R.drawable.mediaselector_icon_unselect);
    }

    @Override
    public void initViewData() {
        setTitle("1/" + mMediaItemList.size());
        setConfirm(mSelected.size());
    }

    private void setTitle(String title) {
        mToolBar.setTitle(title);
    }

    private void setConfirm(int count) {
        String textConfirm = getString(R.string.mediapicker_confirm);
        if (count == 0) {
            mConfirmLayout.setEnabled(false);
        } else {
            mConfirmLayout.setEnabled(true);
            if (mSingleChoice) {
                textConfirm = getString(R.string.mediapicker_confirm);
            } else if (mMaxCount > 0) {
                textConfirm = textConfirm + "(" + count + "/" + mMaxCount + ")";
            } else {
                textConfirm = textConfirm + "(" + count + ")";
            }
        }
        mConfirmWidget.setText(textConfirm);
    }

    @Override
    public void initViewPager() {
        mViewPager.clearOnPageChangeListeners();
        mViewPager.setOffscreenPageLimit(1);
        int initPosition = getIntent().getIntExtra(MediaPicker.POSITION, 0);
        final MediaBasePagerAdapter<?> pagerAdapter;
        if (mChooseMode == MediaConfig.MODE_VIDEO) {
            pagerAdapter = new VideoPagerAdapter(this, mMediaItemList, mViewPager, initPosition);
        } else {
            pagerAdapter = new ImagePagerAdapter(this, mMediaItemList);
        }
        pagerAdapter.setOnPagerItemClickListener(new OnPagerItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (mShowBar) {
                    mPresenter.hideBar(mContainer, mToolBar, mBottomWidget);
                    mShowBar = false;
                } else {
                    mPresenter.showBar(mContainer, mToolBar, mBottomWidget);
                    mShowBar = true;
                }
            }
        });
        mViewPager.setAdapter(pagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int position) {
                setTitle((position + 1) + "/" + mMediaItemList.size());
                setCheckStatus(position);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        setCheckStatus(initPosition);
        mViewPager.setCurrentItem(initPosition);
    }

    @Override
    public void onInitListener() {
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mConfirmLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConfirmed = true;
                finish();
            }
        });
        mSelectTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mMediaItemList.isEmpty()) {
                    int currentPos = mViewPager.getCurrentItem();
                    if (currentPos >= 0 && currentPos < mMediaItemList.size()) {
                        MediaBean mediaBean = mMediaItemList.get(currentPos);
                        if (mSelected.contains(mediaBean)) {
                            if (mSingleChoice) {
                                mSelected.clear();
                            } else {
                                mSelected.remove(mediaBean);
                            }
                            toggle(false);
                        } else {
                            /*单选情况下 清空已选图片再添加选中图片*/
                            if (mSingleChoice) {
                                mSelected.clear();
                                mSelected.add(mediaBean);
                                toggle(true);
                            } else {
                                /*未指定maxCount 或者选择的数量没有超过maxCount*/
                                if (mMaxCount == 0 || mSelected.size() < mMaxCount) {
                                    mSelected.add(mediaBean);
                                    toggle(true);
                                }
                            }
                        }
                        setConfirm(mSelected.size());
                    }
                }
            }
        });
    }

    private void setCheckStatus(int position) {
        if (!mMediaItemList.isEmpty() && position >= 0 && position < mMediaItemList.size()) {
            toggle(mSelected.contains(mMediaItemList.get(position)));
        }
    }

    //改变选择状态
    private void toggle(boolean isChecked) {
        mSelectTextView.setCompoundDrawablesWithIntrinsicBounds(isChecked ? mSelectDrawable : mUnSelectDrawable, null, null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mViewPager.getAdapter() instanceof VideoPagerAdapter) {
            VideoPagerAdapter adapter = (VideoPagerAdapter) mViewPager.getAdapter();
            adapter.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mViewPager.getAdapter() instanceof VideoPagerAdapter) {
            VideoPagerAdapter adapter = (VideoPagerAdapter) mViewPager.getAdapter();
            adapter.pause();
        }
    }

    @Override
    protected void onDestroy() {
        if (mViewPager.getAdapter() instanceof VideoPagerAdapter) {
            VideoPagerAdapter adapter = (VideoPagerAdapter) mViewPager.getAdapter();
            adapter.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void finish() {
        MediaPickerActivity.onPreviewResult(this, mSelected, mConfirmed);
        super.finish();
    }
}
