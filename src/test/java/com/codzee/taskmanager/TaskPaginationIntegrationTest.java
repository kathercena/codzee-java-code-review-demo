package com.codzee.taskmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskPaginationIntegrationTest {

    private static final int PAGE_SIZE = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void taskListPaginationMetadataBoundariesAndStatusFiltersAreConsistent() throws Exception {
        long initialAll = fetchPage(0, PAGE_SIZE, null).totalElements();
        long initialTodo = fetchPage(0, PAGE_SIZE, "TODO").totalElements();
        long initialDone = fetchPage(0, PAGE_SIZE, "DONE").totalElements();
        List<Long> createdIds = new ArrayList<>();

        try {
            List<Long> createdTodoIds = createTasks("TODO", 5, createdIds);
            List<Long> createdDoneIds = createTasks("DONE", 3, createdIds);
            List<Long> allCreatedIds = new ArrayList<>(createdTodoIds);
            allCreatedIds.addAll(createdDoneIds);

            long expectedAll = initialAll + allCreatedIds.size();
            assertAllPages(null, expectedAll, allCreatedIds);

            long expectedTodo = initialTodo + createdTodoIds.size();
            assertAllPages("TODO", expectedTodo, createdTodoIds);

            long expectedDone = initialDone + createdDoneIds.size();
            assertAllPages("DONE", expectedDone, createdDoneIds);

            int totalPages = totalPages(expectedAll, PAGE_SIZE);
            ApiPage beyondLastPage = fetchPage(totalPages, PAGE_SIZE, null);
            assertEquals(totalPages, beyondLastPage.page());
            assertEquals(PAGE_SIZE, beyondLastPage.size());
            assertTrue(beyondLastPage.content().isEmpty());
            assertEquals(expectedAll, beyondLastPage.totalElements());
            assertEquals(totalPages, beyondLastPage.totalPages());
        } finally {
            for (Long id : createdIds) {
                mockMvc.perform(delete("/api/tasks/{id}", id))
                        .andExpect(status().isNoContent());
            }
        }
    }

    private void assertAllPages(String statusFilter, long expectedTotal, List<Long> expectedCreatedIds)
            throws Exception {
        int expectedPages = totalPages(expectedTotal, PAGE_SIZE);
        Set<Long> observedIds = new HashSet<>();

        for (int pageNumber = 0; pageNumber < expectedPages; pageNumber++) {
            ApiPage page = fetchPage(pageNumber, PAGE_SIZE, statusFilter);
            long remaining = expectedTotal - (long) pageNumber * PAGE_SIZE;
            int expectedContentSize = (int) Math.min(PAGE_SIZE, remaining);

            assertEquals(pageNumber, page.page());
            assertEquals(PAGE_SIZE, page.size());
            assertEquals(expectedTotal, page.totalElements());
            assertEquals(expectedPages, page.totalPages());
            assertEquals(expectedContentSize, page.content().size());

            for (TaskItem item : page.content()) {
                assertTrue(observedIds.add(item.id()), "Task appeared on more than one page: " + item.id());
                if (statusFilter != null) {
                    assertEquals(statusFilter, item.status());
                }
            }
        }

        assertEquals(expectedTotal, observedIds.size());
        assertTrue(observedIds.containsAll(expectedCreatedIds));
    }

    private List<Long> createTasks(String taskStatus, int count, List<Long> createdIds) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String requestBody = """
                    {"title":"pagination-%s-%d","description":"Pagination fixture","status":"%s","priority":"LOW"}
                    """.formatted(taskStatus, index, taskStatus);
            var result = mockMvc.perform(post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));
            MvcResult mvcResult = result.andReturn();
            long id = objectMapper.readTree(mvcResult.getResponse().getContentAsString()).get("id").asLong();
            createdIds.add(id);
            ids.add(id);

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id));
        }
        return ids;
    }

    private ApiPage fetchPage(int pageNumber, int size, String statusFilter) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/tasks")
                .param("page", Integer.toString(pageNumber))
                .param("size", Integer.toString(size));
        if (statusFilter != null) {
            request.param("status", statusFilter);
        }

        String response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        List<TaskItem> content = new ArrayList<>();
        for (JsonNode item : root.path("content")) {
            content.add(new TaskItem(item.path("id").asLong(), item.path("status").asText()));
        }

        return new ApiPage(root.path("page").asInt(), root.path("size").asInt(),
                root.path("totalElements").asLong(), root.path("totalPages").asInt(), content);
    }

    private int totalPages(long totalElements, int size) {
        return (int) ((totalElements + size - 1) / size);
    }

    private record TaskItem(long id, String status) {
    }

    private record ApiPage(int page, int size, long totalElements, int totalPages, List<TaskItem> content) {
    }
}
