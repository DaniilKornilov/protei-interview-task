package com.protei.task.scheduler;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobInfo {
    private String userId;
    private long initialOffsetMs;
}
