package com.imsproject.watch.view.contracts

import android.content.Context
import android.content.Intent
import com.imsproject.watch.view.FlourMillActivity
import com.imsproject.watch.view.PacmanActivity
import com.imsproject.watch.view.TreeActivity

class TreeResultContract : GenericActivityResultContract() {
    override fun createIntent(context: Context): Intent {
        return Intent(context, TreeActivity::class.java)
    }
}

