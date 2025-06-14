package com.example.clickstream.models;

import lombok.Data;

@Data
public class Item {
    // Core identifiers
    private String itemId;
    private String appId;

    // Content
    private String name;
    private String brand;
    private String category;

    // Commercial
    private Double price;
    private String currency;
    private Integer quantity;

    // Foreign key
    private String eventId;
}
