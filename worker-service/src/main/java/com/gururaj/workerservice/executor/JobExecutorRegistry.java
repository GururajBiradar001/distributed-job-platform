package com.gururaj.workerservice.executor;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JobExecutorRegistry {

    private final Map<String, JobExecutor> executors;

    public JobExecutorRegistry(List<JobExecutor> executorList) {

        this.executors = executorList.stream()
                .collect(Collectors.toMap(
                        JobExecutor::getJobType,
                        Function.identity()
                ));
    }

    public JobExecutor getExecutor(String jobType) {

        return executors.get(jobType);
    }
}