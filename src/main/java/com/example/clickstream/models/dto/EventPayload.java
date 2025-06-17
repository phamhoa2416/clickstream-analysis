package com.example.clickstream.models.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class EventPayload {
    @NotEmpty(message = "Event list cannot be empty")
    private List<Map<String, Object>> events;
}
