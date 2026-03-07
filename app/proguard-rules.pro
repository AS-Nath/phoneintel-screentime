# PhoneIntel ProGuard Rules

# Keep Hilt entry points
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Room entities
-keep class com.phoneintel.app.data.db.entities.** { *; }
-keep class com.phoneintel.app.data.db.dao.** { *; }

# Keep domain models
-keep class com.phoneintel.app.domain.model.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-keep class androidx.compose.** { *; }

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }
