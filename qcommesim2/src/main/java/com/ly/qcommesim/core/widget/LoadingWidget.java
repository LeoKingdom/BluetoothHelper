package com.ly.qcommesim.core.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ly.qcommesim.R;
import com.ly.qcommesim.core.utils.ViewVisibleUtils;


/**
 * 通用的Loading框
 *
 */
public class LoadingWidget extends RelativeLayout {

    private ImageView ivLoading;//Loadindh图片
    private LinearLayout llBG;//背景图
    private RotateAnimation ra;//动画
    private TextView tvLoading;

    public LoadingWidget(Context context) {
        this(context, null, 0);
    }

    public LoadingWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        View inflate = View.inflate(context, R.layout.widget_loading, this);
        ivLoading = inflate.findViewById(R.id.iv_loading);
        llBG = inflate.findViewById(R.id.ll_bg);
        tvLoading = inflate.findViewById(R.id.tv_loading);
    }

    /**
     * 显示
     */
    public void show() {
        ra = new RotateAnimation(0, 360, 1, 0.5f, 1, 0.5f);
        ra.setDuration(1500);
        ra.setRepeatCount(Animation.INFINITE);
        ra.setRepeatMode(Animation.INFINITE);
        ra.setInterpolator(new LinearInterpolator());
        ra.setFillAfter(true);// 该句一定要加--> 否则setVisibility(GONE)无效
        ivLoading.setAnimation(ra);
        ra.startNow();
        ivLoading.startAnimation(ra);
        ViewVisibleUtils.showFade(this, 0f, 1f, 400);
    }

    /**
     * 隐藏
     */
    public void hide() {
        if (ra != null) {
            ra.cancel();
        }
        ivLoading.clearAnimation();// 该句一定要加--> 否则setVisibility(GONE)无效
        ra = null;
        ViewVisibleUtils.hideFade(this, 1f, 0f, 400);
    }

    /**
     * 获取转圈的文本
     * @return tvLoading
     */
    public TextView getTvLoading(){
        return this.tvLoading;
    }

    /**
     * 更改加载text
     * @param text 加载的text
     */
    public void setLoadingText(String text){
        tvLoading.setText(text);
    }

    /**
     * 更改加载text
     * @param textId 加载的text的id
     */
    public void setLoadingText(int textId){
        tvLoading.setText(textId);
    }

    /**
     * 设置text显示或隐藏
     * @param visibility 显示或隐藏
     */
    public void setLoadingTextVisible(int visibility){
        tvLoading.setVisibility(visibility);
    }

    /**
     * 设置加载背景色
     * @param colorId
     */
    public void setLoadingBgColor(int colorId){
        llBG.setBackgroundColor(getContext().getResources().getColor(colorId));
    }

    /**
     * 设置加载背景图
     * @param drawableId
     */
    public void setLoadingBg(int drawableId){
        llBG.setBackgroundResource(drawableId);
    }

}
