package com.protei.task.scheduler;

import com.protei.task.systemuser.SystemUserService;
import com.protei.task.systemuser.enumeration.SystemUserStatus;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DisallowConcurrentExecution
public class SystemUserStatusUpdateJob implements Job {

    private final SystemUserService userService;

    @Autowired
    public SystemUserStatusUpdateJob(SystemUserService userService) {
        this.userService = userService;
    }

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap data = context.getJobDetail().getJobDataMap();
        String userId = (String) data.get(JobUtil.USER_ID_KEY);
        log.info("Updating SystemUser with id '{}'", userId);
        userService.updateSystemUserStatus(Long.parseLong(userId), SystemUserStatus.AWAY.name());
    }
}
