package com.yaoyi.myue.drawer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.yaoyi.myue.R;
import com.yaoyi.myue.filter.AFilter;
import com.yaoyi.myue.filter.GroupFilter;
import com.yaoyi.myue.filter.NoFilter;
import com.yaoyi.myue.filter.ProcessFilter;
import com.yaoyi.myue.filter.RotationOESFilter;
import com.yaoyi.myue.filter.WaterMarkFilter;
import com.yaoyi.myue.gpufilter.SlideGpuFilterGroup;
import com.yaoyi.myue.gpufilter.basefilter.GPUImageFilter;
import com.yaoyi.myue.gpufilter.filter.MagicBeautyFilter;
import com.yaoyi.myue.media.VideoInfo;
import com.yaoyi.myue.utils.EasyGlUtils;
import com.yaoyi.myue.utils.MatrixUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * Created by cj on 2017/10/16.
 * desc：添加水印和美白效果
 */

public class VideoDrawer implements GLSurfaceView.Renderer {
    /**用于后台绘制的变换矩阵*/
    private float[] OM;
    /**用于显示的变换矩阵*/
    private float[] SM = new float[16];
    private SurfaceTexture surfaceTexture;
    /**可选择画面的滤镜*/
    private RotationOESFilter mPreFilter;
    /**显示的滤镜*/
    private AFilter mShow;
    /**美白的filter*/
    private MagicBeautyFilter mBeautyFilter;
    private AFilter mProcessFilter;
    /**绘制水印的滤镜*/
    private final GroupFilter mBeFilter;
    /**多种滤镜切换*/
    private SlideGpuFilterGroup mSlideFilterGroup;

    /**绘制其他样式的滤镜*/
    private GPUImageFilter mGroupFilter;
    /**控件的长宽*/
    private int viewWidth;
    private int viewHeight;

    /**创建离屏buffer*/
    private int[] fFrame = new int[1];
    private int[] fTexture = new int[1];
    /**用于视频旋转的参数*/
    private int rotation;
    /**是否开启美颜*/
    private boolean isBeauty = false;
     private int texture[]=new int[1];

    public VideoDrawer(Context context, Resources res){
        mPreFilter = new RotationOESFilter(res);//重新设置了可以调节输入纹理方位的矩阵
        mShow = new NoFilter(res);//传入了一个shader代码，并设置了透明
        mBeFilter = new GroupFilter(res);//？初始化一个filter数组列表（Afilter型）和队列Afilter型）
        mBeautyFilter = new MagicBeautyFilter();//初始化了特殊的shader代码

        mProcessFilter=new ProcessFilter(res);//初始化一个filter（Afilter型），并且该filter传入了shader代码，也设置了上下反转矩阵

        mSlideFilterGroup = new SlideGpuFilterGroup();//初始化了一个Scroll和三个GPUImageFilter，将当前的位置设置为0，左边设置为最后一个滤镜，右边设置为第二个滤镜
        OM = MatrixUtils.getOriginalMatrix();//OM此处被赋值为4维单位矩阵
        MatrixUtils.flip(OM,false,true);//反转4维单位阵，留待后面用某一个单位阵相乘
//        mShow.setMatrix(OM);

        WaterMarkFilter waterMarkFilter = new WaterMarkFilter(res);//初始化了一个不清除底色的noFilter
        waterMarkFilter.setWaterMark(BitmapFactory.decodeResource(res, R.mipmap.watermark));//设置水印路径

        waterMarkFilter.setPosition(0,70,0,0);
        mBeFilter.addFilter(waterMarkFilter);//filter队列中现在有了一个水印滤镜

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        GLES20.glGenTextures(1,texture,0);//生成一个纹理对象，gl会自动保存，根据后面运行结果，可以知道这个对象是1
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES ,texture[0]);//将此处刚刚生成的纹理对象1与GL_TEXTURE_EXTERNAL_OES绑定
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,//GL_TEXTURE_EXTERNAL_OES的一些参数，待查
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        surfaceTexture = new SurfaceTexture(texture[0]);//刚刚生成的纹理对象1与surfaceTexture绑定，此处也同时完成解码流到纹理的绑定
        mPreFilter.create();//创建了一个gl程序，完成加载mPreFilter的shader代码，即fragment颜色从视频帧过来
        mPreFilter.setTextureId(texture[0]);//设置mPreFilter的纹理id为1，等其调用draw（）时，用这个将相关绘制单元绑定到当前纹理

