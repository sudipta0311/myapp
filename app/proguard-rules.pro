# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# PDF parsing
-keep class com.tom_roush.** { *; }
-dontwarn com.tom_roush.**

# Apache POI and related libraries
-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.xmlbeans.**
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.commons.**
-keep class org.apache.commons.** { *; }

# Saxon (used by Apache XMLBeans)
-dontwarn net.sf.saxon.**

# OSGi framework (used by log4j)
-dontwarn org.osgi.**

# AWT/Swing (not available on Android)
-dontwarn java.awt.**
-dontwarn javax.swing.**

# javax.xml.stream (not fully available on Android)
-dontwarn javax.xml.stream.**

# aQute BND annotations
-dontwarn aQute.bnd.annotation.**

# Apache Logging
-dontwarn org.apache.logging.log4j.**
-keep class org.apache.logging.log4j.** { *; }

# GraphBuilder
-dontwarn com.graphbuilder.**

# OpenCSV
-dontwarn com.opencsv.**
-keep class com.opencsv.** { *; }

# Google Sign-In
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# Gmail API
-keep class com.google.api.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.gmail.** { *; }
-keep class com.google.api.services.gmail.model.** { *; }
-dontwarn com.google.api.**

# Google HTTP Client
-keep class com.google.api.client.http.** { *; }
-keep class com.google.api.client.json.** { *; }
-keep class com.google.api.client.util.** { *; }
-keep class com.google.api.client.googleapis.** { *; }

# Keep Gmail model classes (prevent R8 from stripping)
-keepclassmembers class com.google.api.services.gmail.model.** {
    *;
}

# Prevent R8 from removing reflection-based classes
-keepattributes InnerClasses
-keep class **.R
-keep class **.R$* {
    <fields>;
}

# Gson (used by Google APIs)
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Apache HTTP Client (used by Google APIs)
-dontwarn org.apache.http.**
-dontwarn android.net.http.**
-keep class org.apache.http.** { *; }

# JNDI/LDAP classes (not available on Android)
-dontwarn javax.naming.**

# JGSS/Kerberos classes (not available on Android)
-dontwarn org.ietf.jgss.**
