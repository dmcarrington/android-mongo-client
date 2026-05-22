package com.dmc.mongoclient.data.mongo

import com.mongodb.spi.dns.DnsClient
import com.mongodb.spi.dns.DnsException
import org.xbill.DNS.Lookup
import org.xbill.DNS.SRVRecord
import org.xbill.DNS.TXTRecord
import org.xbill.DNS.TextParseException
import org.xbill.DNS.Type

/**
 * [DnsClient] implementation backed by dnsjava. Drop-in replacement for the
 * driver's `JndiDnsClient`, whose `javax.naming.*` dependency is absent on
 * Android. Wired in via `META-INF/services/com.mongodb.spi.dns.DnsClientProvider`
 * — the driver's `DefaultDnsResolver` finds it through `ServiceLoader` before
 * falling back to JNDI.
 *
 * Output format per `DefaultDnsResolver`:
 *  - SRV → `"<priority> <weight> <port> <target>"` (target may include trailing dot)
 *  - TXT → raw concatenation of the record's character-strings, NO quotes.
 *    The driver strips whitespace (`replaceAll("\\s", "")`) but does not strip
 *    quotes, so wrapping in quotes leaves literal `"` chars in parsed keys.
 */
class AndroidDnsClient : DnsClient {

    override fun getResourceRecordData(name: String, type: String): List<String> {
        val rrType = when (type.uppercase()) {
            "SRV" -> Type.SRV
            "TXT" -> Type.TXT
            else -> throw DnsException("Unsupported DNS record type: $type", IllegalArgumentException(type))
        }

        val lookup = try {
            Lookup(name, rrType)
        } catch (e: TextParseException) {
            throw DnsException("Invalid DNS name: $name", e)
        }

        val records = lookup.run()
        return when (lookup.result) {
            Lookup.SUCCESSFUL -> records.orEmpty().map { rec ->
                when (rec) {
                    is SRVRecord -> "${rec.priority} ${rec.weight} ${rec.port} ${rec.target}"
                    is TXTRecord -> rec.strings.joinToString(separator = "")
                    else -> rec.rdataToString()
                }
            }
            Lookup.HOST_NOT_FOUND, Lookup.TYPE_NOT_FOUND -> emptyList()
            else -> throw DnsException(
                "DNS lookup for $name ($type) failed: ${lookup.errorString}",
                RuntimeException(lookup.errorString),
            )
        }
    }
}
