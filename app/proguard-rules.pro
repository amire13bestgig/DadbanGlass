# قوانین Proguard برای مدل‌های DTO دادبان و Keystore
-keep class ir.dadban.glass.model.** { *; }
-keepclassmembers class ir.dadban.glass.model.** { *; }

-keep class ir.dadban.glass.security.** { *; }
-keepclassmembers class ir.dadban.glass.security.** { *; }

# حفظ سریالایزرهای JSON
-keepclassmembers class * {
    public static ** fromJson(org.json.JSONObject);
    public org.json.JSONObject toJson();
}
