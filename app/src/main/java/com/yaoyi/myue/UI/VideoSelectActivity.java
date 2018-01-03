package com.yaoyi.myue.UI;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.yaoyi.myue.Data.Adapter.VideoAdapter;
import com.yaoyi.myue.R;

import java.io.IOException;
/**
 * Created by cj on 2017/10/16.
 * desc: local video select activity
 */

public class VideoSelectActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>,VideoAdapter.OnVideoSelectListener {
    ImageView ivClose;
    GridView gridview;
    public static final String PROJECT_VIDEO = MediaStore.MediaColumns._ID;
    private VideoAdapter mVideoAdapter;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_select);
        initView();
        initData();
    }

    private void initView() {
        ivClose= (ImageView) findViewById(R.id.iv_close);//叉按钮
        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();//结束当前Activity
            }
        });
        gridview=(GridView)findViewById(R.id.gridview_media_video);//显示本地视频列表
    }
    private void initData() {
        getLoaderManager().initLoader(0,null,this);//？？用Loader来创建异步访问数据库模式，
        //初始化的时候调用回调即下面的onCreateLoader
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;//本地视频uri
        String order = MediaStore.MediaColumns.DATE_ADDED + " DESC";//排序，以视频添加时间为标准
        return new CursorLoader(getApplicationContext(), videoUri,
                new String[]{MediaStore.Video.Media.DATA, PROJECT_VIDEO},//_data列，_ID列
                null, null, order);//以添加时间排序cursor
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {//lodaer加载完一定回返回data
        if (data == null || data.getCount() <= 0) {
            return;
        }
        if (mVideoAdapter == null) {
            mVideoAdapter = new VideoAdapter(getApplicationContext(), data);//创建适配器
            //mVideoAdapter.setMediaSelectVideoActivity(this);
           // mVideoAdapter.setOnSelectChangedListener(this);//？
        } else {
            mVideoAdapter.swapCursor(data);//？
        }


        if (gridview.getAdapter() == null) {
            gridview.setAdapter(mVideoAdapter);//给窗格视图设置适配器
        }
        mVideoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mVideoAdapter != null)
            mVideoAdapter.swapCursor(null);
    }

    @Override
    protected void onDestroy() {
        getLoaderManager().destroyLoader(0);
        Glide.get(this).clearMemory();
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onSelect(final String path, String cover) {
        //处理音频，视频
        int videoTrack=-1;
        int audioTrack=-1;
        MediaExtractor extractor=new MediaExtractor();
        try {
            extractor.setDataSource(path);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                    videoTrack=i;
                    String videoMime = format.getString(MediaFormat.KEY_MIME);
                    if(!"video/avc".equals(videoMime)){
                        Toast.makeText(this,"视频格式不支持", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    continue;
                }
                if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                    audioTrack=i;
                    String audioMime = format.getString(MediaFormat.KEY_MIME);
                    if(!"audio/mp4a-latm".equals(audioMime)){
                        Toast.makeText(this,"视频格式不支持", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    continue;
                }
            }
            extractor.release();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this,"视频格式不支持", Toast.LENGTH_SHORT).show();
            extractor.release();
            return;
        }
        if(videoTrack==-1||audioTrack==-1){
            Toast.makeText(this,"视频格式不支持", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
        mDialog.setMessage("去分离音频还是添加滤镜");
        mDialog.setPositiveButton("加滤镜", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //跳转预览界面 TODO
                if(!TextUtils.isEmpty(path)){
                    /*Intent intent=new Intent(VideoSelectActivity.this,PreviewActivity.class);
                    intent.putExtra("path",path);
                    startActivity(intent);*/
                    dialog.dismiss();
                }
            }
        });
        mDialog.setNegativeButton("分离音频", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!TextUtils.isEmpty(path)){
                    /*Intent intent=new Intent(VideoSelectActivity.this,AudioPreviewActivity.class);
                    intent.putExtra("path",path);
                    startActivity(intent);*/
                    dialog.dismiss();
                }
            }
        });
        mDialog.show();
    }
}
