# Phase 0 baseline. Debug builds don't run R8, so this is only exercised when
# release shrinking is enabled in Phase 6. Widen reactively if release crashes
# surface stripped reflection targets.

-keep class org.bson.** { *; }
-keep class com.mongodb.** { *; }
-dontwarn com.mongodb.internal.**
-dontwarn org.bson.**
-dontwarn javax.naming.**
-dontwarn org.slf4j.**
