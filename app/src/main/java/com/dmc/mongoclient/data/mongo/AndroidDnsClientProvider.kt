package com.dmc.mongoclient.data.mongo

import com.mongodb.spi.dns.DnsClient
import com.mongodb.spi.dns.DnsClientProvider

class AndroidDnsClientProvider : DnsClientProvider {
    override fun create(): DnsClient = AndroidDnsClient()
}
