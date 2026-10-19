package com.codzee.taskmanager.service;

import com.codzee.taskmanager.api.dto.TaskRequest;
import com.codzee.taskmanager.domain.Task;
import com.codzee.taskmanager.domain.TaskPriority;
import com.codzee.taskmanager.domain.TaskStatus;
import com.codzee.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTrimsTitleAndReturnsSavedTask() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = taskService.create(new TaskRequest("  Review PR  ", "Check changes", TaskStatus.TODO, TaskPriority.HIGH));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertEquals("Review PR", captor.getValue().getTitle());
        assertEquals("Review PR", response.title());
        assertEquals(TaskStatus.TODO, response.status());
    }

    @Test
    void getThrowsDomainExceptionWhenTaskDoesNotExist() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.get(99L));
    }
}
