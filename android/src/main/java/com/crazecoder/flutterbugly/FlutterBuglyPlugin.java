package com.crazecoder.flutterbugly;

import android.app.Activity;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.crazecoder.flutterbugly.bean.BuglyInitResultInfo;
import com.crazecoder.flutterbugly.utils.JsonUtil;
import com.crazecoder.flutterbugly.utils.MapUtil;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.crashreport.CrashReport;

import io.flutter.BuildConfig;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** FlutterbuglyPlugin */
public class FlutterbuglyPlugin implements FlutterPlugin, MethodCallHandler {

  private MethodChannel channel;
  private Result result;
  private boolean isResultSubmitted = false;
  private static Activity activity;
  private FlutterPluginBinding flutterPluginBinding;


  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    this.flutterPluginBinding = flutterPluginBinding;
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "crazecoder/flutter_bugly");
    channel.setMethodCallHandler(this);
    activity = (Activity) flutterPluginBinding.getApplicationContext();
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    ///初始化bugly
    if(call.method.equals("initBugly")){
      if(call.hasArgument("appId")){

        ///自动初始化开关
        ///true表示app启动自动初始化升级模块; false不会自动初始化;
        ///开发者如果担心sdk初始化影响app启动速度，可以设置为false，
        ///在后面某个时刻手动调用Beta.init(getApplicationContext(),false);
        if(call.hasArgument("autoInit")){
          Beta.autoInit = Boolean.TRUE.equals(call.argument("autoInit"));
        }

        ///自动检查更新开关
        ///true表示初始化时自动检查升级; false表示不会自动检查升级,需要手动调用Beta.checkUpgrade()方法;
        if(call.hasArgument("autoCheckUpgrade")){
          Beta.autoCheckUpgrade = Boolean.TRUE.equals(call.argument("autoCheckUpgrade"));
        }

        ///升级检查周期设置
        ///若设置为60s(默认检查周期为0s)，60s内SDK不重复向后台请求策略);
        if(call.hasArgument("upgradeCheckPeriod")){
          Integer period = call.argument("upgradeCheckPeriod");
          if(period==null) period = 0;
          Beta.upgradeCheckPeriod = period;
        }

        ///延迟初始化
        ///若设置启动延时为1s（默认延时3s），APP启动1s后初始化SDK，避免影响APP启动速度;
        if (call.hasArgument("initDelay")) {
          Integer delay = call.argument("initDelay");
          if(delay==null) delay = 3;
          Beta.initDelay = delay * 1000;
        }

        ///设置开启显示打断策略
        ///设置点击过确认的弹窗在App下次启动自动检查更新时会再次显示。
        if (call.hasArgument("showInterruptedStrategy")) {
          Beta.showInterruptedStrategy = Boolean.TRUE.equals(call.argument("showInterruptedStrategy"));
        }

        ///设置是否显示消息通知
        ///如果你不想在通知栏显示下载进度，你可以将这个接口设置为false，默认值为true。
        if (call.hasArgument("enableNotification")) {
          Beta.enableNotification = Boolean.TRUE.equals(call.argument("enableNotification"));
        }

        ///设置Wifi下自动下载
        ///如果你想在Wifi网络下自动下载，可以将这个接口设置为true，默认值为false。
        if (call.hasArgument("autoDownloadOnWifi")) {
          Beta.autoDownloadOnWifi = Boolean.TRUE.equals(call.argument("autoDownloadOnWifi"));
        }

        ///设置是否显示弹窗中的apk信息
        ///如果你使用我们默认弹窗是会显示apk信息的，如果你不想显示可以将这个接口设置为false
        if (call.hasArgument("canShowApkInfo")) {
          Beta.canShowApkInfo = Boolean.TRUE.equals(call.argument("canShowApkInfo"));
        }

        ///关闭热更新能力
        ///升级SDK默认是开启热更新能力的，如果你不需要使用热更新，可以将这个接口设置为false。
        if (call.hasArgument("enableHotfix")) {
          Beta.enableHotfix = Boolean.TRUE.equals(call.argument("enableHotfix"));
        }

        String appId = call.argument("appId");
        Bugly.init(activity.getApplicationContext(), appId, BuildConfig.DEBUG);

        if (call.hasArgument("channel")) {
          String channel = call.argument("channel");
          if (!TextUtils.isEmpty(channel))
            Bugly.setAppChannel(activity.getApplicationContext(), channel);
        }
        result(getResultBean(true, appId, "Bugly 初始化成功"));
      }else {
        result(getResultBean(false, null, "Bugly appId不能为空"));
      }
    } else if (call.method.equals("setUserId")) {
      if (call.hasArgument("userId")) {
        String userId = call.argument("userId");
        Bugly.setUserId(activity.getApplicationContext(), userId);
      }
      result(null);
    } else if (call.method.equals("setUserTag")) {
      if (call.hasArgument("userTag")) {
        Integer userTag = call.argument("userTag");
        if (userTag != null)
          Bugly.setUserTag(activity.getApplicationContext(), userTag);
      }
      result(null);
    } else if (call.method.equals("putUserData")) {
      if (call.hasArgument("key") && call.hasArgument("value")) {
        String userDataKey = call.argument("key");
        String userDataValue = call.argument("value");
        Bugly.putUserData(activity.getApplicationContext(), userDataKey, userDataValue);
      }
      result(null);
    } else if (call.method.equals("checkUpgrade")) {
      boolean isManual = false;
      boolean isSilence = false;
      if (call.hasArgument("isManual")) {
        isManual = Boolean.TRUE.equals(call.argument("isManual"));
      }
      if (call.hasArgument("isSilence")) {
        isSilence = Boolean.TRUE.equals(call.argument("isSilence"));
      }
      Beta.checkUpgrade(isManual, isSilence);
      result(null);
    } else if (call.method.equals("getUpgradeInfo")) {
      UpgradeInfo strategy = Beta.getUpgradeInfo();
      result(strategy);
    } else if (call.method.equals("setAppChannel")) {
      String channel = call.argument("channel");
      if (!TextUtils.isEmpty(channel)) {
        Bugly.setAppChannel(activity.getApplicationContext(), channel);
      }
      result(null);
    } else if (call.method.equals("postCatchedException")) {
      postException(call);
      result(null);
    } else {
      result.notImplemented();
      isResultSubmitted = true;
    }

  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
    flutterPluginBinding = null;
  }


  private void result(Object object) {
    if (result != null && !isResultSubmitted) {
      if (object == null) {
        result.success(null);
      } else {
        result.success(JsonUtil.toJson(MapUtil.deepToMap(object)));
      }
      isResultSubmitted = true;
    }
  }

  private BuglyInitResultInfo getResultBean(boolean isSuccess, String appId, String msg) {
    BuglyInitResultInfo bean = new BuglyInitResultInfo();
    bean.setSuccess(isSuccess);
    bean.setAppId(appId);
    bean.setMessage(msg);
    return bean;
  }

  private void postException(MethodCall call) {
    String message = "";
    String detail = null;
    if (call.hasArgument("crash_message")) {
      message = call.argument("crash_message");
    }
    if (call.hasArgument("crash_detail")) {
      detail = call.argument("crash_detail");
    }
    if (TextUtils.isEmpty(detail)) return;
    CrashReport.postException(8, "Flutter Exception", message, detail, null);

  }
}
