package com.imsproject.watch.view.contracts

import android.content.Context
import android.content.Intent
import com.imsproject.watch.view.FlourMillActivity
import com.imsproject.watch.view.PacmanActivity

class PacmanResultContract : GenericActivityResultContract() {
    override fun createIntent(context: Context): Intent {
        return Intent(context, PacmanActivity::class.java)
    }
}

