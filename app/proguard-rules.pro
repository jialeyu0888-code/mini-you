# Room 实体与 DAO 由注解处理器生成代码，保留实体字段名以便 JSON 转换
-keep class com.miniyou.app.data.db.** { *; }

# OkHttp 平台探测
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
