package dev.yong.photo.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dev.yong.photo.PhotoSelector;
import dev.yong.photo.R;
import dev.yong.photo.bean.MediaFile;

/**
 * @author CoderYong
 */
public class PhotoAdapter extends BaseAdapter {

    private List<MediaFile> mMediaFiles;
    private LayoutInflater mInflater;

    private OnCameraClickListener mOnCameraClickListener;
    private OnItemClickListener mOnItemClickListener;
    private OnItemCheckedChangeListener mOnItemCheckedChangeListener;

    private boolean isShowCamera = false;

    public PhotoAdapter(List<MediaFile> mediaFiles, Context context) {
        this.mMediaFiles = mediaFiles;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        int count = isShowCamera ? 1 : 0;
        return mMediaFiles == null ? count : count + mMediaFiles.size();
    }

    @Override
    public MediaFile getItem(int position) {
        if (isShowCamera) {
            position--;
        }
        return mMediaFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (isShowCamera) {
            if (position == 0) {
                convertView = mInflater.inflate(R.layout.item_camera, parent, false);
                convertView.setOnClickListener(v -> {
                    if (mOnCameraClickListener != null) {
                        mOnCameraClickListener.onCameraClick();
                    }
                });
                return convertView;
            }
            position--;
        }
        if (convertView == null || convertView.getTag() == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_photo, parent, false);
            holder.ivPhoto = convertView.findViewById(R.id.iv_photo);
            holder.ivVideo = convertView.findViewById(R.id.iv_video);
            holder.tvDuration = convertView.findViewById(R.id.tv_duration);
            holder.cbPhoto = convertView.findViewById(R.id.cb_photo);
            holder.viewBlack = convertView.findViewById(R.id.view_black);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        MediaFile mediaFile = mMediaFiles.get(position);
        //设置图片选中状态
        holder.cbPhoto.setChecked(mediaFile.isSelected());
        holder.viewBlack.setVisibility(mediaFile.isSelected() ? View.VISIBLE : View.INVISIBLE);
        if (mediaFile.getType() == MediaFile.Type.VIDEO) {
            holder.ivVideo.setVisibility(View.VISIBLE);
            holder.tvDuration.setVisibility(View.VISIBLE);
            holder.tvDuration.setText(mediaFile.getDuration());
        } else {
            holder.ivVideo.setVisibility(View.GONE);
            holder.tvDuration.setVisibility(View.GONE);
        }
        Glide.with(parent.getContext()).load(mediaFile.getPath()).into(holder.ivPhoto);
        final int item = position;
        holder.cbPhoto.setOnClickListener(v -> {
            CheckBox checkBox = (CheckBox) v;
            if (checkBox.isChecked()) {
                int maxCount = PhotoSelector.getInstance().maxSelectCount();
                if (PhotoSelector.getInstance().selectedCount() == maxCount) {
                    checkBox.setChecked(false);
                    Toast.makeText(parent.getContext(), "您最多只能选择" + maxCount + "个", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            mMediaFiles.get(item).setSelected(checkBox.isChecked());
            zoom(holder.ivPhoto, checkBox.isChecked());
            holder.viewBlack.setVisibility(checkBox.isChecked() ? View.VISIBLE : View.INVISIBLE);
            if (mOnItemCheckedChangeListener != null) {
                mOnItemCheckedChangeListener.onCheckedChanged(mMediaFiles.get(item), checkBox.isChecked());
            }
        });

        if (mOnItemClickListener != null) {
            holder.ivPhoto.setOnClickListener(v -> mOnItemClickListener.onItemClick(item));
        }
        return convertView;
    }

    private void zoom(ImageView imageView, boolean isChecked) {
        AnimatorSet set = new AnimatorSet();
        if (isChecked) {
            set.playTogether(
                    ObjectAnimator.ofFloat(imageView, "scaleX", 1f, 1.12f),
                    ObjectAnimator.ofFloat(imageView, "scaleY", 1f, 1.12f)
            );
        } else {
            set.playTogether(
                    ObjectAnimator.ofFloat(imageView, "scaleX", 1.12f, 1f),
                    ObjectAnimator.ofFloat(imageView, "scaleY", 1.12f, 1f)
            );
        }
        set.setDuration(500);
        set.start();
    }

    public void addData(MediaFile... mediaFiles) {
        if (mediaFiles != null) {
            if (mMediaFiles == null) {
                mMediaFiles = Arrays.asList(mediaFiles);
            } else {
                this.mMediaFiles.addAll(Arrays.asList(mediaFiles));
            }
            Collections.sort(mMediaFiles, (o1, o2) -> (int) (o2.getLastModified() - o1.getLastModified()));
            notifyDataSetChanged();
        }
    }

    public void replaceData(List<MediaFile> mediaFiles) {
        if (mMediaFiles == null) {
            mMediaFiles = mediaFiles;
        } else if (mediaFiles != mMediaFiles) {
            this.mMediaFiles.clear();
            this.mMediaFiles.addAll(mediaFiles);
        }
        notifyDataSetChanged();
    }

    public List<MediaFile> getData() {
        return mMediaFiles;
    }

    public void setShowCamera(boolean showCamera) {
        isShowCamera = showCamera;
    }

    public void setOnCameraClickListener(OnCameraClickListener listener) {
        this.mOnCameraClickListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    public void setOnItemCheckedChangeListener(OnItemCheckedChangeListener listener) {
        this.mOnItemCheckedChangeListener = listener;
    }

    public interface OnCameraClickListener {

        /**
         * 相机点击回调
         */
        void onCameraClick();
    }

    public interface OnItemClickListener {

        /**
         * 相册点击回调
         *
         * @param position 相册位置
         */
        void onItemClick(int position);
    }

    public interface OnItemCheckedChangeListener {

        /**
         * 相册选择回调
         *
         * @param mediaFile 选择的文件
         * @param isChecked 是否选中
         */
        void onCheckedChanged(MediaFile mediaFile, boolean isChecked);
    }

    static class ViewHolder {
        ImageView ivPhoto;
        ImageView ivVideo;
        TextView tvDuration;
        CheckBox cbPhoto;
        View viewBlack;
    }
}
