package com.yaoyi.myue.UI;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Context;
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
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.Toast;

import com.yaoyi.myue.Data.Adapter.VideoAdapter;
import com.yaoyi.myue.R;

import java.io.IOException;

import static com.yaoyi.myue.UI.VideoSelectActivity.PROJECT_VIDEO;

/*首页展示好玩的视频*/
public class HomeActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,VideoAdapter.OnVideoSelectListener{

    private NavigationView mNavigationView;
    GridView gridview;
    private VideoAdapter mVideoAdapter;
    @TargetApi(21)
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
       init();
        setContentView(R.layout.activity_home);
        mNavigationView =  findViewById(R.id.navigation_view);
        mNavigationView.setItemIconTintList(null);//设置菜单图标恢复本来的颜色,但是我不知道什么原因
       initView();
       initData();
    }

    private void initView() {
        gridview=(GridView)findViewById(R.id.gridview_media_videolist);//显示本地视频列表
        gridview.setNumColumns(2);
    }
    private void initData() {
        getLoaderManager().initLoader(0,null,this);//？？用Loader来创建异步访问数据库模式，
        //初始化的时候调用回调即下面的onCreateLoader
    }
    @Override
    public void onResume(){
       super.onResume();
       fullscreen();//hide statusbar, actionbar,navigationbar
    }
    protected void  init(){
       fullscreen();
    }
    protected void fullscreen(){
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
@Override
public void onPause(){
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

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link FragmentManager#beginTransaction()
     * FragmentManager.openTransaction()} for further discussion on this.
     * <p>
     * <p>This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     * <p>
     * <ul>
     * <li> <p>The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.  For example, if the data is a {@link Cursor}
     * and you place it in a {@link CursorAdapter}, use
     * the {@link CursorAdapter#CursorAdapter(Context, * Cursor, int)} constructor <em>without</em> passing
     * in either {@link CursorAdapter#FLAG_AUTO_REQUERY}
     * or {@link CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
     * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
     * from doing its own observing of the Cursor, which is not needed since
     * when a change happens you will get a new Cursor throw another call
     * here.
     * <li> The Loader will release the data once it knows the application
     * is no longer using it.  For example, if the data is
     * a {@link Cursor} from a {@link CursorLoader},
     * you should not call close() on it yourself.  If the Cursor is being placed in a
     * {@link CursorAdapter}, you should use the
     * {@link CursorAdapter#swapCursor(Cursor)}
     * method so that the old Cursor is not closed.
     * </ul>
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() <= 0) {
            return;
        }
        if (mVideoAdapter == null) {
            mVideoAdapter = new VideoAdapter(getApplicationContext(), data);//创建适配器
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
