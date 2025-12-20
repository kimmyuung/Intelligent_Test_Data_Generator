package com.itdg.common.dto.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_withData() {
        // given
        String testData = "Hello";

        // when
        ApiResponse<String> response = ApiResponse.success(testData);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(testData);
    }

    @Test
    void success_withMessageAndData() {
        // given
        String message = "Success message";
        Integer data = 123;

        // when
        ApiResponse<Integer> response = ApiResponse.success(message, data);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    void error_withMessage() {
        // given
        String errorMessage = "Error occurred";

        // when
        ApiResponse<Void> response = ApiResponse.error(errorMessage);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo(errorMessage);
        assertThat(response.getData()).isNull();
    }
}
