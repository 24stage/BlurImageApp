package com.example.bluromatic.workers

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
//定义一个日志标签，方便后面用 Log.e 打印日志时标识来源。
private const val TAG = "BlurWorker"

class BlurWorker(ctx: Context, params: WorkerParameters): CoroutineWorker(ctx,params){
    // 用 @RequiresPermission 注解，表示调用此方法需要有通知权限（Android 13+）
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    // 重写 doWork 方法，这是 WorkManager 的核心方法，用于执行后台任务。
    override suspend fun doWork(): Result {
        // 从 inputData 里读取图片的 Uri 和模糊等级，支持动态处理不同图片和不同模糊程度。
        val resourceUri = inputData.getString(KEY_IMAGE_URI)
        val blurLevel = inputData.getInt(KEY_BLUR_LEVEL, 1)
        // 显示一条“正在处理图片”的通知，告诉用户后台任务已经开始。
        makeStatusNotification(
            // applicationContext 是 CoroutineWorker 提供的全局 Context。
            applicationContext.resources.getString(R.string.blurring_image),
            applicationContext
        )
        // 切换到 IO 线程执行耗时操作
        return withContext(Dispatchers.IO){
            //return try {
            return@withContext try{
                //人为延迟一段时间，模拟任务耗时
                delay(DELAY_TIME_MILLIS)
                require(! resourceUri.isNullOrBlank()){
                    val errorMessage = applicationContext.resources.getString(R.string.invalid_input_uri)
                    Log.e(TAG, errorMessage)
                    errorMessage
                }
                // 现在通过 ContentResolver 和传入的 Uri 加载图片
                val resolver = applicationContext.contentResolver
                val picture = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(resourceUri))
                )
                // 调用 blurBitmap 方法，对图片进行模糊处理
                val output = blurBitmap(picture, blurLevel)
                // 调用 writeBitmapToFile 方法，将模糊后的图片保存到文件中。
                val outputUri = writeBitmapToFile(applicationContext, output)
                // 创建一个包含输出 Uri 的 Data 对象，用于传递给下一个任务。
                val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())
                // 把处理后的图片 Uri 作为输出数据返回
                Result.success(outputData)
            } catch (throwable: Throwable) {
                Log.e(
                    TAG,
                    applicationContext.resources.getString(R.string.error_applying_blur),
                    throwable
                )
                Result.failure()
            }
        }
    }
}