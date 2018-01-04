package com.yaoyi.myue.filter;

import android.content.res.Resources;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

/**
 * Description: 加载默认的滤镜的filter
 */
public class OesFilter extends AFilter{

    public OesFilter(Resources mRes) {
        super(mRes);
    }

    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/oes_base_vertex.sh","shader/oes_base_fragment.sh");
    }

    @Override
    protected void onBindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0+getTextureType());//选择激活纹理单元0
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,getTextureId());//用GL_TEXTURE_EXTERNAL_OES这种纹理来绑定输入纹理到以已经激活到纹理单元0
        GLES20.glUniform1i(mHTexture,getTextureType());//告诉gl程序使用激活的纹理单元0
    }

    @Override
    protected void onSizeChanged(int width, int height) {

    }

}
