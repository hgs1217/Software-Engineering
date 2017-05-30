package com.papermelody.activity;

import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.papermelody.R;
import com.papermelody.fragment.CommentFragment;
import com.papermelody.fragment.ListenFragment;
import com.papermelody.fragment.MusicHallFragment;
import com.papermelody.model.OnlineMusic;
import com.papermelody.model.response.HttpResponse;
import com.papermelody.util.NetworkFailureHandler;
import com.papermelody.util.RetrofitClient;
import com.papermelody.util.SocialSystemAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import butterknife.BindView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by HgS_1217_ on 2017/4/10.
 */

public class OnlineListenActivity extends BaseActivity {
    /**
     * 用例：用户试听
     * 音乐圈中网络作品的试听页面，其中试听部分是用Fragment实现，音乐信息、评论等内容布局在Activity中
     */
    @BindView(R.id.btn_download)
    Button btnDownload;
    @BindView(R.id.music_view_num)
    TextView viewNum;
    @BindView(R.id.music_upvote_num)
    TextView upvoteNum;
    @BindView(R.id.btn_music_upvote)
    Button btnUpvote;

    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;
    private String fileName;    // 从intent中得到文件名称，下载到本地然后播放
    private SocialSystemAPI api;
    private OnlineMusic onlineMusic;
    private Thread downloadThread;
    private Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
//         获取从音乐圈传入的onlineMusic实例
        onlineMusic = (OnlineMusic) intent.getSerializableExtra(MusicHallFragment.SERIAL_ONLINEMUSIC);
        api = RetrofitClient.getSocialSystemAPI();
        initView();

//         测试用，先同步server代码
//        fileName = onlineMusic.getFilename();
        fileName = "Kissbye.mid";
        Log.i("nib", onlineMusic.getMusicName());

        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().add(R.id.container_online_listen, ListenFragment.newInstance(fileName)).commit();
        fragmentManager.beginTransaction().add(R.id.container_comment, CommentFragment.newInstance(onlineMusic)).commit();

        downloadThread = new Thread(() -> {
            Log.i("nib", "downloading");
            String dataPath = getApplicationContext().getFilesDir().getAbsolutePath() + "/";
            String sourceURL = getString(R.string.server_ip) + "uploaded/" + fileName;
            Log.i("nib", sourceURL);
            download_2(sourceURL, dataPath, fileName);
//            boolean downloadResult = download(sourceURL, dataPath, fileName);
//            if (downloadResult) {
//                ToastUtil.showShort(R.string.download_success);
//            } else {
//                ToastUtil.showShort(R.string.download_failed);
//            }
        });

        // FIXME: 点击按钮下载不会闪退，但需返回重进一次才能播放
        btnDownload.setOnClickListener((View v) -> {
            downloadThread.start();
//            try {
//                downloadThread.join();
//            } catch (InterruptedException e) {
//                Log.i("nib", e.toString());
//            }
//            ListenFragment.refreshSource();
        });
    }

    private void initView () {
        // FIXME: 点赞数和浏览数只有每次重进后才会刷新
        viewNum.setText(String.valueOf(onlineMusic.getViewNum()));
        upvoteNum.setText(String.valueOf(onlineMusic.getUpvoteNum()));
        addViewNum();
        btnUpvote.setOnClickListener((view) -> {
            addUpvoteNum();  // FIXME: 存在可以多次点赞的bug
        });
    }

    private void addViewNum () {
        addSubscription(api.addView(onlineMusic.getMusicID())
                .flatMap(NetworkFailureHandler.httpFailureFilter)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> ((HttpResponse) response))
                .subscribe(
                        response -> {},
                        NetworkFailureHandler.basicErrorHandler
                ));
    }

    private void addUpvoteNum () {
        addSubscription(api.addUpvote(onlineMusic.getMusicID())
                .flatMap(NetworkFailureHandler.httpFailureFilter)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> ((HttpResponse) response))
                .subscribe(
                        response -> {
                            upvoteNum.setText(String.valueOf(onlineMusic.getUpvoteNum() + 1));
                        },
                        NetworkFailureHandler.basicErrorHandler
                ));
    }

    // strurl要下载的文件的url，path保存的路径，filename文件名
    private boolean download(String strurl, String path, String fileName) {
        InputStream is = null;
        OutputStream os = null;
        URL url = null;
        try {
            //创建文件夹
            File f = new File(path);
            if (!f.exists()) {
                f.mkdir();
            }
            //创建文件
            File file = new File(path + fileName);
            //判断是否存在文件
            if (file.exists()) {
                //创建新文件
                file.createNewFile();
            } else {
                file.delete();
                file.createNewFile();
            }
            //创建并打开连接
            url = new URL(strurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //文件输入流
            is = conn.getInputStream();
            //输出流
            os = new FileOutputStream(file);
            byte buffer[] = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
            os.close();
            is.close();
            Log.i("nib", "download success");
            return true;
        } catch (Exception e) {
            Log.i("nib", e.toString());
            return false;
        }
    }

    // 调用系统的下载器下载
    private void download_2(String strurl, String path, String fileName) {
        File file = new File(getExternalStorageDirectory() + "/Download/" + fileName);
        if (file.exists()) {
            file.delete();
        }
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(strurl));
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        downloadManager.enqueue(request);
        Log.i("nib", Environment.DIRECTORY_DOWNLOADS + "/" + fileName);
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_online_listen;
    }
}
