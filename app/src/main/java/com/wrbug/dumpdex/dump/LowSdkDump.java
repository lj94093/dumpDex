package com.wrbug.dumpdex.dump;

import android.app.Application;
import android.content.Context;

import com.wrbug.dumpdex.util.DeviceUtils;
import com.wrbug.dumpdex.util.FileUtils;
import com.wrbug.dumpdex.Native;
import com.wrbug.dumpdex.PackerInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LowSdkDump
 *
 * @author WrBug
 * @since 2018/3/23
 */
public class LowSdkDump {
    private static int dumpClassCount=0;
    private static HashSet<Object> cachedDex=new HashSet<>();
    public static void log(String txt) {

        XposedBridge.log("dumpdex.LowSdkDump-> " + txt);
    }

    public static void init(final XC_LoadPackage.LoadPackageParam lpparam, PackerInfo.Type type) {
        log("start hook Instrumentation#newApplication");
        if (DeviceUtils.supportNativeHook()) {
            Native.dump(lpparam.packageName);
        }
        if (type == PackerInfo.Type.BAI_DU) {
            return;
        }
        XposedHelpers.findAndHookMethod("android.app.Instrumentation", lpparam.classLoader, "newApplication", ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                log("Application=" + param.getResult());
                dump(lpparam.packageName, param.getResult().getClass());
                attachBaseContextHook(lpparam, ((Application) param.getResult()));
            }
        });
    }

    private static void dump(String packageName, Class<?> aClass) {
//        获取Class对象的dexCache成员
        Object dexCache = XposedHelpers.getObjectField(aClass, "dexCache");
//        log("decCache=" + dexCache);
        Object o = XposedHelpers.callMethod(dexCache, "getDex");
        if(cachedDex.contains(o)){
            return;
        }
        cachedDex.add(o);
        byte[] bytes = (byte[]) XposedHelpers.callMethod(o, "getBytes");
//        Class.dexCache.getDex().getBytes()获取到对应的dex文件
        String path = "/data/data/" + packageName + "/dump";
        File file = new File(path, "source-" + bytes.length + ".dex");
        if (file.exists()) {
            log(file.getName() + " exists");
            return;
        }
        FileUtils.writeByteToFile(bytes, file.getAbsolutePath());
    }


    private static void attachBaseContextHook(final XC_LoadPackage.LoadPackageParam lpparam, final Application application) {
        ClassLoader classLoader = application.getClassLoader();
        XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                dumpClassCount++;
                if(dumpClassCount%1000==0){
                    log("loadClass->" + param.args[0]+".Total class count:"+dumpClassCount);
                }
                Class result = (Class) param.getResult();
                if (result != null) {
                    dump(lpparam.packageName, result);
                }
            }
        });
        XposedHelpers.findAndHookMethod("java.lang.ClassLoader", classLoader, "loadClass", String.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                dumpClassCount++;
                if(dumpClassCount%1000==0){
                    log("loadClassWithclassLoader->" + param.args[0]+".Total class count:"+dumpClassCount);
                }
                Class result = (Class) param.getResult();
                if (result != null) {
//                    获取到loadClass的返回值
                    dump(lpparam.packageName, result);
                }
            }
        });
    }
}
