# =================================================================
# 🛡️ Ludo King Hardened R8 / ProGuard Obfuscation & Shrinking Rules
# =================================================================

# 1. Aggressive Obfuscation, Flattening & Optimization
-allowaccessmodification
-repackageclasses ''
-renamesourcefileattribute ''
-keepattributes *Annotation*,Signature

# 2. Strip all Debugging Logs and Line Number metadata
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# 3. Preserve Activity Class Names ONLY (Do NOT keep inner classes or methods!)
-keep class com.smarty.sunny.SplashActivity
-keep class com.smarty.sunny.HomeActivity
-keep class com.smarty.sunny.MainActivity
-keep class com.smarty.sunny.AudioManager { *; }
-keep class com.smarty.sunny.TelegramDiceController { *; }

# 4. Preserve Essential View Constructors
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 5. Glide Rules
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**
-dontwarn android.support.**
-dontwarn androidx.**