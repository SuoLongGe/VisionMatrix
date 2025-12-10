# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep NCNN classes
-keep class com.tencent.ncnn.** { *; }

# Keep detection classes
-keep class com.visionmatrix.ctrlf.** { *; }


