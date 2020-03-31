package pony.xcode.media.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import pony.xcode.media.utils.DateUtils;

public class MediaBean implements Parcelable {
    private long id; //文件id
    private String path;    //绝对路径
    private long time; //加入相册的时间
    private String name;    //名称
    private String mimeType;    //mimeType
    private long duration; //视频时长
    private long size;  //视频大小

    public MediaBean() {

    }

    public MediaBean(String path) {
        this.path = path;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public String getFormatDurationTime() {
        return DateUtils.generateTime(duration);
    }

    public boolean isGif() {
        return "image/gif".equals(mimeType);
    }

    public boolean isUriPath() {
        return !TextUtils.isEmpty(path) && path.contains("content://");
    }

    /* 图片的路径和创建时间相同就认为是同一张图片*/
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof MediaBean) {
            return path.equals(((MediaBean) obj).path) && name.equals(((MediaBean) obj).name);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 31 + (!TextUtils.isEmpty(path) ? path.hashCode() : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.path);
        dest.writeLong(this.time);
        dest.writeString(this.name);
        dest.writeString(this.mimeType);
        dest.writeLong(this.duration);
        dest.writeLong(this.size);
    }

    protected MediaBean(Parcel in) {
        this.id = in.readLong();
        this.path = in.readString();
        this.time = in.readLong();
        this.name = in.readString();
        this.mimeType = in.readString();
        this.duration = in.readLong();
        this.size = in.readLong();
    }

    public static final Creator<MediaBean> CREATOR = new Creator<MediaBean>() {
        @Override
        public MediaBean createFromParcel(Parcel source) {
            return new MediaBean(source);
        }

        @Override
        public MediaBean[] newArray(int size) {
            return new MediaBean[size];
        }
    };
}
