package com.ly.blehelper.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ly.blehelper.R;
import com.ly.blehelper.utils.ViewVisibleUtils;


/**
 * 升级进度条弹框
 *
 */
public class ProgressDialogWidget extends RelativeLayout {

    private TextView tvTitle;//标题
    private TextView tvOk;//确定
    private TextView tvCancel;//取消
    private View tvSplit;//分割线
    private ImageView ivBg;//背景
    private ProgressBar progressBar;//进度条
    private TextView progressNumTv;//进度
    private boolean outSideDimiss=false;

    private Button closeBtn;//关闭按钮


    private TextView currentPacket;
    public ProgressDialogWidget(Context context) {
        this(context, null, 0);
    }

    public ProgressDialogWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressDialogWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        View inflate = View.inflate(context, R.layout.widget_dialog_progress, this);
        tvTitle = inflate.findViewById(R.id.progress_tv_title);
        tvOk = inflate.findViewById(R.id.tv_click_ok);
        tvCancel = inflate.findViewById(R.id.tv_click_cancel);
        tvSplit = inflate.findViewById(R.id.v_bottom_split);
        ivBg = inflate.findViewById(R.id.iv_bg);
        progressBar=inflate.findViewById(R.id.progress_bar);
        progressNumTv=inflate.findViewById(R.id.progress_bar_tv);
        currentPacket=inflate.findViewById(R.id.progress_tv_pro);
        closeBtn=inflate.findViewById(R.id.progress_bar_close_btn);
        ivBg.setOnClickListener(null);
    }

    public void setOutsideDimiss(boolean outsideDimiss){
        this.outSideDimiss=outsideDimiss;
        if (outsideDimiss){
            ivBg.setOnClickListener((view)->{
                hide();
            });
        }else {
            ivBg.setOnClickListener(null);
        }
    }

    public void setProgressNumTvText(String text){
        if (progressNumTv!=null){
            progressNumTv.setText(text);
        }
    }

    public void setProgress(int progress){
        if (progressBar!=null){
            progressBar.setProgress(progress);
        }
    }

    public void setProgressMax(int max){
        if (progressBar!=null){
            progressBar.setMax(max);
        }
    }

    public void showCloseBtn(){
        if (closeBtn!=null){
            closeBtn.setVisibility(VISIBLE);
        }
    }

    public void showCloseBtnWithText(String txt){
        if (closeBtn!=null){
            closeBtn.setVisibility(VISIBLE);
            closeBtn.setText(txt);
        }
    }

    public void closeListener(){
        if (closeBtn!=null){
            closeBtn.setOnClickListener((view)-> {
              hide();
            });
        }
    }

    /**
     * 显示
     */
    public void show() {
        ViewVisibleUtils.showFade(this,0f,1f,400);
    }

    /**
     * 隐藏
     */
    public void hide() {
        ViewVisibleUtils.hideFade(this,1f,0f,400);
    }

    /**
     * 单选：只有tvOk可以用
     */
    public void setSingleChoice(){
        tvSplit.setVisibility(VISIBLE);
        tvCancel.setVisibility(GONE);
        tvOk.setVisibility(VISIBLE);
    }

    /**
     * 双选
     */
    public void setDoubleChoice(){
        tvSplit.setVisibility(VISIBLE);
        tvCancel.setVisibility(VISIBLE);
        tvOk.setVisibility(VISIBLE);
    }

    public Button getCloseBtn() {
        return closeBtn;
    }


    /**
     * 标题
     *
     * @return the text view
     */
    public TextView getTvTitle() {
        return this.tvTitle;
    }

    public TextView getCurrentPacket() {
        return currentPacket;
    }


    /**
     * 进度
     * @return
     */
    public TextView getProgressNumTv(){return this.progressNumTv;}

    /**
     * 进度条
     * @return
     */
    public ProgressBar getProgressBar(){return this.progressBar;}

    /**
     * 确定
     *
     * @return the text view
     */
    public TextView getTvOk() {
        return this.tvOk;
    }

    /**
     * 取消
     *
     * @return the text view
     */
    public TextView getTvCancel() {
        return this.tvCancel;
    }

    /**
     * 设置标题
     * @param resourceId string id
     */
    public ProgressDialogWidget setTvTitle(int resourceId){
        tvTitle.setText(resourceId);
        return this;
    }

    /**
     * 设置标题
     * @param title 内容
     */
    public ProgressDialogWidget setTvTitle(String title){
        tvTitle.setText(title);
        return this;
    }

    /**
     * 设置ok
     * @param resourceId 内容id
     * @return
     */
    public ProgressDialogWidget setTvOk(int resourceId){
        tvOk.setText(resourceId);
        return this;
    }

    /**
     * 设置cancel
     * @param resourceId 内容id
     * @return
     */
    public ProgressDialogWidget setTvCancel(int resourceId) {
        tvCancel.setText(resourceId);
        return this;
    }
}
