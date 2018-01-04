
package com.yaoyi.myue.UI;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import com.yaoyi.myue.R;
/*图片合成视频界面*/
public class PhotoToVideoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);//去掉当前activity的标题栏，也即是将toolbar去掉（2015年toolbar取代actionbar即version4.4）

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_to_video);
    }
}
