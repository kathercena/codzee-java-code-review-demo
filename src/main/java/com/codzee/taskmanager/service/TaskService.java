package com.codzee.taskmanager.service;

import com.codzee.taskmanager.api.dto.PageResponse;
import com.codzee.taskmanager.api.dto.TaskRequest;
import com.codzee.taskmanager.api.dto.TaskResponse;
import com.codzee.taskmanager.domain.Task;
import com.codzee.taskmanager.domain.TaskStatus;
import com.codzee.taskmanager.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskResponse create(TaskRequest request) {
        Task task = new Task(request.title().trim(), request.description(), request.status(), request.priority());
        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return toResponse(findTask(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(int page, int size, TaskStatus status) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
        Page<Task> tasks = status == null
                ? taskRepository.findAll(pageable)
                : taskRepository.findByStatus(status, pageable);
        return new PageResponse<>(tasks.map(this::toResponse).getContent(), page, size,
                tasks.getTotalElements(), tasks.getTotalPages());
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = findTask(id);
        task.update(request.title().trim(), request.description(), request.status(), request.priority());
        return toResponse(taskRepository.saveAndFlush(task));
    }

    public void delete(Long id) {
        taskRepository.delete(findTask(id));
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                task.getPriority(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
