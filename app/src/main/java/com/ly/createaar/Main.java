package com.ly.createaar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/12/31 13:48
 * version: 1.0
 */
public class Main extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public void toEsim(View view){
        startActivity(new Intent(this,EsimActivity.class));
    }
    public void toUpgrade(View view){
        startActivity(new Intent(this, UpgradeActivity.class));
    }
}
