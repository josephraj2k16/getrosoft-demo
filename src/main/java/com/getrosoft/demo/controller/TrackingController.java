package com.getrosoft.demo.controller;

import com.getrosoft.demo.service.TrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TrackingController {

    private final TrackingService trackingService;

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Autowired
    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/test-connection")
    public String testConnection() {
        try {
            ListTablesRequest request = ListTablesRequest.builder().build();
            ListTablesResponse response = dynamoDbClient.listTables(request);
            return "Connected to DynamoDB. Tables: " + response.tableNames();
        } catch (Exception e) {
            return "Failed to connect to DynamoDB: " + e.getMessage();
        }
    }

    @GetMapping("/next-tracking-number")
    public ResponseEntity<Map<String, Object>> getTrackingNumber(
            @RequestParam String origin_country_id,
            @RequestParam String destination_country_id,
            @RequestParam BigDecimal weight,
            @RequestParam String created_at,
            @RequestParam UUID customer_id,
            @RequestParam String customer_slug) {

        String trackingNumber = trackingService.generateTrackingNumber(
                origin_country_id, destination_country_id, weight, created_at, customer_id, customer_slug);

        Map<String, Object> response = new HashMap<>();
        response.put("tracking_number", trackingNumber);
        response.put("created_at", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return ResponseEntity.ok(response);
    }

}
