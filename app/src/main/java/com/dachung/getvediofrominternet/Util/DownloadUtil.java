package com.dachung.getvediofrominternet.Util;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 视频下载工具类
 */

public class DownloadUtil {

    private static DownloadUtil downloadUtil;
    private final OkHttpClient okHttpClient;

    // 单例类实现
    public static DownloadUtil get(){
        if (downloadUtil == null){
            downloadUtil = new DownloadUtil();
        }
        return downloadUtil;
    }

    private DownloadUtil(){
        okHttpClient = new OkHttpClient();
    }

    // 下载实现
    public void download(final String url, final String saveDir, final OnDownListener listener){

        String fileName = getNameFromUrl(url);
        File file = new File(saveDir,fileName);
        if(file != null){
            listener.onDownloadFailed("文件已存在");
            return;
        }

        // 建立请求request
        Request request = new Request.Builder().url(url).build();
        // 建立请求call接口
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                listener.onDownloadFailed("未知原因");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                // 输出流
                FileOutputStream fos = null;

                // 存储下载文件的目录
                String savePath = isExistDir(saveDir);
                try {
                    // 获取相应response比特流
                    is = response.body().byteStream();
                    // 获取文件大小
                    long total = response.body().contentLength();
                    System.out.println("---------------------------" + total);
                    File file = new File(savePath, getNameFromUrl(url));
                    // 获取文件输出流
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1){
                        fos.write(buf, 0, len);
                        sum += len;
                        // 获取下载进度百分比
                        int progress = (int) (sum * 1.0f / total * 100);
                        // 下载中
                        listener.onDownloading(progress);
                    }
                    fos.flush();
                    // 下载完成
                    listener.onDownloadSuccess();
                } catch (Exception e){
                    listener.onDownloadFailed("未知原因");
                } finally {
                    try {
                        if (is != null){
                            is.close();
                        }
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                    try {
                        if (fos != null){
                            fos.close();
                        }
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }

            }
        });

    }

    private String isExistDir(String saveDir) throws IOException{
        // 下载位置
        File downloadFile = new File(Environment.getExternalStorageDirectory(), saveDir);
        // 若无此文件夹，则新建
        if (!downloadFile.mkdirs()){
            downloadFile.createNewFile();
        }
        // 取出绝对路径
        String savePath = downloadFile.getAbsolutePath();
        return savePath;
    }

    // 从下载链接中解析出文件名
    private String getNameFromUrl(String url){
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public interface OnDownListener{
        //下载成功
        void onDownloadSuccess();

        //下载进度
        void onDownloading(int progress);

        //下载失败
        void onDownloadFailed(String reason);
    }
}
