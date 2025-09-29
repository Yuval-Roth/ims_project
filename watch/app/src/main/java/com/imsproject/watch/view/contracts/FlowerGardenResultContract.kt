package com.imsproject.watch.view.contracts

import android.content.Context
import android.content.Intent
import com.imsproject.watch.view.FlowerGardenActivity

class FlowerGardenResultContract : GenericActivityResultContract() {
    override fun createIntent(context: Context): Intent {
        return Intent(context, FlowerGardenActivity::class.java)
    }
}

