package com.imsproject.watch.view.contracts

import android.content.Context
import android.content.Intent
import com.imsproject.watch.view.FlowerGardenActivity
import com.imsproject.watch.view.WaterRipplesActivity
import com.imsproject.watch.viewmodel.FlowerGardenViewModel

class FlowerGardenResultContract : GenericActivityResultContract() {
    override fun createIntent(context: Context): Intent {
        return Intent(context, FlowerGardenActivity::class.java)
    }
}

