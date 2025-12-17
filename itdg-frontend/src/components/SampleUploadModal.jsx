import React, { useState, useMemo } from 'react';
import axios from 'axios';
import './SampleUploadModal.css';

const ML_SERVER_URL = 'http://localhost:8000';

const SampleUploadModal = ({ tableName, onClose, onAnalyzeComplete }) => {
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [step, setStep] = useState('upload'); // 'upload' | 'analyzed' | 'training' | 'preview'
    const [analysisResult, setAnalysisResult] = useState(null);
    const [trainResult, setTrainResult] = useState(null);
    const [previewData, setPreviewData] = useState(null);
    const [error, setError] = useState(null);
    const [modelType, setModelType] = useState('copula');

    // 페이징 상태
    const [currentPage, setCurrentPage] = useState(0);
    const [pageSize, setPageSize] = useState(10);

    const handleFileChange = (e) => {
        const selectedFile = e.target.files[0];
        if (!selectedFile) return;

        const validExt = /\.(csv|json|xlsx?)$/i;
        if (!validExt.test(selectedFile.name)) {
            alert("지원되지 않는 파일 형식입니다. .csv, .json, .xls, .xlsx 파일만 업로드 가능합니다.");
            e.target.value = null;
            return;
        }

        setFile(selectedFile);
        setError(null);
    };

    // Step 1: 파일 업로드 및 분석
    const handleAnalyze = async () => {
        if (!file) {
            alert("파일을 선택해주세요.");
            return;
        }

        setLoading(true);
        setError(null);

        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await axios.post(`${ML_SERVER_URL}/api/v1/analyze`, formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });

            if (response.data.success) {
                setAnalysisResult(response.data);
                setStep('analyzed');
            } else {
                setError("분석에 실패했습니다.");
            }
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.detail || "서버 통신 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    };

    // Step 2: 모델 학습 + 100개 미리보기 데이터 생성
    const handleTrainAndPreview = async () => {
        if (!analysisResult?.fileId) return;

        setLoading(true);
        setError(null);
        setStep('training');

        try {
            // 1. 모델 학습
            const trainResponse = await axios.post(`${ML_SERVER_URL}/api/v1/train`, null, {
                params: { file_id: analysisResult.fileId, model_type: modelType }
            });

            if (!trainResponse.data.success) {
                throw new Error("학습에 실패했습니다.");
            }

            setTrainResult(trainResponse.data);

            // 2. 100개 미리보기 데이터 생성
            const previewResponse = await axios.post(
                `${ML_SERVER_URL}/api/v1/generate/${trainResponse.data.modelId}`,
                null,
                { params: { num_rows: 100 } }
            );

            if (previewResponse.data.success) {
                setPreviewData(previewResponse.data);
                setCurrentPage(0);
                setStep('preview');
            } else {
                throw new Error("데이터 생성에 실패했습니다.");
            }
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.detail || err.message || "학습/생성 중 오류가 발생했습니다.");
            setStep('analyzed');
        } finally {
            setLoading(false);
        }
    };

    // 재생성 (기존 모델 삭제 → 다시 학습)
    const handleRegenerate = async () => {
        // 기존 모델 삭제
        if (trainResult?.modelId) {
            try {
                await axios.delete(`${ML_SERVER_URL}/api/v1/model/${trainResult.modelId}`);
            } catch (err) {
                console.warn("Model deletion failed:", err);
            }
        }

        // 상태 초기화 후 다시 학습
        setTrainResult(null);
        setPreviewData(null);
        setCurrentPage(0);
        await handleTrainAndPreview();
    };

    // 최종 승인
    const handleConfirm = () => {
        if (trainResult && previewData) {
            onAnalyzeComplete(tableName, {
                ...analysisResult,
                modelId: trainResult.modelId,
                modelType: trainResult.modelType,
                trained: true,
                previewData: previewData.data // 미리보기 데이터도 포함
            });
            onClose();
        }
    };

    // 페이징된 데이터
    const pagedData = useMemo(() => {
        if (!previewData?.data) return [];
        const start = currentPage * pageSize;
        return previewData.data.slice(start, start + pageSize);
    }, [previewData, currentPage, pageSize]);

    const totalPages = useMemo(() => {
        if (!previewData?.data) return 0;
        return Math.ceil(previewData.data.length / pageSize);
    }, [previewData, pageSize]);

    // 모달 너비 결정
    const getModalWidth = () => {
        if (step === 'preview') return 'extra-wide';
        if (step === 'analyzed') return 'wide';
        return '';
    };

    return (
        <div className="modal-overlay">
            <div className={`modal-content sample-upload-modal ${getModalWidth()}`}>
                <div className="modal-header">
                    <h3>📊 데이터 학습 (Data Learning) - {tableName}</h3>
                    <button className="close-btn-icon" onClick={onClose}>&times;</button>
                </div>

                {/* 진행 상태 표시 */}
                <div className="progress-steps">
                    <div className={`step ${step !== 'upload' ? 'completed' : 'active'}`}>
                        <span className="step-number">1</span>
                        <span className="step-label">파일 분석</span>
                    </div>
                    <div className={`step ${step === 'preview' ? 'completed' : step === 'training' ? 'active' : ''}`}>
                        <span className="step-number">2</span>
                        <span className="step-label">AI 학습</span>
                    </div>
                    <div className={`step ${step === 'preview' ? 'active' : ''}`}>
                        <span className="step-number">3</span>
                        <span className="step-label">미리보기</span>
                    </div>
                </div>

                {error && <div className="error-msg">{error}</div>}

                {/* Step 1: 파일 업로드 */}
                {step === 'upload' && (
                    <div className="upload-step">
                        <p className="description">
                            실제 운영 데이터나 샘플 파일을 업로드하면, <br />
                            AI가 데이터의 패턴을 학습하여 더욱 리얼한 테스트 데이터를 생성합니다.
                        </p>

                        <div className="file-input-wrapper">
                            <input
                                type="file"
                                accept=".csv,.json,.xlsx,.xls"
                                onChange={handleFileChange}
                            />
                            <p className="hint">지원 형식: CSV, JSON, Excel (.xlsx, .xls)</p>
                        </div>

                        <div className="modal-actions">
                            <button className="cancel-btn" onClick={onClose}>취소</button>
                            <button
                                className="analyze-btn"
                                onClick={handleAnalyze}
                                disabled={!file || loading}
                            >
                                {loading ? '분석 중...' : '데이터 분석 시작'}
                            </button>
                        </div>
                    </div>
                )}

                {/* Step 2: 분석 결과 및 모델 선택 */}
                {step === 'analyzed' && analysisResult && (
                    <div className="result-step">
                        <div className="result-summary">
                            <p>✅ <strong>{analysisResult.rows}</strong> 개의 데이터를 분석했습니다.</p>
                            <p className="file-info">파일: {analysisResult.filename}</p>
                        </div>

                        <div className="stats-table-wrapper compact">
                            <table className="stats-table">
                                <thead>
                                    <tr>
                                        <th>컬럼명</th>
                                        <th>타입</th>
                                        <th>통계 정보</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {Object.values(analysisResult.stats).map((stat) => (
                                        <tr key={stat.name}>
                                            <td>{stat.name}</td>
                                            <td><span className="type-badge">{stat.category}</span></td>
                                            <td>
                                                {stat.category === 'numeric' ? (
                                                    <span>{stat.min} ~ {stat.max}</span>
                                                ) : (
                                                    <span>유니크: {stat.unique_count}</span>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        <div className="model-selector-cards">
                            <label className="model-card-label">생성 방식 선택:</label>
                            <div className="model-cards">
                                <div
                                    className={`model-card ${modelType === 'copula' ? 'selected' : ''}`}
                                    onClick={() => setModelType('copula')}
                                >
                                    <div className="model-icon">⚡</div>
                                    <div className="model-info">
                                        <h4>빠른 생성</h4>
                                        <p>GaussianCopula</p>
                                        <span className="model-time">~5초</span>
                                    </div>
                                </div>
                                <div
                                    className={`model-card ${modelType === 'ctgan' ? 'selected' : ''}`}
                                    onClick={() => setModelType('ctgan')}
                                >
                                    <div className="model-icon">🎯</div>
                                    <div className="model-info">
                                        <h4>정확한 생성</h4>
                                        <p>CTGAN</p>
                                        <span className="model-time">5~10분</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="modal-actions">
                            <button className="cancel-btn" onClick={() => setStep('upload')}>다시 선택</button>
                            <button className="train-btn" onClick={handleTrainAndPreview} disabled={loading}>
                                🚀 학습 및 미리보기 생성
                            </button>
                        </div>
                    </div>
                )}

                {/* Step 2.5: 학습 중 */}
                {step === 'training' && (
                    <div className="training-step">
                        <div className="training-animation">
                            <div className="spinner"></div>
                            <p>🧠 AI가 데이터 패턴을 학습하고 있습니다...</p>
                            <p className="training-hint">
                                {modelType === 'ctgan'
                                    ? 'CTGAN 모델은 5-10분 정도 소요될 수 있습니다.'
                                    : '잠시만 기다려주세요...'}
                            </p>
                        </div>
                    </div>
                )}

                {/* Step 3: 미리보기 (100개 + 페이징) */}
                {step === 'preview' && previewData && (
                    <div className="preview-step">
                        <div className="preview-header">
                            <div className="preview-title">
                                <h4>📋 생성된 샘플 데이터 (100건 미리보기)</h4>
                                <span className="model-badge">{modelType === 'copula' ? '⚡ 빠른 생성' : '🎯 정확한 생성'}</span>
                            </div>
                            <p className="preview-notice">
                                학습 시간: {trainResult?.trainingTime}초 |
                                승인 시 이 데이터를 포함하여 최종 데이터를 생성합니다.
                            </p>
                        </div>

                        {/* 페이징 컨트롤 */}
                        <div className="pagination-controls">
                            <div className="page-size-selector">
                                <label>페이지당:</label>
                                <select value={pageSize} onChange={(e) => {
                                    setPageSize(Number(e.target.value));
                                    setCurrentPage(0);
                                }}>
                                    <option value={10}>10개</option>
                                    <option value={20}>20개</option>
                                    <option value={50}>50개</option>
                                </select>
                            </div>
                            <div className="page-navigation">
                                <button
                                    onClick={() => setCurrentPage(p => Math.max(0, p - 1))}
                                    disabled={currentPage === 0}
                                >
                                    ◀ 이전
                                </button>
                                <span className="page-info">
                                    {currentPage + 1} / {totalPages} 페이지
                                </span>
                                <button
                                    onClick={() => setCurrentPage(p => Math.min(totalPages - 1, p + 1))}
                                    disabled={currentPage >= totalPages - 1}
                                >
                                    다음 ▶
                                </button>
                            </div>
                        </div>

                        <div className="preview-table-wrapper">
                            <table className="preview-table">
                                <thead>
                                    <tr>
                                        <th>#</th>
                                        {previewData.columns.map(col => (
                                            <th key={col}>{col}</th>
                                        ))}
                                    </tr>
                                </thead>
                                <tbody>
                                    {pagedData.map((row, idx) => (
                                        <tr key={currentPage * pageSize + idx}>
                                            <td>{currentPage * pageSize + idx + 1}</td>
                                            {previewData.columns.map(col => (
                                                <td key={col}>{String(row[col] ?? '')}</td>
                                            ))}
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        <div className="modal-actions preview-actions">
                            <button
                                className="regenerate-btn"
                                onClick={handleRegenerate}
                                disabled={loading}
                            >
                                🔄 다시 생성
                            </button>
                            <button className="confirm-btn" onClick={handleConfirm}>
                                ✅ 이 데이터로 승인
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default SampleUploadModal;


