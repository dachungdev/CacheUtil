package com.dachung.getvediofrominternet.Util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.VideoView;

import com.dachung.getvediofrominternet.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VideoLoader {

    private static final String TAG = "VideoLoader";

    // 内存缓存
    private LruCache<String, File> mMemoryCache;
    // 磁盘缓存
    private File diskCacheDirectory;
//    private DiskLruCache mDiskLruCache;

    // 网络拉取文件组件
    private OkHttpClient client;

    // 上下文
    private Context mContext;
    // 消息回传的结果
    public static final int MESSAGE_POST_RESULT = 1;

    // 线程池相关参数
    // CPU数量
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // 线程池核心线程数目
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    // 线程池最大线程数目
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    // 非核心线程闲置时间
    private static final long KEEP_ALIVE = 10L;

    // 缓存相关参数
    //
//    private static final int TAG_KEY_URI = R.id.fullscreen_videoview;
    // 磁盘缓存容量大小
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50;
    // IO缓冲区大小
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    // 磁盘缓存下标
    private static final int DISK_CACHE_INDEX = 0;
    // 磁盘缓存是否已建立
    private boolean mIsDiskLruCacheCreated = false;

    // 用于标识每个线程
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {

        // 带有原子性的参数
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "ImageLoader#" + mCount.getAndIncrement());
        }
    };

    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
            new LinkedBlockingDeque<Runnable>(), sThreadFactory
    );

