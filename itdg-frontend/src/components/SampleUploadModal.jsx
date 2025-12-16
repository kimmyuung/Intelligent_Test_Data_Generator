import React, { useState } from 'react';
import axios from 'axios';
import './SampleUploadModal.css';

const SampleUploadModal = ({ tableName, onClose, onAnalyzeComplete }) => {
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [analysisResult, setAnalysisResult] = useState(null);
    const [error, setError] = useState(null);

    const handleFileChange = (e) => {
        const selectedFile = e.target.files[0];
        if (!selectedFile) return;

        // Validation
        const validExt = /\.(csv|json|xlsx?)$/i;
        if (!validExt.test(selectedFile.name)) {
            alert("지원되지 않는 파일 형식입니다. .csv, .json, .xls, .xlsx 파일만 업로드 가능합니다.");
            e.target.value = null; // Reset input
            return;
        }

        setFile(selectedFile);
        setError(null);
    };

    const handleUpload = async () => {
        if (!file) {
            alert("파일을 선택해주세요.");
            return;
        }

        setLoading(true);
        setError(null);

        const formData = new FormData();
        formData.append('file', file);

        try {
            // Call Python ML Server
            const response = await axios.post('http://localhost:8000/api/v1/analyze', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });

            if (response.data.success) {
                setAnalysisResult(response.data);
                // Notify parent immediately or let user confirm? 
                // Let's just show result first.
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

    const handleConfirm = () => {
        if (analysisResult) {
            onAnalyzeComplete(tableName, analysisResult);
            onClose();
        }
    };

    return (
        <div className="modal-overlay">
            <div className={`modal-content sample-upload-modal ${analysisResult ? 'wide' : ''}`}>
                <div className="modal-header">
                    <h3>📊 데이터 학습 (Data Learning) - {tableName}</h3>
                    <button className="close-btn-icon" onClick={onClose}>&times;</button>
                </div>

                {!analysisResult ? (
                    <div className="upload-step">
                        <p className="description">
                            실제 운영 데이터나 샘플 파일을 업로드하면, <br />
                            AI가 데이터의 패턴(분포, 값의 범위, 포맷 등)을 학습하여 <br />
                            더욱 리얼한 테스트 데이터를 생성합니다.
                        </p>

                        <div className="file-input-wrapper">
                            <input
                                type="file"
                                accept=".csv,.json,.xlsx,.xls"
                                onChange={handleFileChange}
                            />
                            <p className="hint">지원 형식: CSV, JSON, Excel (.xlsx)</p>
                        </div>

                        {error && <div className="error-msg">{error}</div>}

                        <div className="modal-actions">
                            <button className="cancel-btn" onClick={onClose}>취소</button>
                            <button
                                className="analyze-btn"
                                onClick={handleUpload}
                                disabled={!file || loading}
                            >
                                {loading ? '분석 중...' : '데이터 분석 시작'}
                            </button>
                        </div>
                    </div>
                ) : (
                    <div className="result-step">
                        <div className="result-summary">
                            <p>✅ <strong>{analysisResult.rows}</strong> 개의 데이터를 분석했습니다.</p>
                            <p className="file-info">파일: {analysisResult.filename} (ID: {analysisResult.fileId?.substring(0, 8)}...)</p>
                        </div>

                        <div className="stats-table-wrapper">
                            <table className="stats-table">
                                <thead>
                                    <tr>
                                        <th>컬럼명</th>
                                        <th>타입</th>
                                        <th>통계 정보 (분포/범위)</th>
                                        <th>NULL 비율</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {Object.values(analysisResult.stats).map((stat) => (
                                        <tr key={stat.name}>
                                            <td>{stat.name}</td>
                                            <td>{stat.type}</td>
                                            <td>
                                                {stat.category === 'numeric' ? (
                                                    <span>Min: {stat.min} ~ Max: {stat.max} (Avg: {stat.mean?.toFixed(2)})</span>
                                                ) : (
                                                    <span>
                                                        Top: {Object.entries(stat.top_values || {})
                                                            .map(([k, v]) => `${k}(${v})`).join(', ')}
                                                    </span>
                                                )}
                                            </td>
                                            <td>
                                                {stat.count > 0 ? ((stat.null_count / stat.count) * 100).toFixed(1) : 0}%
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        <p className="info-msg">
                            * 이 데이터는 30분 후 자동으로 삭제됩니다. <br />
                            * 테스트 데이터 생성 시점까지 임시 보관됩니다.
                        </p>

                        <div className="modal-actions">
                            <button className="confirm-btn" onClick={handleConfirm}>
                                이 학습 결과 적용하기
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default SampleUploadModal;
