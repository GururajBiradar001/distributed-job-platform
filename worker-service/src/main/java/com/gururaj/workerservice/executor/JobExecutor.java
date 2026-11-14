package com.gururaj.workerservice.executor;

import com.gururaj.workerservice.dto.JobCreatedEvent;

public interface JobExecutor {

    String getJobType();

    void execute(JobCreatedEvent event);
}