//    // 消息机制，用于切换线程
//    private Handler mMainHandler = new Handler(Looper.getMainLooper()){
//        @Override
//        public void handleMessage(Message msg){
//            // 获取要加载的目标文件
//            LoaderResult result = (LoaderResult) msg.obj;
//            VideoView videoView = result.videoView;
//            String uri = (String) videoView.getTag(TAG_KEY_URI);
//            // 判断是否为同一个uri
//            if (uri.equals(result.uri)){
//                videoView.setVideoPath(result.file.getPath());
//                videoView.start();
//            } else {
//                Log.w(TAG, "the video path has changed");
//            }
//        };
//    };

    // 类构造器
    private VideoLoader(Context context){
        //todo 获取上下文
        mContext = context.getApplicationContext();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, File>(cacheSize){

            // 该方法用于计算缓存对象的大小
            @Override
            protected  int sizeOf(String key, File file){
                // 通过强转返回缓存文件的大小
                return (int)file.length() / 1024;
            }
        };

        // 获取硬盘缓存文件夹
        diskCacheDirectory = getDiskCacheDir(mContext, "video");
        // 判断该文件夹是否存在
        if (!diskCacheDirectory.exists()){
            // 不存在则创建这个文件夹
            Log.d(TAG, "VideoLoader: 磁盘缓存文件夹不存在");
            diskCacheDirectory.mkdirs();
        }

        client = new OkHttpClient.Builder().build();

//        if(getUseableSpace(diskCacheDir) > DISK_CACHE_SIZE){
//            try {
//                // 创建磁盘缓存
//                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
//                mIsDiskLruCacheCreated = true;
//            } catch (IOException e){
//                e.printStackTrace();
//            }
//        }
    }

    // 返回一个VideoLoader的实例
    public static VideoLoader build(Context context){
        return new VideoLoader(context);
    }

    // 将文件添加至内存缓存
    private void addFileToMemoryCache(String key, File file){
        if(getFileFromMemCache(key) == null){
            mMemoryCache.put(key, file);
        }
    }

    // 根据缓存key从内存缓存中取出缓存文件
    private File getFileFromMemCache(String key){
        return mMemoryCache.get(key);
    }

    // 将视频文件绑定在控件上，对外接口真正的绑定操作是重载方法
    public void bindFile(final String uri, final VideoView videoView){
        bindFile(uri, videoView, 0, 0);
    }

    // bindFile重载方法
    public void bindFile(final String uri, final VideoView videoView, final int reqWidth, final int reqHeight){
//        videoView.setTag(TAG_KEY_URI, uri);
//        File file = loadFileFromMemCache(uri);
//        if (file != null){
//            videoView.setVideoPath(file.getPath());
//            return;
//        }
//
//        // 加载文件是IO操作，所以将其放入线程池中执行
//        Runnable loadFileTask = new Runnable() {
//            @Override
//            public void run() {
//                File file = loadFile(uri);
//                if (file != null){
//                    LoaderResult result = new LoaderResult(videoView, uri, file);
//                    mMainHandler.obtainMessage(MESSAGE_POST_RESULT, result).
//                    sendToTarget();
//                }
//            }
//        };
//        THREAD_POOL_EXECUTOR.execute(loadFileTask);
    }

    // 加载文件
    public File loadFile(String uri){
        // 从内存缓存中加载
        File file = loadFileFromMemCache(uri);
        if (file != null){
            Log.d(TAG, "loadFile, 内存缓存有货，url:" + uri);
            return file;
        }

        try {
            // 从磁盘缓存中加载
            file = loadFileFromDiskCache(getNameFromUrl(uri));
            if (file.length() != 0){
                Log.d(TAG, "loadFile, 磁盘缓存有货，url:" + uri);
                return file;
            }

            // 网路拉取
            file = loadFileFromHttp(uri);
            Log.d(TAG, "loadFile, url:" + uri);
        } catch (IOException e){
            e.printStackTrace();
        }

        // 若文件为空且磁盘缓存仍为创建
        if (file == null && !mIsDiskLruCacheCreated){
            Log.w(TAG, "encounter error, DiskLruCache is not created.");
            file = downloadFileFromUrl(uri);
        }
        return file;
    }

    //  todo 从下载链接中解析出文件名
    private String getNameFromUrl(String url){
        return url.substring(url.lastIndexOf("/") + 1);
    }

    // 从内存缓存中获取文件
    private File loadFileFromMemCache(String url){
        final String key = hashKeyFromUrl(url);
        File file = getFileFromMemCache(key);
        return file;
    }

    // 从网路拉取文件，在里面进行读取磁盘缓存的操作
    private File loadFileFromHttp(String url) throws IOException{
        if (Looper.myLooper() == Looper.getMainLooper()){
            throw new RuntimeException("can't not visit network from UI Thread.");
        }
        // 若磁盘缓存为空，则直接返回null
        if (diskCacheDirectory.length() == 0){
            Log.d(TAG, "loadFileFromHttp: 磁盘缓存为空，则直接返回null");
            return null;
        }

        // 建立缓存Key
        String key = getNameFromUrl(url);
//        // 通过editor对象对磁盘缓存进行编辑操作
//        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
//        if (editor != null){
//            // 获取磁盘缓存输出流
//            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
//            if (downloadUrlToStream(url, outputStream)){
//                editor.commit();
//            } else {
//                editor.abort();
//            }
//            mDiskLruCache.flush();
//        }

        downloadUrlToStream(url);

        return loadFileFromDiskCache(key);
    }

    // 从磁盘缓存中获取文件
    private File loadFileFromDiskCache(String key){
        // IO操作是耗时操作，所以建议不要在主线程中进行
        if (Looper.myLooper() == Looper.getMainLooper()){
            Log.w(TAG, "It's not recommended to load file from UI Thread");
        }
        // 若磁盘缓存为空，则直接返回空
        if (diskCacheDirectory == null){
            // 磁盘缓存文件夹为空，将会启用网络传输
            Log.d(TAG, "loadFileFromDiskCache: 磁盘缓存文件夹为空，将会启用网络传输");
            return  null;
        }

        // 将磁盘存储地址与文件名取出该文件
//        String key = getNameFromUrl(url);
//        Log.d(TAG, "-----------------loadFileFromDiskCache: " + "得到的key为：" + key + ".0");

        File file = new File(diskCacheDirectory, key);
        Log.d(TAG, "loadFileFromDiskCache: 磁盘缓存文件夹名字:" + diskCacheDirectory.toString());
        Log.d(TAG, "------------------loadFileFromDiskCache: length:" + file.length() + "");
        addFileToMemoryCache(key,file);
        return file;
    }

    // 同步方法利用okhttp数据流下载
    public void downloadUrlToStream(String urlString){

        Request request = new Request.Builder().url(urlString).build();

        // 调用同步请求
        Call call = client.newCall(request);

        try {
            Response response = call.execute();
            Log.d(TAG, "downloadUrlToStream: " + "网络拉取数据成功");

            InputStream is = null;
            byte[] buf = new byte[2048];
            int len = 0;
            // 输出流
            FileOutputStream fos = null;

            try {
                // 获取相应response比特流
                is = response.body().byteStream();
                // 获取文件大小
                long total = response.body().contentLength();
                System.out.println("---------------------------" + total);
                File file = new File(diskCacheDirectory, getNameFromUrl(urlString));
                // 获取文件输出流
                fos = new FileOutputStream(file);
                long sum = 0;
                while ((len = is.read(buf)) != -1){
                    fos.write(buf, 0, len);
                    sum += len;
                }
                fos.flush();
            } catch (Exception e){
                Log.w(TAG, "downloadUrlToStream: " + "下载失败，未知原因" );
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

        } catch (IOException e){
            e.printStackTrace();
        }

//        downloadUtil = DownloadUtil.get();
//        downloadUtil.download(urlString, diskCacheDirectory.getPath(), new DownloadUtil.OnDownListener() {
//            @Override
//            public void onDownloadSuccess() {
//
//            }
//
//            @Override
//            public void onDownloading(int progress) {
//
//            }
//
//            @Override
//            public void onDownloadFailed(String reason) {
//                Log.d(TAG, "onDownloadFailed: " + reason);
//            }
//        });

//        HttpURLConnection urlConnection = null;
//        // 带有缓冲的输出流
//        BufferedOutputStream out = null;
//        // 带有缓冲的输入流
//        BufferedInputStream in = null;
//        try{
//            // 要下载的文件地址url
//            final URL url = new URL(urlString);
//            // 生成HttpURLConnection对象
//            urlConnection = (HttpURLConnection) url.openConnection();
//
//            // 根据获取回传的数据流
//            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
//            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
//
//            Log.d(TAG, "------------------downloadUrlToStream: " + "开始下载");
//            int b;
//            while ((b = in.read()) != -1){
//                out.write(b);
//            }
//            Log.d(TAG, "------------------downloadUrlToStream: " + "下载完成");
//            return true;
//        } catch (IOException e){
//            Log.e(TAG, "downloadBitmap failed." + e);
//        } finally {
//            if (urlConnection != null){
//                urlConnection.disconnect();
//            }
//            NetworkUtil.closeTheConn(out);
//            NetworkUtil.closeTheConn(in);
//        }
//        return false;
    }

    // 从url中下载文件
    private File downloadFileFromUrl(String urlString){
        File file = null;
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
            file = new File("a.txt");
        } catch (Exception e){
            Log.e(TAG, "Error in downloadFile:" + e);
        } finally {
            if (urlConnection != null){
                urlConnection.disconnect();
            }
            NetworkUtil.closeTheConn(in);
        }
        return file;
    }

    // 用以索引文件
    private String hashKeyFromUrl(String url){
        String cacheKey;
        try{
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(url.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    //
    private String bytesToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++){
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1){
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    // 得到磁盘缓存文件夹
    public File getDiskCacheDir(Context context, String uniqueName){
        // 判断外部存储是否可用
        boolean externalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if(externalStorageAvailable){
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            // 若磁盘缓存不可用，则返回内存缓存文件夹
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    // 计算可用空间并返回
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private long getUseableSpace(File path){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){
            return path.getUsableSpace();
        }
        // 若版本很低则会调用以下代码
        final StatFs stats = new StatFs(path.getPath());
        return (long)stats.getBlockSize() * (long) stats.getAvailableBlocksLong();
    }

    // 加载结果
    private static class LoaderResult{
        public VideoView videoView;
        public String uri;
        public File file;

        public LoaderResult(VideoView videoView, String uri, File file){
            this.videoView = videoView;
            this.uri = uri;
            this.file = file;
        }
    }
}
