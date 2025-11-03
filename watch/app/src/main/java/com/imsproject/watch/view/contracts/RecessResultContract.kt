package com.imsproject.watch.view.contracts

import android.content.Context
import android.content.Intent
import com.imsproject.watch.view.RecessActivity
import com.imsproject.watch.view.WaterRipplesActivity

class RecessResultContract : GenericActivityResultContract() {
    override fun createIntent(context: Context): Intent {
        return Intent(context, RecessActivity::class.java)
    }
}

