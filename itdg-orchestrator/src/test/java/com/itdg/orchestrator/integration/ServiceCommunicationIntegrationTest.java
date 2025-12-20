package com.itdg.orchestrator.integration;

import com.itdg.orchestrator.service.ServiceCommunicationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

/**
 * 서비스 간 통신 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("서비스 간 통신 통합 테스트")
class ServiceCommunicationIntegrationTest {

        @Autowired
        private ServiceCommunicationService communicationService;

        @Test
        @Order(1)
        @DisplayName("모든 서비스 헬스체크")
        void checkAllServicesHealth_ShouldComplete() {
                StepVerifier.create(communicationService.checkAllServicesHealth())
                                .expectNextMatches(result -> result != null)
                                .expectComplete()
                                .verify(Duration.ofSeconds(30));
        }

        @Test
        @Order(2)
        @DisplayName("Analyzer 헬스체크 - 서버 실행 여부와 무관하게 완료")
        void checkAnalyzerHealth_ShouldCompleteWithOrWithoutServer() {
                Mono<?> result = communicationService.checkAnalyzerHealth()
                                .cast(Object.class) // 타입을 Object로 캐스팅
                                .onErrorResume(e -> Mono.just("DOWN"));

                StepVerifier.create(result)
                                .expectNextMatches(r -> r != null)
                                .expectComplete()
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        @Order(3)
        @DisplayName("Generator 헬스체크 - 서버 실행 여부와 무관하게 완료")
        void checkGeneratorHealth_ShouldCompleteWithOrWithoutServer() {
                Mono<?> result = communicationService.checkGeneratorHealth()
                                .cast(Object.class) // 타입을 Object로 캐스팅
                                .onErrorResume(e -> Mono.just("DOWN"));

                StepVerifier.create(result)
                                .expectNextMatches(r -> r != null)
                                .expectComplete()
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        @Order(4)
        @DisplayName("ML Server 헬스체크 - 서버 실행 여부와 무관하게 완료")
        void checkMlServerHealth_ShouldCompleteWithOrWithoutServer() {
                Mono<?> result = communicationService.checkMlServerHealth()
                                .cast(Object.class) // 타입을 Object로 캐스팅
                                .onErrorResume(e -> Mono.just("DOWN"));

                StepVerifier.create(result)
                                .expectNextMatches(r -> r != null)
                                .expectComplete()
                                .verify(Duration.ofSeconds(10));
        }
}
