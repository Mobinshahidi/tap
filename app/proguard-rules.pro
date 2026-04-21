# Keep service declarations used by Android runtime.
-keep class com.tap.apk.TapAccessibilityService { *; }

# Keep model classes for DataStore string parsing stability.
-keep class com.tap.apk.models.** { *; }
