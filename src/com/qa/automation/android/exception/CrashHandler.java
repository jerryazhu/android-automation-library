package com.qa.automation.android.exception;

import android.content.Context;
import android.util.Log;
import com.qa.automation.android.util.AppInfoUtil;
import com.qa.automation.android.util.DeviceUtil;
import com.qa.automation.android.util.email.MailSender;

import javax.mail.MessagingException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * The type Crash handler.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = CrashHandler.class.getSimpleName();

    private static CrashHandler instance; // 单例模式
    private Context context; // 程序Context对象
    private Thread.UncaughtExceptionHandler defaultHandler; // 系统默认的UncaughtException处理类
    private String emailTo = "lwfwind@126.com";
    private String sUserInfo = "";

    private CrashHandler() {

    }

    /**
     * 获取CrashHandler实例
     *
     * @return CrashHandler instance
     */
    public static CrashHandler getInstance() {
        if (instance == null) {
            synchronized (CrashHandler.class) {
                if (instance == null) {
                    instance = new CrashHandler();
                }
            }
        }
        return instance;
    }

    /**
     * Gets email to.
     *
     * @return the email to
     */
    public String getEmailTo() {
        return emailTo;
    }

    /**
     * Sets email to.
     *
     * @param emailTo the email to
     */
    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    /**
     * 异常处理初始化
     *
     * @param context the context
     */
    public void init(Context context) {
        this.context = context;
        // 获取系统默认的UncaughtException处理器
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

        // 自定义错误处理
        boolean res = handleException(ex);
        if (!res && defaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            defaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.w(TAG, "error : ", e);
            }
            // 退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex exception
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(final Throwable ex) {
        if (ex == null) {
            return false;
        }

        new Thread() {
            @Override
            public void run() {
                Log.w(TAG, "error : ", ex);
                ;
                try {
                    sUserInfo = getUserInfo();
                    String[] emails = emailTo.split(" ");
                    MailSender.sendTextMail("android_automation@126.com", "Automation123", "smtp.126.com",
                            "android-automation-library uncatched exception", sUserInfo + getErrorTrace(ex),
                            null, emails);
                } catch (MessagingException e) {
                    Log.w(TAG, "send mail error : ", e);
                }
            }
        }.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Gets error trace.
     *
     * @param ex the ex
     * @return the error trace
     */
    public String getErrorTrace(Throwable ex) {
        StringWriter stringWrite = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWrite);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        return stringWrite.toString();
    }

    /**
     * Gets user info.
     *
     * @return the user info
     */
    public String getUserInfo() {
        StringBuilder info = new StringBuilder();
        String newline = System.lineSeparator();
        info.append("time:").append(new Date().toString()).append(newline);
        info.append("processName:").append(AppInfoUtil.getCurProcessName()).append(newline);
        info.append("app version:").append(AppInfoUtil.getAPPVersion()).append(newline);
        info.append("app channel:").append(AppInfoUtil.getChannelName()).append(newline);
        info.append("device model:").append(DeviceUtil.getDeviceMode()).append(newline);
        info.append("device id:").append(DeviceUtil.getDeviceId()).append(newline);
        info.append("device version code:").append(DeviceUtil.getDeviceVersionCode()).append(newline);
        info.append("device net state:").append(DeviceUtil.getNetworkType()).append(newline);
        info.append(newline);
        info.append("====================");
        info.append("error message:");
        info.append(newline);
        return info.toString();
    }
}