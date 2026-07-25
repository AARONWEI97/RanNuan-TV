# RanNuan TV — 保留所有自有代码
-keep class com.rannuan.tv.** { *; }
-keep interface com.rannuan.tv.** { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
# Retrofit 通过 suspend 方法最后一个 Continuation<T> 参数恢复真实返回类型。
# 如果 R8 将它改写成裸 Continuation，运行时会发生 Class -> ParameterizedType 崩溃。
-keep interface kotlin.coroutines.Continuation { *; }

# Gson — 泛型反序列化核心修复
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# 防止 R8 把 ParameterizedType 干掉
-keep class java.lang.reflect.** { *; }
-dontwarn com.google.gson.**
-dontwarn sun.misc.Unsafe

# 数据模型 — 不可混淆字段名（Gson 按名序列化）
-keepclassmembers class com.rannuan.tv.data.model.** {
    <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ExoPlayer
-keep class androidx.media3.** { *; }

# Coil
-dontwarn coil.**
