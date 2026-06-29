# Shizuku
-keep interface rikka.shizuku.** { *; }

# Custom AIDL interface for Shizuku IPC
-keep class akihz.anlaki.dev.ICommandService { *; }
-keep class akihz.anlaki.dev.ICommandService$Stub { *; }
-keep class akihz.anlaki.dev.ICommandService$Stub$Proxy { *; }
-keep class akihz.anlaki.dev.data.ICommandServiceImpl { *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Hilt generated components
-keep class * extends dagger.hilt.android.components.** { *; }

# Keep @HiltViewModel constructors (R8 can strip these in AGP 8.9+)
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

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
