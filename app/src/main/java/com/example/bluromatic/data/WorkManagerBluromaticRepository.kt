/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluromatic.data

import android.content.Context
import android.net.Uri
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.getImageUri
import com.example.bluromatic.workers.BlurWorker
import com.example.bluromatic.workers.CleanupWorker
import com.example.bluromatic.workers.SaveImageToFileWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class WorkManagerBluromaticRepository(context: Context) : BluromaticRepository {
    // 通过 context.getImageUri() 获取了一个图片的 Ur
    private var imageUri: Uri = context.getImageUri()
    private val workManager = WorkManager.getInstance(context)
    override val outputWorkInfo: Flow<WorkInfo?> = MutableStateFlow(null)

    // 实现接口 BluromaticRepository 的方法，用于发起“模糊图片”的后台任务。
    override fun applyBlur(blurLevel: Int) {
        //调用 beginWith() 方法返回 WorkContinuation 对象，并为包含第一个工作请求的 WorkRequest 链创建起点。
        var continuation = workManager.beginWith(OneTimeWorkRequest.from(CleanupWorker::class.java))
        //  创建一个只执行一次的 WorkRequest，指定任务是 BlurWorker
        val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()
        // 通过 setInputData 方法，把图片的 Uri 和模糊等级一起传递给 BlurWorker
        blurBuilder.setInputData(createInputDataForWorkRequest(blurLevel,imageUri))
//        // 把这个任务加入 WorkManager 队列，系统会自动调度执行。
//        workManager.enqueue(blurBuilder.build())
        continuation = continuation.then(blurBuilder.build())
        val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            .build()
        continuation = continuation.then(save)
        continuation.enqueue()

    }

    override fun cancelWork() {}

    /**
     * Creates the input data bundle which includes the blur level to
     * update the amount of blur to be applied and the Uri to operate on
     * @return Data which contains the Image Uri as a String and blur level as an Integer
     */
    private fun createInputDataForWorkRequest(blurLevel: Int, imageUri: Uri): Data {
        val builder = Data.Builder()
        builder.putString(KEY_IMAGE_URI, imageUri.toString()).putInt(KEY_BLUR_LEVEL, blurLevel)
        return builder.build()
    }
}