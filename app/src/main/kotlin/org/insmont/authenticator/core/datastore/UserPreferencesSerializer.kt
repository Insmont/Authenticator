package org.insmont.authenticator.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object UserPreferencesSerializer : Serializer<UserPreferencesProto> {
    override val defaultValue: UserPreferencesProto = UserPreferencesProto.getDefaultInstance()
        .toBuilder()
        .setThemeConfig(ThemeConfigProto.THEME_CONFIG_FOLLOW_SYSTEM)
        .setDynamicColorEnabled(true)
        .setHapticFeedbackEnabled(true)
        .setAppLockEnabled(false)
        .build()

    override suspend fun readFrom(input: InputStream): UserPreferencesProto = try {
        return UserPreferencesProto.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
        throw CorruptionException("无法读取 proto。", exception)
    }

    override suspend fun writeTo(t: UserPreferencesProto, output: OutputStream) = t.writeTo(output)
}