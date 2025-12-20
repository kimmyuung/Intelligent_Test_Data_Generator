package com.itdg.generator.integration;

import com.itdg.generator.GeneratorApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Generator API 통합 테스트 (MockMvc 사용)
 */
@SpringBootTest(classes = GeneratorApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Generator API 통합 테스트")
class GeneratorApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    @DisplayName("헬스체크 API 성공")
    void healthCheck_ShouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Order(2)
    @DisplayName("데이터 생성 - 단일 테이블")
    void generateData_SingleTable_ShouldSucceed() throws Exception {
        String requestBody = """
                {
                    "schema": {
                        "databaseName": "test_db",
                        "tables": [
                            {
                                "tableName": "users",
                                "columns": [
                                    {"name": "id", "dataType": "INTEGER", "isPrimaryKey": true},
                                    {"name": "name", "dataType": "VARCHAR"},
                                    {"name": "email", "dataType": "VARCHAR"}
                                ]
                            }
                        ]
                    },
                    "rowCount": 10,
                    "seed": 12345
                }
                """;

        mockMvc.perform(post("/api/generator/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.generatedData").exists());
    }

    @Test
    @Order(3)
    @DisplayName("데이터 생성 - 빈 스키마")
    void generateData_EmptySchema_ShouldReturnError() throws Exception {
        String requestBody = """
                {
                    "schema": null,
                    "rowCount": 10
                }
                """;

        mockMvc.perform(post("/api/generator/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(4)
    @DisplayName("다양한 데이터 타입 생성")
    void generateData_VariousDataTypes_ShouldSucceed() throws Exception {
        String requestBody = """
                {
                    "schema": {
                        "databaseName": "test_db",
                        "tables": [
                            {
                                "tableName": "mixed_types",
                                "columns": [
                                    {"name": "id", "dataType": "INTEGER", "isPrimaryKey": true},
                                    {"name": "name", "dataType": "VARCHAR"},
                                    {"name": "age", "dataType": "INTEGER"},
                                    {"name": "salary", "dataType": "DECIMAL"},
                                    {"name": "is_active", "dataType": "BOOLEAN"},
                                    {"name": "created_at", "dataType": "TIMESTAMP"}
                                ]
                            }
                        ]
                    },
                    "rowCount": 5
                }
                """;

        mockMvc.perform(post("/api/generator/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(5)
    @DisplayName("대량 데이터 생성 (100행)")
    void generateData_LargeRowCount_ShouldSucceed() throws Exception {
        String requestBody = """
                {
                    "schema": {
                        "databaseName": "test_db",
                        "tables": [
                            {
                                "tableName": "bulk_data",
                                "columns": [
                                    {"name": "id", "dataType": "INTEGER", "isPrimaryKey": true},
                                    {"name": "value", "dataType": "VARCHAR"}
                                ]
                            }
                        ]
                    },
                    "rowCount": 100
                }
                """;

        mockMvc.perform(post("/api/generator/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
