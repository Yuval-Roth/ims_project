package com.imsproject.watch.view.contracts

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.imsproject.watch.PACKAGE_PREFIX
import com.imsproject.watch.view.WaterRipplesActivity

abstract class GenericActivityResultContract : ActivityResultContract<Map<String,Any>, Result>() {

    protected abstract fun createIntent(context: Context): Intent

    override fun createIntent(
        context: Context,
        input: Map<String,Any>
    ): Intent {
        val intent = createIntent(context)
        val serverStartTime = input["timeServerStartTime"] as Long
        val additionalData = input["additionalData"] as String
        intent.putExtra("$PACKAGE_PREFIX.timeServerStartTime", serverStartTime)
        intent.putExtra("$PACKAGE_PREFIX.additionalData", additionalData)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result {
        return Result(Result.Code.entries[resultCode],intent?.getStringExtra("$PACKAGE_PREFIX.error"))
    }
}

