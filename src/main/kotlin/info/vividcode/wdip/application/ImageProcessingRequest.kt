package info.vividcode.wdip.application

import kotlinx.coroutines.experimental.CompletableDeferred

typealias ImageProcessingRequest = Pair<String, CompletableDeferred<String>>
