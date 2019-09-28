package com.dachung.getvediofrominternet.Util;

import java.io.Closeable;
import java.io.IOException;

public class NetworkUtil {
    public static void closeTheConn(Closeable closeable){
        if (closeable != null){
            try {
                closeable.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
