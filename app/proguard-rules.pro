# Coolventory ProGuard / R8 rules
# Keep kotlinx.serialization generated serializers and metadata so JSON persistence
# survives R8 shrinking and obfuscation.

# --- Kotlin / metadata ---
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, EnclosingMethod
-dontnote kotlinx.serialization.**

# --- kotlinx.serialization ---
# Keep the Companion serializer() methods and generated $serializer classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    <fields>;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all Coolventory model classes (data classes + enums) fully.
-keep class com.coolventory.app.model.** { *; }
-keepclassmembers class com.coolventory.app.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep enum values() / valueOf() used by serialization.
-keepclassmembers enum com.coolventory.app.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- DataStore / Coroutines (generally R8-safe, keep signatures) ---
-dontwarn kotlinx.coroutines.**

# --- Compose ---
# AGP ships Compose-aware rules; nothing extra required here.
