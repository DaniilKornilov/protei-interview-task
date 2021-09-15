package com.protei.task.scheduler;

import org.quartz.*;

import java.util.Date;

public final class JobUtil {
    public static final String USER_ID_KEY = "userId";

    private JobUtil() {
    }

    public static JobDetail buildJobDetail(Class<? extends Job> jobClass, JobInfo info) {
        JobDataMap jobDataMap = new JobDataMap();
        String userId = info.getUserId();
        jobDataMap.put(userId, info);
        jobDataMap.put(USER_ID_KEY, userId);
        return JobBuilder
                .newJob(jobClass)
                .withIdentity(userId)
                .setJobData(jobDataMap)
                .build();
    }

    public static Trigger buildTrigger(JobInfo info) {
        SimpleScheduleBuilder builder = SimpleScheduleBuilder.
                simpleSchedule().withRepeatCount(0);
        return TriggerBuilder
                .newTrigger()
                .withIdentity(info.getUserId())
                .withSchedule(builder)
                .startAt(new Date(System.currentTimeMillis() + info.getInitialOffsetMs()))
                .build();
    }
}
