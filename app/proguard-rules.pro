-keepattributes *Annotation*, InnerClasses, Signature, Exception, SourceFile, LineNumberTable

-keep class com.nyaa.aniyaa.data.model.** { *; }
-keep class com.nyaa.aniyaa.data.db.** { *; }
-dontwarn androidx.room.paging.**

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.jspecify.annotations.NullMarked
-dontwarn io.noties.markwon.**
-dontwarn coil.**

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
