-keepattributes *Annotation*, InnerClasses, Signature, Exception, SourceFile, LineNumberTable

-keep class com.nyaa.aniyaa.data.model.** { *; }

-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

-keep class org.jsoup.** { *; }

-keep class io.noties.markwon.** { *; }
-keep class io.noties.markwon.image.** { *; }
-dontwarn io.noties.markwon.**

-keep class coil.** { *; }
-dontwarn coil.**

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
