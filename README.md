# CacheUtil

##基于LruCache与OkHttp的音视频文件缓存工具##

**将用于播放的载体（MusicPlayer或VideoPlayer）与目标url传入FileLoader**
**内部会通过，是否有内存缓存->是否有本地文件->网络拉取的判断与执行顺序对对应的音视频文件进行加载**

DACHUNG 19.9.20