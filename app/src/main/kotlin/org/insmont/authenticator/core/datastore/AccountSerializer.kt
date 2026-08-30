package org.insmont.authenticator.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object AccountSerializer : Serializer<AccountListProto> {
    override val defaultValue: AccountListProto = AccountListProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AccountListProto = try {
        AccountListProto.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read proto.", exception)
    }

    override suspend fun writeTo(t: AccountListProto, output: OutputStream) = t.writeTo(output)
}