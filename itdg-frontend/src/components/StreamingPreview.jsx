import React, { useState, useCallback } from 'react';
import { streamGenerate, streamGenerateMl, downloadData } from '../api/streamingApi';
import './StreamingPreview.css';

/**
 * 스트리밍 기반 데이터 미리보기 컴포넌트
 * 
 * 특징:
 * - 실시간 진행률 표시
 * - 생성 즉시 미리보기 테이블 업데이트
 * - CSV/XLSX/JSON 다운로드 포맷 선택
 * - ML 학습 데이터 기반 생성 지원
 */
const StreamingPreview = ({ tableName, schema, defaultRowCount = 1000, mlModelId }) => {
    const [data, setData] = useState([]);
    const [progress, setProgress] = useState(0);
    const [total, setTotal] = useState(0);
    const [percentComplete, setPercentComplete] = useState(0);
    const [isLoading, setIsLoading] = useState(false);
    const [isDownloading, setIsDownloading] = useState(false);
    const [isComplete, setIsComplete] = useState(false);
    const [error, setError] = useState(null);
    const [rowCount, setRowCount] = useState(defaultRowCount);
    const [downloadFormat, setDownloadFormat] = useState('csv');
    const [cleanupFn, setCleanupFn] = useState(null);
    const [useMlGeneration, setUseMlGeneration] = useState(!!mlModelId);  // ML 모델이 있으면 기본 사용

    const handleStartGenerate = useCallback(async () => {
        setIsLoading(true);
        setIsComplete(false);
        setError(null);
        setData([]);
        setProgress(0);
        setTotal(rowCount);
        setPercentComplete(0);

        const request = {
            tableName,
            schema,
            rowCount,
            seed: Date.now(),
            mlModelId: useMlGeneration ? mlModelId : null,  // ML 모델 ID 추가
        };

        // ML 모델 사용 여부에 따라 다른 API 호출
        const streamFn = useMlGeneration && mlModelId ? streamGenerateMl : streamGenerate;

        const cleanup = await streamFn(
            request,
            // onProgress
            (current, total, percent) => {
                setProgress(current);
                setTotal(total);
                setPercentComplete(percent);
            },
            // onData
            (rows) => {
                // 미리보기는 최대 100개만 표시
                setData(prev => {
                    if (prev.length >= 100) return prev;
                    const remaining = 100 - prev.length;
                    return [...prev, ...rows.slice(0, remaining)];
                });
            },
            // onComplete
            () => {
                setIsComplete(true);
                setIsLoading(false);
            },
            // onError
            (err) => {
                setError(err.message || 'Streaming failed');
                setIsLoading(false);
            }
        );

        setCleanupFn(() => cleanup);
    }, [tableName, schema, rowCount, useMlGeneration, mlModelId]);

    const handleStop = useCallback(() => {
        if (cleanupFn) {
            cleanupFn();
            setIsLoading(false);
        }
    }, [cleanupFn]);

    const handleDownload = useCallback(async () => {
        if (total === 0) {
            alert('생성된 데이터가 없어 다운로드할 수 없습니다.');
            return;
        }

        setIsDownloading(true);
        setError(null);

        try {
            await downloadData({
                tableName,
                schema,
                rowCount,
                seed: Date.now(),
            }, downloadFormat);
        } catch (err) {
            setError(`${downloadFormat.toUpperCase()} 다운로드 실패: ${err.message}`);
        } finally {
            setIsDownloading(false);
        }
    }, [tableName, schema, rowCount, downloadFormat, total]);

    const columns = data.length > 0 ? Object.keys(data[0]) :
        schema?.columns?.map(c => c.name) || [];

    const formatOptions = [
        { value: 'csv', label: 'CSV', icon: '📄' },
        { value: 'xlsx', label: 'Excel (XLSX)', icon: '📊' },
        { value: 'json', label: 'JSON', icon: '📋' },
    ];

    return (
        <div className="streaming-preview">
            <div className="streaming-header">
                <h3>🚀 스트리밍 데이터 생성</h3>
                <span className="table-name">{tableName}</span>
            </div>

            <div className="streaming-controls">
                <div className="row-count-input">
                    <label>생성 행 수:</label>
                    <input
                        type="number"
                        value={rowCount}
                        onChange={(e) => setRowCount(Math.max(1, parseInt(e.target.value) || 1))}
                        disabled={isLoading || isDownloading}
                        min="1"
                        max="10000000"
                    />
                </div>

                <div className="button-group">
                    {!isLoading ? (
                        <button
                            className="btn-primary"
                            onClick={handleStartGenerate}
                            disabled={!schema}
                        >
                            ▶️ 생성 시작
                        </button>
                    ) : (
                        <button
                            className="btn-danger"
                            onClick={handleStop}
                        >
                            ⏹️ 중지
                        </button>
                    )}
                </div>
            </div>

            {/* 다운로드 섹션 */}
            <div className="download-section">
                <div className="format-selector">
                    <label>다운로드 포맷:</label>
                    <div className="format-buttons">
                        {formatOptions.map(opt => (
                            <button
                                key={opt.value}
                                className={`format-btn ${downloadFormat === opt.value ? 'active' : ''}`}
                                onClick={() => setDownloadFormat(opt.value)}
                                disabled={isLoading || isDownloading}
                            >
                                {opt.icon} {opt.label}
                            </button>
                        ))}
                    </div>
                </div>

                <button
                    className="btn-download"
                    onClick={handleDownload}
                    disabled={isLoading || isDownloading || !schema}
                >
                    {isDownloading ? '⏳ 다운로드 중...' : `📥 ${downloadFormat.toUpperCase()} 다운로드`}
                </button>
            </div>

            {(isLoading || isComplete) && (
                <div className="progress-section">
                    <div className="progress-bar-container">
                        <div
                            className="progress-bar-fill"
                            style={{ width: `${percentComplete}%` }}
                        />
                    </div>
                    <div className="progress-text">
                        <span>{progress.toLocaleString()} / {total.toLocaleString()}</span>
                        <span>{percentComplete}%</span>
                    </div>
                </div>
            )}

            {error && (
                <div className="error-message">
                    ❌ {error}
                </div>
            )}

            {isComplete && (
                <div className="success-message">
                    ✅ 생성 완료! {total.toLocaleString()}개 행 생성됨
                </div>
            )}

            {data.length > 0 && (
                <div className="preview-section">
                    <h4>미리보기 (상위 {data.length}개)</h4>
                    <div className="preview-table-container">
                        <table className="preview-table">
                            <thead>
                                <tr>
                                    <th>#</th>
                                    {columns.map((col, idx) => (
                                        <th key={idx}>{col}</th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                {data.map((row, rowIdx) => (
                                    <tr key={rowIdx}>
                                        <td className="row-number">{rowIdx + 1}</td>
                                        {columns.map((col, colIdx) => (
                                            <td key={colIdx}>
                                                {formatCellValue(row[col])}
                                            </td>
                                        ))}
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
};

/**
 * 셀 값 포맷팅
 */
const formatCellValue = (value) => {
    if (value === null || value === undefined) return <span className="null-value">NULL</span>;
    if (typeof value === 'boolean') return value ? '✓' : '✗';
    if (typeof value === 'object') return JSON.stringify(value);
    const str = String(value);
    return str.length > 50 ? str.substring(0, 47) + '...' : str;
};

export default StreamingPreview;
