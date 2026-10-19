package com.codzee.taskmanager.api.dto;

import com.codzee.taskmanager.domain.TaskPriority;
import com.codzee.taskmanager.domain.TaskStatus;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Instant createdAt,
        Instant updatedAt
) {
}
