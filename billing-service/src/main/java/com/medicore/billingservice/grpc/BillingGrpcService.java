package com.medicore.billingservice.grpc;

import com.medicore.billing.grpc.BillingRequest;
import com.medicore.billing.grpc.BillingResponse;
import com.medicore.billing.grpc.BillingServiceGrpc.BillingServiceImplBase;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

    @Override
    public void createBillingAccount(BillingRequest billingRequest,
                                     StreamObserver<BillingResponse> responseObserver) {
        log.info("createBillingAccount request received for patientId={}", billingRequest.getPatientId());

        // TODO: save the billing account to a database and calculate charges
        BillingResponse response = BillingResponse.newBuilder()
                .setAccountId(UUID.randomUUID().toString())
                .setStatus("ACTIVE")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
