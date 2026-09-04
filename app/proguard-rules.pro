# Firestore deserializes POJOs via reflection - keep model classes and their members.
-keepclassmembers class com.mhoehn.freunde.data.model.** {
    *;
}
-keep class com.mhoehn.freunde.data.model.** { *; }
