package com.imsproject.watch.view.contracts

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.view.WaterRipplesActivity

class WaterRipplesResultContract : ActivityResultContract<Map<String,Any>, Result>() {

    override fun createIntent(
        context: Context,
        input: Map<String,Any>
    ): Intent {
        val intent = Intent(context, WaterRipplesActivity::class.java)
        val serverStartTime = input["timeServerStartTime"] as Long
        intent.putExtra("$PACKAGE_PREFIX.timeServerStartTime", serverStartTime)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result {
        return Result(Result.Code.entries[resultCode],intent?.getStringExtra("$PACKAGE_PREFIX.error"))
    }
}

