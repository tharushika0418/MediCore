package com.medicore.patientservice.grpc;

import com.medicore.billing.grpc.BillingRequest;
import com.medicore.billing.grpc.BillingResponse;
import com.medicore.billing.grpc.BillingServiceGrpc;
import com.medicore.billing.grpc.BillingServiceGrpc.BillingServiceBlockingStub;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * gRPC client for billing-service.
 * <p>
 * The channel's address comes from {@code spring.grpc.client.channel.billing-service.target}
 * in application.properties. Spring creates, reuses and shuts down the channel.
 */
@Service
public class BillingServiceGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);

    private static final long DEADLINE_SECONDS = 5;

    private final BillingServiceBlockingStub blockingStub;

    public BillingServiceGrpcClient(
            GrpcChannelFactory channelFactory,
            @Value("${spring.grpc.client.channel.billing-service.target}") String target) {

        log.info("Connecting to Billing Service gRPC service at {}", target);

        ManagedChannel channel = channelFactory.createChannel("billing-service");
        this.blockingStub = BillingServiceGrpc.newBlockingStub(channel);
    }

    public BillingResponse createBillingAccount(String patientId, String name, String email) {
        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId)
                .setName(name)
                .setEmail(email)
                .build();

        log.info("Calling billing-service via gRPC for patientId={}", patientId);

        BillingResponse response = blockingStub
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .createBillingAccount(request);

        log.info("Received billing account: accountId={}, status={}",
                response.getAccountId(), response.getStatus());

        return response;
    }
}
