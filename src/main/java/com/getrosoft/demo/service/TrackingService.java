package com.getrosoft.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TrackingService {

    private static final String TABLE_NAME = "TrackingNumbers";
    private static final String TRACKING_NUMBER_KEY = "tracking_number";

    private final DynamoDbClient dynamoDbClient;

    @Autowired
    public TrackingService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public String generateTrackingNumber(String originCountryId, String destinationCountryId,
                                         BigDecimal weight, String createdAt, UUID customerId,
                                         String customerSlug) {

        String trackingNumber;
        boolean isUnique;
        int attempt = 0;

        do {

            trackingNumber = createTrackingNumber(originCountryId, destinationCountryId, customerSlug);
            isUnique = isTrackingNumberUnique(trackingNumber);


            attempt++;
            if (attempt >= 5 && !isUnique) {
                throw new RuntimeException("Failed to generate unique tracking number after multiple attempts.");
            }

        } while (!isUnique);
        saveTrackingNumberToDynamoDB(trackingNumber);

        return trackingNumber;
    }

    private String createTrackingNumber(String originCountryId, String destinationCountryId,
                                        String customerSlug) {

        String baseTracking = String.format("%s%s%s", originCountryId, destinationCountryId, customerSlug.substring(0, 3).toUpperCase());
        String randomSuffix = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));

        String trackingNumber = (baseTracking + randomSuffix).toUpperCase();

        return trackingNumber.length() > 16 ? (baseTracking + randomSuffix).toUpperCase().substring(0, 16) : trackingNumber;
    }

    private boolean isTrackingNumberUnique(String trackingNumber) {
        // Check DynamoDB to see if tracking number already exists
        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(TRACKING_NUMBER_KEY, AttributeValue.builder().s(trackingNumber).build()))
                .build();

        return dynamoDbClient.getItem(request).item().isEmpty();
    }

    private void saveTrackingNumberToDynamoDB(String trackingNumber) {
        // Store the tracking number in DynamoDB with a creation timestamp
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(TRACKING_NUMBER_KEY, AttributeValue.builder().s(trackingNumber).build());
        item.put("created_at", AttributeValue.builder().s(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }
}

