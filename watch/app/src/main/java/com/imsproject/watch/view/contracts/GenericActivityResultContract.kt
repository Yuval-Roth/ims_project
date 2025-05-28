package com.imsproject.watch.view.contracts

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.imsproject.watch.PACKAGE_PREFIX

abstract class GenericActivityResultContract : ActivityResultContract<Map<String,Any>, Result>() {

    protected abstract fun createIntent(context: Context): Intent

    override fun createIntent(
        context: Context,
        input: Map<String,Any>
    ): Intent {
        val intent = createIntent(context)
        val serverStartTime = input["timeServerStartTime"] as Long
        val additionalData = input["additionalData"] as String
        val syncTolerance = input["syncTolerance"] as Long
        val syncWindowLength = input["syncWindowLength"] as Long
        intent.putExtra("$PACKAGE_PREFIX.timeServerStartTime", serverStartTime)
        intent.putExtra("$PACKAGE_PREFIX.additionalData", additionalData)
        intent.putExtra("$PACKAGE_PREFIX.syncTolerance", syncTolerance)
        intent.putExtra("$PACKAGE_PREFIX.syncWindowLength", syncWindowLength)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result {
        return Result(
            Result.Code.entries[resultCode],
            intent?.getStringExtra("$PACKAGE_PREFIX.error"),
            intent?.getStringExtra("$PACKAGE_PREFIX.expId")
        )
    }
}

