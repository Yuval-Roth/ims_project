package com.imsproject.watch.view.contracts

import android.content.Context
import android.content.Intent
import com.imsproject.watch.view.FlourMillActivity
import com.imsproject.watch.view.WavesActivity

class WavesResultContract : GenericActivityResultContract() {
    override fun createIntent(context: Context): Intent {
        return Intent(context, WavesActivity::class.java)
    }
}

