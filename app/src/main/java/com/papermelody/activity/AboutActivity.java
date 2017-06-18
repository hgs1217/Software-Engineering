package com.papermelody.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.papermelody.R;

import butterknife.BindView;

/**
 * Created by HgS_1217_ on 2017/3/18.
 */

public class AboutActivity extends BaseActivity {
    /**
     * 用例：查看关于
     * 关于页面
     */
    @BindView(R.id.webview)
    WebView webView;
    @BindView(R.id.toolbar2)
    Toolbar xx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        xx.setTitle("Surprise!");
        WebSettings settings = webView.getSettings();
        webView.loadUrl("https://yujiepan.github.io/fun.html");
        settings.setSupportZoom(true);          //支持缩放
        settings.setBuiltInZoomControls(true);  //启用内置缩放装置
        settings.setJavaScriptEnabled(true);    //启用JS脚本


    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_about;
    }


    private void xxx() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        //0:low  1:middle  2:high
        pref.getString("skin_sensi", "-1");
        pref.getString("motion_sensi", "-1");
        pref.getString("color_sensi", "-1");
        pref.getBoolean("high_performance", true);
        pref.getBoolean("heart_show", true);
        pref.getBoolean("debug_info", false);
    }


}


