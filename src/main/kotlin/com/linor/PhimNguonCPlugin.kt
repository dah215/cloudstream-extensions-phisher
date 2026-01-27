package com.linor
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class PhimNguonCPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(PhimNguonCProvider(this))
    }
}
