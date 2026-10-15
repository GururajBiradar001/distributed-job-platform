package com.gururaj.jobservice.entity;

public enum JobStatus {

    CREATED,
    SCHEDULED,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    RETRYING,
    DEAD,
    CANCELLED
}