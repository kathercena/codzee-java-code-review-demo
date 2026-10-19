package com.codzee.taskmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskPaginationIntegrationTest {

    private static final int PAGE_SIZE = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> createdTaskIds = new ArrayList<>();

    @AfterEach
    void cleanUpCreatedTasks() {
        List<Throwable> cleanupFailures = new ArrayList<>();
        for (Long id : createdTaskIds) {
            try {
                int statusCode = mockMvc.perform(delete("/api/tasks/{id}", id))
                        .andReturn().getResponse().getStatus();
                if (statusCode != HttpStatus.NO_CONTENT.value()) {
                    cleanupFailures.add(new AssertionError(
                            "Expected HTTP 204 when deleting test-created task " + id + " but got " + statusCode));
                }
            } catch (Exception exception) {
                cleanupFailures.add(exception);
            }
        }
        createdTaskIds.clear();

        if (!cleanupFailures.isEmpty()) {
            AssertionError cleanupError = new AssertionError(
                    "Failed to clean up " + cleanupFailures.size() + " test-created task(s)");
            cleanupFailures.forEach(cleanupError::addSuppressed);
            throw cleanupError;
        }
    }

    @Test
    void taskListPaginationMetadataBoundariesAndStatusFiltersAreConsistent() throws Exception {
        long initialAll = fetchPage(0, PAGE_SIZE, null).totalElements();
        long initialTodo = fetchPage(0, PAGE_SIZE, "TODO").totalElements();
        long initialDone = fetchPage(0, PAGE_SIZE, "DONE").totalElements();
        List<Long> createdTodoIds = createTasks("TODO", 5);
        List<Long> createdDoneIds = createTasks("DONE", 3);
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

    private List<Long> createTasks(String taskStatus, int count) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String requestBody = """
                    {"title":"pagination-%s-%d","description":"Pagination fixture","status":"%s","priority":"LOW"}
                    """.formatted(taskStatus, index, taskStatus);
            var result = mockMvc.perform(post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));
            MvcResult mvcResult = result.andExpect(status().isCreated()).andReturn();
            JsonNode responseBody = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
            assertTrue(responseBody.has("id"), "Create response must contain an id");
            JsonNode idNode = responseBody.get("id");
            assertNotNull(idNode, "Create response id must not be null");
            assertTrue(idNode.isIntegralNumber() && idNode.canConvertToLong(),
                    "Create response id must be an integer that fits in a long");
            long id = idNode.longValue();
            createdTaskIds.add(id);
            ids.add(id);
            assertTrue(id > 0, "Create response id must be positive");
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
        assertTrue(root.has("content"), "Pagination response must contain a content field");
        JsonNode contentNode = root.get("content");
        assertNotNull(contentNode, "Pagination response content must not be null");
        assertTrue(contentNode.isArray(), "Pagination response content must be an array");

        List<TaskItem> content = new ArrayList<>();
        for (JsonNode item : contentNode) {
            JsonNode idNode = item.get("id");
            assertNotNull(idNode, "Each task in content must contain an id");
            assertTrue(idNode.isIntegralNumber() && idNode.canConvertToLong(),
                    "Each task id must be an integer that fits in a long");
            JsonNode statusNode = item.get("status");
            assertNotNull(statusNode, "Each task in content must contain a status");
            assertTrue(statusNode.isTextual(), "Each task status must be a string");
            content.add(new TaskItem(idNode.longValue(), statusNode.textValue()));
        }

        return new ApiPage(requiredInt(root, "page"), requiredInt(root, "size"),
                requiredLong(root, "totalElements"), requiredInt(root, "totalPages"), content);
    }

    private int requiredInt(JsonNode root, String field) {
        JsonNode value = root.get(field);
        assertNotNull(value, "Pagination response must contain " + field);
        assertTrue(value.isIntegralNumber() && value.canConvertToInt(),
                "Pagination response " + field + " must be an integer");
        return value.intValue();
    }

    private long requiredLong(JsonNode root, String field) {
        JsonNode value = root.get(field);
        assertNotNull(value, "Pagination response must contain " + field);
        assertTrue(value.isIntegralNumber() && value.canConvertToLong(),
                "Pagination response " + field + " must be an integer");
        return value.longValue();
    }

    private int totalPages(long totalElements, int size) {
        return (int) ((totalElements + size - 1) / size);
    }

    private record TaskItem(long id, String status) {
    }

    private record ApiPage(int page, int size, long totalElements, int totalPages, List<TaskItem> content) {
    }
}
