package com.yaoyi.myue.UI;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.yaoyi.myue.R;


/*选择视频源界面*/
public class SelectVideoSource extends AppCompatActivity {

    private Button mCaptureVideo;
    private Button mLocalVideo;
    private Button mOnlineVideo;
    private Button mPhotoVideo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);//去掉当前activity的标题栏，也即是将toolbar去掉（2015年toolbar取代actionbar即version4.4）

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_video_source);

        mCaptureVideo=findViewById(R.id.captureVideo_btn);//拍视频
        mLocalVideo=findViewById(R.id.localVideo_btn);//本地视频
        mOnlineVideo=findViewById(R.id.onlineVideo_btn);//网络视频
        mPhotoVideo=findViewById(R.id.photoVideo);

        mCaptureVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//跳转到用相机录制页面
//                Intent captureVideo=new Intent(SelectVideoSource.this,RecordVideoActivity.class);
                Intent captureVideo=new Intent(SelectVideoSource.this,RecordedActivity.class);
                startActivity(captureVideo);
            }
        });

        mLocalVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//跳转到本地视频列表页面
                Intent localVideo=new Intent(SelectVideoSource.this,LocalVideoListActivity.class);
                startActivity(localVideo);
            }
        });
        mOnlineVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//跳转到网页视频预览界面
                Intent onlineVideo=new Intent(SelectVideoSource.this,OnlineVideoActivity.class);
                startActivity(onlineVideo);
            }
        });
        mPhotoVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//跳转到网页视频预览界面
                Intent phothoVideo=new Intent(SelectVideoSource.this,PhotoToVideoActivity.class);
                startActivity(phothoVideo);
            }
        });

    }
}
