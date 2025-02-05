package com.imsproject.watch.view.contracts

import android.content.Context
import android.content.Intent
import com.imsproject.watch.view.WineGlassesActivity

class WineGlassesResultContract : GenericActivityResultContract() {
    override fun createIntent(context: Context): Intent {
        return Intent(context, WineGlassesActivity::class.java)
    }
}

