package io.github.septicake.jobs

import org.quartz.Job
import org.quartz.JobExecutionContext

class PollCheck : Job {
    override fun execute(p0: JobExecutionContext?) {
        println("damn that sucks")
    }
}