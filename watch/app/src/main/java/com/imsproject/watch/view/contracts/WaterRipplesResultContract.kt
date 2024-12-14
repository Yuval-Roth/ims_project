package com.imsproject.watch.view.contracts

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.view.WaterRipplesActivity

class WaterRipplesResultContract : ActivityResultContract<Unit, Result>() {

    override fun createIntent(
        context: Context,
        input: Unit
    ): Intent {
        return Intent(context, WaterRipplesActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result {
        return Result(Result.Code.entries[resultCode],intent?.getStringExtra("$PACKAGE_PREFIX.error"))
    }
}

