# Shizuku
-keep interface rikka.shizuku.** { *; }

# Custom AIDL interface for Shizuku IPC
-keep class akihz.anlaki.dev.ICommandService { *; }
-keep class akihz.anlaki.dev.ICommandService$Stub { *; }
-keep class akihz.anlaki.dev.ICommandService$Stub$Proxy { *; }
-keep class akihz.anlaki.dev.data.ICommandServiceImpl { *; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
