package com.imsproject.watch.view.contracts

import android.content.Context
import android.content.Intent
import com.imsproject.watch.view.WaterRipplesActivity

class WaterRipplesResultContract : GenericActivityResultContract() {
    override fun createIntent(context: Context): Intent {
        return Intent(context, WaterRipplesActivity::class.java)
    }
}

