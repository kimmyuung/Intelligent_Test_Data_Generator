/**
 * 스트리밍 API 유틸리티
 * 
 * SSE(Server-Sent Events) 기반 실시간 데이터 수신 및 파일 다운로드
 */

const API_URL = process.env.REACT_APP_ORCHESTRATOR_URL || 'http://localhost:8081';

/**
 * SSE 기반 실시간 데이터 생성 스트리밍
 * 
 * @param {Object} request - 생성 요청 {tableName, schema, rowCount, seed}
 * @param {Function} onProgress - 진행률 콜백 (current, total, percent)
 * @param {Function} onData - 데이터 청크 콜백 (rows[])
 * @param {Function} onComplete - 완료 콜백
 * @param {Function} onError - 에러 콜백
 * @returns {Function} cleanup 함수
 */
export const streamGenerate = async (request, onProgress, onData, onComplete, onError) => {
    try {
        const response = await fetch(`${API_URL}/api/orchestrator/stream/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream',
            },
            body: JSON.stringify(request),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        const processStream = async () => {
            while (true) {
                const { done, value } = await reader.read();

                if (done) {
                    onComplete && onComplete();
                    break;
                }

                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop() || '';

                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        try {
                            const data = JSON.parse(line.slice(5).trim());

                            if (data.rows && data.rows.length > 0) {
                                onData && onData(data.rows);
                            }

                            onProgress && onProgress(data.progress, data.total, data.percentComplete);
                        } catch (e) {
                            // JSON 파싱 실패 시 무시
                        }
                    } else if (line.startsWith('event:complete')) {
                        onComplete && onComplete();
                        return;
                    }
                }
            }
        };

        processStream().catch(onError);

        // Cleanup 함수 반환
        return () => {
            reader.cancel();
        };
    } catch (error) {
        onError && onError(error);
        return () => { };
    }
};

/**
 * EventSource 기반 SSE 스트리밍 (GET 요청용)
 */
export const streamGenerateWithEventSource = (tableName, schema, rowCount, callbacks) => {
    const { onProgress, onData, onComplete, onError } = callbacks;

    // POST 요청은 EventSource로 직접 불가능하므로 fetch 사용
    // 이 함수는 GET 엔드포인트가 있을 때 사용
    const params = new URLSearchParams({
        tableName,
        rowCount: rowCount.toString(),
    });

    const eventSource = new EventSource(
        `${API_URL}/api/orchestrator/stream/generate?${params}`
    );

    eventSource.addEventListener('data', (event) => {
        try {
            const chunk = JSON.parse(event.data);
            onData && onData(chunk.rows);
            onProgress && onProgress(chunk.progress, chunk.total, chunk.percentComplete);
        } catch (e) {
            console.error('Failed to parse SSE data:', e);
        }
    });

    eventSource.addEventListener('complete', () => {
        eventSource.close();
        onComplete && onComplete();
    });

    eventSource.onerror = (error) => {
        console.error('SSE Error:', error);
        eventSource.close();
        onError && onError(error);
    };

    // Cleanup 함수 반환
    return () => eventSource.close();
};

/**
 * CSV 스트리밍 다운로드
 */
export const downloadCsv = async (request) => {
    try {
        const response = await fetch(`${API_URL}/api/orchestrator/stream/download/csv`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(request),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        // Blob으로 변환하여 다운로드
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${request.tableName}.csv`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    } catch (error) {
        console.error('CSV download failed:', error);
        throw error;
    }
};

/**
 * JSON 스트리밍 다운로드
 */
export const downloadJson = async (request) => {
    try {
        const response = await fetch(`${API_URL}/api/orchestrator/stream/download/json`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(request),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${request.tableName}.json`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    } catch (error) {
        console.error('JSON download failed:', error);
        throw error;
    }
};

/**
 * XLSX 스트리밍 다운로드
 */
export const downloadXlsx = async (request) => {
    try {
        const response = await fetch(`${API_URL}/api/orchestrator/stream/download/xlsx`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(request),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${request.tableName}.xlsx`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    } catch (error) {
        console.error('XLSX download failed:', error);
        throw error;
    }
};

/**
 * 포맷별 통합 다운로드 함수
 * @param {Object} request - 요청 정보
 * @param {string} format - 'csv' | 'xlsx' | 'json'
 */
export const downloadData = async (request, format = 'csv') => {
    switch (format.toLowerCase()) {
        case 'xlsx':
            return downloadXlsx(request);
        case 'json':
            return downloadJson(request);
        case 'csv':
        default:
            return downloadCsv(request);
    }
};

/**
 * Batch Job 시작 (대용량 DB Insert용)
 */
export const startBatchJob = async (request) => {
    const response = await fetch(`${API_URL}/api/generator/batch/start`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
    });

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
};

/**
 * Batch Job 상태 조회
 */
export const getBatchJobStatus = async (executionId) => {
    const response = await fetch(`${API_URL}/api/generator/batch/status/${executionId}`);

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
};

/**
 * Batch Job 상태 SSE 스트리밍
 */
export const streamBatchJobStatus = (executionId, onStatus, onComplete, onError) => {
    const eventSource = new EventSource(
        `${API_URL}/api/generator/batch/status/${executionId}/stream`
    );

    eventSource.onmessage = (event) => {
        try {
            const status = JSON.parse(event.data);
            onStatus && onStatus(status);

            if (status.complete) {
                eventSource.close();
                onComplete && onComplete(status);
            }
        } catch (e) {
            console.error('Failed to parse batch status:', e);
        }
    };

    eventSource.onerror = (error) => {
        console.error('Batch status SSE error:', error);
        eventSource.close();
        onError && onError(error);
    };

    return () => eventSource.close();
};

export default {
    streamGenerate,
    streamGenerateWithEventSource,
    downloadCsv,
    downloadJson,
    startBatchJob,
    getBatchJobStatus,
    streamBatchJobStatus,
};
