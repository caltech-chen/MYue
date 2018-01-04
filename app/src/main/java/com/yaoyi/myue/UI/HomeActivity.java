package com.yaoyi.myue.UI;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.GridView;
import android.widget.Toast;

import com.yaoyi.myue.Data.Adapter.VideoAdapterH;
import com.yaoyi.myue.R;

import java.io.IOException;

import static com.yaoyi.myue.UI.VideoSelectActivity.PROJECT_VIDEO;

/*首页展示好玩的视频*/
public class HomeActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, VideoAdapterH.OnVideoSelectListener {

    private NavigationView mNavigationView;//侧滑栏view
    private FloatingActionButton start;
    private  GridView gridview;//显示视频Girdview
    private VideoAdapterH mVideoAdapter;//Girdview的Adapter
    private Handler fullscreenh = new Handler();//全屏线程
    private final Runnable fullscreenRunnable = new Runnable() {//全屏线程构造
        @SuppressLint("InlinedApi")//保证不同版本的全屏,详细见下英文描述
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    //    @TargetApi(21)//
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);//去掉当前activity的标题栏，也即是将toolbar去掉（2015年toolbar取代actionbar即version4.4）
        init();//这里只初始化了全屏
        setContentView(R.layout.activity_home);//启动显示好玩视频的主界面
        mNavigationView = findViewById(R.id.navigation_view);//关联侧滑栏界面
        mNavigationView.setItemIconTintList(null);//设置菜单图标恢复本来的颜色,但是我不知道什么原因

         start=findViewById(R.id.startfloatingActionButton);//浮动按钮
start.setOnClickListener(new View.OnClickListener() {//浮动按钮监听
    Intent select_video_source=new Intent(HomeActivity.this,SelectVideoSource.class);//切换到选择视频来源Activity
    @Override
    public void onClick(View v) {
        startActivity(select_video_source);

    }
});
        initView();//初始化GirdView
        initData();//？？？？
    }
    @Override
    public void onResume() {
        super.onResume();
        fullscreen();//hide statusbar, actionbar,navigationbar
    }
    private void initView() {
        gridview = (GridView) findViewById(R.id.gridview_media_videolist);//显示本地视频列表
    }

    private void initData() {
        getLoaderManager().initLoader(0, null, this);//？？用Loader来创建异步访问数据库模式，
        //初始化的时候调用回调即下面的onCreateLoader
    }



    protected void init() {
        fullscreen();
    }

    protected void fullscreen() {
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    public void onPause() {
        super.onPause();
        fullscreen();
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;//本地视频uri
        String order = MediaStore.MediaColumns.DATE_ADDED + " DESC";//排序，以视频添加时间为标准
        return new CursorLoader(getApplicationContext(), videoUri,
                new String[]{MediaStore.Video.Media.DATA, PROJECT_VIDEO},//_data列，_ID列
                null, null, order);//以添加时间排序cursor
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() <= 0) {
            return;
        }
        if (mVideoAdapter == null) {
            mVideoAdapter = new VideoAdapterH(getApplicationContext(), data);//创建适配器
            mVideoAdapter.setMediaSelectVideoActivity(this);
            mVideoAdapter.setOnSelectChangedListener(this);//？
        } else {
            mVideoAdapter.swapCursor(data);//？
        }


        if (gridview.getAdapter() == null) {
            gridview.setAdapter(mVideoAdapter);//给窗格视图设置适配器
        }
        mVideoAdapter.notifyDataSetChanged();
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mVideoAdapter != null)
            mVideoAdapter.swapCursor(null);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onSelect(final String path, String cover) {
        //处理音频，视频
        int videoTrack = -1;//标志
        int audioTrack = -1;
        MediaExtractor extractor = new MediaExtractor();//媒体解码对象
        try {
            extractor.setDataSource(path);
            for (int i = 0; i < extractor.getTrackCount(); i++) {//得到相应的音频和视频轨道数量
                MediaFormat format = extractor.getTrackFormat(i);//
                if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {//视频轨道
                    videoTrack = i;//记录视频在第几轨
                    String videoMime = format.getString(MediaFormat.KEY_MIME);//拿到视频轨到名字
                    if (!"video/avc".equals(videoMime)) {//如果视频轨道是video/avc，即对应avc视频，没那么就不支持
                        Toast.makeText(this, "视频格式不支持", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    continue;
                }
                if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {//拿到音频轨道
                    audioTrack = i;//记录音频轨道在第几轨
                    String audioMime = format.getString(MediaFormat.KEY_MIME);//拿到音频轨道全称
                    if (!"audio/mp4a-latm".equals(audioMime)) {//如果是mp4a-latm这种就不显示不支持
                        Toast.makeText(this, "视频格式不支持", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    continue;
                }
            }
            extractor.release();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "视频格式不支持", Toast.LENGTH_SHORT).show();
            extractor.release();//释放解码其
            return;
        }
        if (videoTrack == -1 || audioTrack == -1) {
            Toast.makeText(this, "视频格式不支持", Toast.LENGTH_SHORT).show();//只要音频或视频又一个不符合条件的就返回
            return;
        }
        AlertDialog.Builder mDialog = new AlertDialog.Builder(this);//他这里用的是警告Dialog、所以是红色
        mDialog.setMessage("去分离音频还是添加滤镜");
        mDialog.setPositiveButton("加滤镜", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //跳转预览界面 TODO
                if (!TextUtils.isEmpty(path)) {
                    /*Intent intent=new Intent(VideoSelectActivity.this,PreviewActivity.class);
                    intent.putExtra("path",path);
                    startActivity(intent);*/
                    dialog.dismiss();//隐去对话框
                }
            }
        });
        mDialog.setNegativeButton("分离音频", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!TextUtils.isEmpty(path)) {
                    /*Intent intent=new Intent(VideoSelectActivity.this,AudioPreviewActivity.class);
                    intent.putExtra("path",path);
                    startActivity(intent);*/
                    dialog.dismiss();
                }
            }
        });
        mDialog.show();
        fullscreenh.postDelayed(fullscreenRunnable, 5);//使状态栏和导航栏隐退，如果不用postDelayed
        //而用post的话，就会因为太快，而起不到隐藏的作用)
    }
}
