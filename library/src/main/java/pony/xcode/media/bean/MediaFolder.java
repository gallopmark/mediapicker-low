package pony.xcode.media.bean;

import androidx.annotation.NonNull;

import pony.xcode.media.utils.StringUtils;

import java.util.ArrayList;

/**
 * 图片文件夹实体类
 */
public class MediaFolder {
    private boolean useCamera; // 是否可以调用相机拍照。只有“全部”文件夹才可以拍照
    private String name;
    private ArrayList<MediaBean> images;

    public MediaFolder(String name) {
        this.name = name;
    }

    public MediaFolder(String name, ArrayList<MediaBean> images) {
        this.name = name;
        this.images = images;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<MediaBean> getImages() {
        return images;
    }

    public boolean isUseCamera() {
        return useCamera;
    }

    public void setUseCamera(boolean useCamera) {
        this.useCamera = useCamera;
    }

    public void addImage(MediaBean media) {
        if (media != null && StringUtils.isNotEmptyString(media.getPath())) {
            if (images == null) {
                images = new ArrayList<>();
            }
            images.add(media);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Folder{" +
                "name='" + name + '\'' +
                ", images=" + images +
                '}';
    }
}
