package dev.vadzimv.example.jetpackpaging

import androidx.annotation.VisibleForTesting
import java.util.concurrent.Executor
import java.util.concurrent.Executors

var pagesDiffCallbackExecutor: Executor = Executors.newFixedThreadPool(2)
    @VisibleForTesting set