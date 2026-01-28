package com.linor
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class PhimNguonCPlugin: Plugin() {
    override fun load(context: android.content.Context) {
        registerMainAPI(PhimNguonCProvider(this))
    }
}
