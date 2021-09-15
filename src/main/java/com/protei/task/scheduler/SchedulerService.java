package com.protei.task.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Service
public class SchedulerService {
    private final Scheduler scheduler;

    @Autowired
    public SchedulerService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public <T extends Job> void schedule(Class<T> jobClass, JobInfo info) {
        JobDetail jobDetail = JobUtil.buildJobDetail(jobClass, info);
        Trigger trigger = JobUtil.buildTrigger(info);
        try {
            log.info("Start job with ID '{}'", info.getUserId());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }

    public JobInfo getRunningJob(String userId) {
        try {
            JobDetail jobDetail = scheduler.getJobDetail(new JobKey(userId));
            if (jobDetail == null) {
                log.info("Failed to find job with ID '{}'", userId);
                return null;
            }
            log.info("Found job with ID '{}'", userId);
            return (JobInfo) jobDetail.getJobDataMap().get(userId);
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public void deleteJob(final String userId) {
        try {
            log.info("Delete job with ID '{}'", userId);
            scheduler.deleteJob(new JobKey(userId));
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }

    @PostConstruct
    public void init() {
        try {
            log.info("Scheduler start");
            scheduler.start();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }

    @PreDestroy
    public void preDestroy() {
        try {
            log.info("Scheduler shut down");
            scheduler.shutdown();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }
}
