package com.futurenotfound.appserve

abstract sealed class RunnerMessage

case object StopRunner extends RunnerMessage

case object StopAllRoutes extends RunnerMessage

case object StartAllRoutes extends RunnerMessage

