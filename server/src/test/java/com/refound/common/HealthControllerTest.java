package com.refound.common;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class HealthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JdbcTemplate jdbc;

    @Test
    void healthWithoutDbDoesNotTouchDatabase() throws Exception {
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"up\"}", true));
    }

    @Test
    void dbUp() throws Exception {
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        mvc.perform(get("/api/v1/health?db=true"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"up\",\"db\":\"up\"}", true));
    }

    @Test
    void dbDownReturns503() throws Exception {
        when(jdbc.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new CannotGetJdbcConnectionException("down"));
        mvc.perform(get("/api/v1/health?db=true"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"up\",\"db\":\"down\"}", true));
    }

    @Test
    void otherEndpointsNeedAuth() throws Exception {
        mvc.perform(get("/api/v1/items")).andExpect(status().is4xxClientError());
    }
}
