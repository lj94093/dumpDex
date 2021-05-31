package com.wrbug.dumpdex;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HotLoad implements IXposedHookLoadPackage {
    private static final String TAG = "HotLoad";
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {

        if (lpparam == null) {
            return;
        }
        hotLoad("com.wrbug.dumpdex.XposedInit", "handleLoadPackage", lpparam);
//        Log.e(TAG, "Load app packageName:" + lpparam.packageName);

    }
    /**调用真正的包含hook逻辑的方法
     * @param hotClass  指定由哪一个类处理相关的hook逻辑
     * @param hotMethod 处理相关的hook逻辑的方法名
     * @param loadPackageParam 传入XC_LoadPackage.LoadPackageParam参数
     * @throws Exception
     */
    private void hotLoad(String hotClass, String hotMethod, XC_LoadPackage.LoadPackageParam loadPackageParam){
        //利用上面实现的代码寻找apk文件
        File apkFile = findApkFile();
//        XposedBridge.log("find apk file:"+apkFile.getName());
        //自定义Classloader
        PathClassLoader pathClassLoader = new PathClassLoader(apkFile.getAbsolutePath(), ClassLoader.getSystemClassLoader());
        try {
            //使用反射的方式去调用具体的Hook逻辑
            final Class<?> cls =pathClassLoader.getClass().forName(hotClass, true, pathClassLoader);
            Object instance = cls.newInstance();
            Method method = cls.getDeclaredMethod(hotMethod, XC_LoadPackage.LoadPackageParam.class);
//            XposedBridge.log("invoke target method:"+hotClass+":"+hotMethod);
            method.invoke(instance, loadPackageParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File findApkFile(){
        final String packageName = HotLoad.class.getPackage().getName();
        String filePath = String.format("/data/app/%s-%s.apk", packageName, 1);
        if (!new File(filePath).exists()) {
            filePath = String.format("/data/app/%s-%s.apk", packageName, 2);
            if (!new File(filePath).exists()) {
                filePath = String.format("/data/app/%s-%s/base.apk", packageName, 1);
                if (!new File(filePath).exists()) {
                    filePath = String.format("/data/app/%s-%s/base.apk", packageName, 2);
                    if (!new File(filePath).exists()) {
                        XposedBridge.log("Error:在/data/app找不到APK文件" + packageName);
                        return null;
                    }
                }
            }
        }
        return new File(filePath);
    }
}
