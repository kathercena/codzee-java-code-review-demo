package com.codzee.taskmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void taskLifecycleAndStatusFilterWork() throws Exception {
        String request = """
                {"title":"Write tests","description":"API flow","status":"TODO","priority":"HIGH"}
                """;

        String created = mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write tests"));

        mockMvc.perform(get("/api/tasks").param("status", "TODO").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));

        String update = """
                {"title":"Write integration tests","description":"Complete API flow","status":"DONE","priority":"MEDIUM"}
                """;
        mockMvc.perform(put("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write integration tests"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(result -> {
                    var body = objectMapper.readTree(result.getResponse().getContentAsString());
                    var createdAt = java.time.Instant.parse(body.get("createdAt").asText());
                    var updatedAt = java.time.Instant.parse(body.get("updatedAt").asText());
                    org.junit.jupiter.api.Assertions.assertFalse(updatedAt.isBefore(createdAt));
                });

        mockMvc.perform(delete("/api/tasks/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tasks/{id}", id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void createTaskReturnsLocationForCreatedResource() throws Exception {
        String request = """
                {"title":"Create location test","description":"Check resource URI","status":"TODO","priority":"HIGH"}
                """;

        var result = mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();
        String created = result.getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();
        var location = java.net.URI.create(result.getResponse().getHeader("Location"));
        org.junit.jupiter.api.Assertions.assertEquals("/api/tasks/" + id, location.getPath());
    }

    @Test
    void invalidBodyAndPaginationReturnStructuredBadRequest() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"status\":\"TODO\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details.title").exists());

        mockMvc.perform(get("/api/tasks").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists());

        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"ok\",\"status\":\"INVALID\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
