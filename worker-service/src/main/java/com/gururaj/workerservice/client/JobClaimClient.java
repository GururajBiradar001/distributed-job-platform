package com.gururaj.workerservice.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class JobClaimClient {

    private final RestClient restClient;

    private final String workerId =
            "worker-" + UUID.randomUUID();

    public JobClaimClient(RestClient.Builder builder) {

        this.restClient = builder
                .baseUrl("http://job-service:8081")
                .build();
    }

    public boolean claimJob(UUID jobId) {

        ResponseEntity<Void> response =
                restClient
                        .post()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .path("/jobs/{jobId}/claim")
                                        .queryParam("workerId", workerId)
                                        .build(jobId)
                        )
                        .exchange((request, clientResponse) ->
                                ResponseEntity
                                        .status(clientResponse.getStatusCode())
                                        .build()
                        );

        return response.getStatusCode() == HttpStatus.OK;
    }

    public boolean renewLease(UUID jobId) {

        ResponseEntity<Void> response =
                restClient
                        .put()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .path("/jobs/{jobId}/heartbeat")
                                        .queryParam("workerId", workerId)
                                        .build(jobId)
                        )
                        .exchange((request, clientResponse) ->
                                ResponseEntity
                                        .status(clientResponse.getStatusCode())
                                        .build()
                        );

        return response.getStatusCode() == HttpStatus.OK;
    }

    public boolean markJobFailed(UUID jobId) {

        ResponseEntity<Void> response =
                restClient
                        .post()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .path("/jobs/{jobId}/failed")
                                        .queryParam("workerId", workerId)
                                        .build(jobId)
                        )
                        .exchange((request, clientResponse) ->
                                ResponseEntity
                                        .status(clientResponse.getStatusCode())
                                        .build()
                        );

        return response.getStatusCode() == HttpStatus.OK;
    }
}