        mBeFilter.create();//空操作
        mProcessFilter.create();//空操作
        mShow.create();//创建了一个gl程序，创建完的话就已经将shader代码加载完了
        mBeautyFilter.init();//加载shader代码，并获取位置，纹理图片，纹理图片坐标
        mBeautyFilter.setBeautyLevel(3);//默认设置3级的美颜
        mSlideFilterGroup.init();//加载了三个GPUImageFilter（和美白差不多的）的shader
    }
    public void onVideoChanged(VideoInfo info){
        setRotation(info.rotation);
        if(info.rotation==0||info.rotation==180){
            MatrixUtils.getShowMatrix(SM,info.width,info.height,viewWidth,viewHeight);
        }else{
            MatrixUtils.getShowMatrix(SM,info.height,info.width,viewWidth,viewHeight);
        }

        mPreFilter.setMatrix(SM);
    }
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewWidth=width;
        viewHeight=height;
        GLES20.glDeleteFramebuffers(1, fFrame, 0);//感觉是删除之前的buffer？
        GLES20.glDeleteTextures(1, fTexture, 0);

        GLES20.glGenFramebuffers(1,fFrame,0);//
        //用fTexture来生成一组texture，此处size=1，只生成一个，此处生成的纹理为空，但有若干参数
        //第二个纹理
        EasyGlUtils.genTexturesWithParameter(1,fTexture,0, GLES20.GL_RGBA,viewWidth,viewHeight);

        mBeFilter.setSize(viewWidth,viewHeight);//空操作，生成第4 、5 个纹理
        mProcessFilter.setSize(viewWidth,viewHeight);//空操作  生成第6个纹理
        mBeautyFilter.onDisplaySizeChanged(viewWidth,viewHeight);//传入宽高
        mBeautyFilter.onInputSizeChanged(viewWidth,viewHeight);//传入宽高
        mSlideFilterGroup.onSizeChanged(viewWidth,viewHeight);//生成一个空纹理，没绑定，并传入宽高，第七个纹理
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        surfaceTexture.updateTexImage();//
        //更新surfaceTexture中的一帧图像
        EasyGlUtils.bindFrameTexture( fFrame[0],fTexture[0]);//指定对应的帧buffer和纹理
        GLES20.glViewport(0,0,viewWidth,viewHeight);//指定gl窗口大小


        mPreFilter.draw();//将texture绘制到frame绑定的fTexture0
        EasyGlUtils.unBindFrameBuffer();//

        mBeFilter.setTextureId(fTexture[0]);
        mBeFilter.draw();

        if (mBeautyFilter != null && isBeauty && mBeautyFilter.getBeautyLevel()==0){
            EasyGlUtils.bindFrameTexture(fFrame[0],fTexture[0]);
            GLES20.glViewport(0,0,viewWidth,viewHeight);
            mBeautyFilter.onDrawFrame(mBeFilter.getOutputTexture())    ;
            EasyGlUtils.unBindFrameBuffer();
            mProcessFilter.setTextureId(fTexture[0]);
        }else {
            mProcessFilter.setTextureId(mBeFilter.getOutputTexture());
        }
        mProcessFilter.draw();

        mSlideFilterGroup.onDrawFrame(mProcessFilter.getOutputTexture());
        if (mGroupFilter != null){
            EasyGlUtils.bindFrameTexture(fFrame[0],fTexture[0]);
            GLES20.glViewport(0,0,viewWidth,viewHeight);
            mGroupFilter.onDrawFrame(mSlideFilterGroup.getOutputTexture());
            EasyGlUtils.unBindFrameBuffer();
            mProcessFilter.setTextureId(fTexture[0]);
        }else {
            mProcessFilter.setTextureId(mSlideFilterGroup.getOutputTexture());
        }
        mProcessFilter.draw();

        GLES20.glViewport(0,0,viewWidth,viewHeight);

        mShow.setTextureId(mProcessFilter.getOutputTexture());
        mShow.draw();
    }
    public SurfaceTexture getSurfaceTexture(){
        return surfaceTexture;
    }//此处将surfaceTexture传递给解码的surface

   public void setRotation(int rotation){
        this.rotation=rotation;
        if(mPreFilter!=null){
            mPreFilter.setRotation(this.rotation);
        }
    }
    /**切换开启美白效果*/
    public void switchBeauty(){
        isBeauty = !isBeauty;
    }
    /**
     * 是否开启美颜功能
     * */
    public void isOpenBeauty(boolean isBeauty){
        this.isBeauty = isBeauty;
    }
    /**
     * 触摸事件监听
     * */
    public void onTouch(MotionEvent event){
        mSlideFilterGroup.onTouchEvent(event);
    }
    /**
     * 滤镜切换的监听
     * */
    public void setOnFilterChangeListener(SlideGpuFilterGroup.OnFilterChangeListener listener){
        mSlideFilterGroup.setOnFilterChangeListener(listener);
    }

    public void checkGlError(String s) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(s + ": glError " + error);
        }
    }

    public void setGpuFilter(GPUImageFilter filter) {
        if (filter != null){
            mGroupFilter = filter;
            mGroupFilter.init();
            mGroupFilter.onDisplaySizeChanged(viewWidth, viewWidth);
            mGroupFilter.onInputSizeChanged(viewWidth,viewHeight);
        }

    }
}
