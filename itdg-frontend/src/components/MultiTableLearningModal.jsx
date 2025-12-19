import React, { useState, useRef } from 'react';
import axios from 'axios';
import './MultiTableLearningModal.css';

const ORCHESTRATOR_URL = process.env.REACT_APP_ORCHESTRATOR_URL || 'http://localhost:8080';

/**
 * 다중 테이블 학습 모달
 * 
 * 여러 테이블의 데이터를 동시에 업로드하고 
 * 테이블 간 관계(FK)를 정의하여 학습
 */
const MultiTableLearningModal = ({
    isOpen,
    onClose,
    tables,
    onTrainingComplete
}) => {
    // 상태 관리
    const [step, setStep] = useState('upload'); // 'upload' | 'relationships' | 'training' | 'complete'
    const [tableData, setTableData] = useState({}); // { tableName: { file, data, columns } }
    const [relationships, setRelationships] = useState([]);
    const [training, setTraining] = useState(false);
    const [trainingResult, setTrainingResult] = useState(null);
    const [error, setError] = useState(null);

    const fileInputRefs = useRef({});

    // 파일 선택 핸들러
    const handleFileSelect = async (tableName, file) => {
        try {
            setError(null);

            // FormData로 파일 분석 요청
            const formData = new FormData();
            formData.append('file', file);

            const response = await axios.post(
                `${ORCHESTRATOR_URL}/api/ml/analyze`,
                formData,
                { headers: { 'Content-Type': 'multipart/form-data' } }
            );

            if (response.data.success) {
                setTableData(prev => ({
                    ...prev,
                    [tableName]: {
                        file,
                        fileId: response.data.fileId,
                        columns: response.data.columns,
                        rowCount: response.data.rowCount,
                        stats: response.data.stats
                    }
                }));
            } else {
                setError(`${tableName} 파일 분석 실패: ${response.data.message}`);
            }
        } catch (err) {
            setError(`${tableName} 파일 업로드 실패: ${err.message}`);
        }
    };

    // 관계 추가
    const addRelationship = () => {
        setRelationships(prev => [...prev, {
            parent_table: '',
            child_table: '',
            parent_key: '',
            child_key: ''
        }]);
    };

    // 관계 수정
    const updateRelationship = (index, field, value) => {
        setRelationships(prev => {
            const updated = [...prev];
            updated[index] = { ...updated[index], [field]: value };
            return updated;
        });
    };

    // 관계 삭제
    const removeRelationship = (index) => {
        setRelationships(prev => prev.filter((_, i) => i !== index));
    };

    // 다중 테이블 학습 시작
    const startMultiTableTraining = async () => {
        try {
            setTraining(true);
            setError(null);

            // 테이블 데이터 준비
            const tablesPayload = Object.entries(tableData).map(([name, data]) => ({
                name,
                data: data.stats?.sampleData || [] // 분석 시 받은 샘플 데이터 사용
            }));

            // 관계 유효성 검사
            const validRelationships = relationships.filter(
                rel => rel.parent_table && rel.child_table && rel.parent_key && rel.child_key
            );

            if (tablesPayload.length < 2) {
                setError('다중 테이블 학습에는 최소 2개 테이블이 필요합니다.');
                setTraining(false);
                return;
            }

            if (validRelationships.length === 0) {
                setError('최소 1개의 테이블 관계를 정의해주세요.');
                setTraining(false);
                return;
            }

            const response = await axios.post(
                `${ORCHESTRATOR_URL}/api/ml/multi-table/train`,
                {
                    tables: tablesPayload,
                    relationships: validRelationships
                }
            );

            if (response.data.success) {
                setTrainingResult(response.data);
                setStep('complete');
            } else {
                setError(`학습 실패: ${response.data.message || 'Unknown error'}`);
            }
        } catch (err) {
            setError(`학습 실패: ${err.response?.data?.detail || err.message}`);
        } finally {
            setTraining(false);
        }
    };

    // 완료 핸들러
    const handleComplete = () => {
        if (trainingResult && onTrainingComplete) {
            onTrainingComplete(trainingResult);
        }
        onClose();
    };

    // 모든 테이블에 데이터가 있는지 확인
    const uploadedTableCount = Object.keys(tableData).length;
    const requiredTableCount = tables?.length || 0;
    const allTablesUploaded = uploadedTableCount >= 2;

    if (!isOpen) return null;

    return (
        <div className="multi-table-modal-overlay" onClick={onClose}>
            <div className="multi-table-modal" onClick={e => e.stopPropagation()}>
                {/* 헤더 */}
                <div className="modal-header">
                    <h2>🔗 다중 테이블 학습 (Multi-Table Learning)</h2>
                    <button className="close-btn" onClick={onClose}>×</button>
                </div>

                {/* 진행 표시기 */}
                <div className="progress-indicator">
                    <div className={`step ${step === 'upload' ? 'active' : ''}`}>
                        <span className="step-number">1</span>
                        <span className="step-label">테이블 업로드</span>
                    </div>
                    <div className="step-line"></div>
                    <div className={`step ${step === 'relationships' ? 'active' : ''}`}>
                        <span className="step-number">2</span>
                        <span className="step-label">관계 정의</span>
                    </div>
                    <div className="step-line"></div>
                    <div className={`step ${step === 'training' || step === 'complete' ? 'active' : ''}`}>
                        <span className="step-number">3</span>
                        <span className="step-label">학습</span>
                    </div>
                </div>

                {/* 에러 메시지 */}
                {error && (
                    <div className="error-message">
                        ⚠️ {error}
                    </div>
                )}

                {/* Step 1: 테이블 데이터 업로드 */}
                {step === 'upload' && (
                    <div className="step-content">
                        <h3>📤 테이블별 데이터 파일 업로드</h3>
                        <p className="step-description">
                            각 테이블에 해당하는 CSV/Excel 파일을 업로드하세요. (최소 2개 이상)
                        </p>

                        <div className="table-upload-list">
                            {tables?.map(table => (
                                <div key={table.tableName} className="table-upload-item">
                                    <div className="table-name">
                                        <span className="table-icon">📋</span>
                                        {table.tableName}
                                    </div>

                                    {tableData[table.tableName] ? (
                                        <div className="upload-success">
                                            <span className="success-icon">✅</span>
                                            <span className="file-info">
                                                {tableData[table.tableName].file.name}
                                                <small>({tableData[table.tableName].rowCount}행)</small>
                                            </span>
                                        </div>
                                    ) : (
                                        <div className="upload-area">
                                            <input
                                                type="file"
                                                accept=".csv,.xlsx,.xls"
                                                ref={el => fileInputRefs.current[table.tableName] = el}
                                                onChange={e => e.target.files[0] && handleFileSelect(table.tableName, e.target.files[0])}
                                                style={{ display: 'none' }}
                                            />
                                            <button
                                                className="upload-btn"
                                                onClick={() => fileInputRefs.current[table.tableName]?.click()}
                                            >
                                                📂 파일 선택
                                            </button>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>

                        <div className="step-actions">
                            <button className="secondary-btn" onClick={onClose}>취소</button>
                            <button
                                className="primary-btn"
                                onClick={() => setStep('relationships')}
                                disabled={!allTablesUploaded}
                            >
                                다음: 관계 정의 →
                            </button>
                        </div>
                    </div>
                )}

                {/* Step 2: 테이블 관계 정의 */}
                {step === 'relationships' && (
                    <div className="step-content">
                        <h3>🔗 테이블 간 관계 정의</h3>
                        <p className="step-description">
                            Foreign Key 관계를 정의하여 테이블 간 연결을 설정하세요.
                        </p>

                        <div className="relationships-list">
                            {relationships.map((rel, index) => (
                                <div key={index} className="relationship-item">
                                    <div className="rel-row">
                                        <div className="rel-field">
                                            <label>부모 테이블</label>
                                            <select
                                                value={rel.parent_table}
                                                onChange={e => updateRelationship(index, 'parent_table', e.target.value)}
                                            >
                                                <option value="">선택...</option>
                                                {Object.keys(tableData).map(name => (
                                                    <option key={name} value={name}>{name}</option>
                                                ))}
                                            </select>
                                        </div>
                                        <div className="rel-field">
                                            <label>부모 키 (PK)</label>
                                            <select
                                                value={rel.parent_key}
                                                onChange={e => updateRelationship(index, 'parent_key', e.target.value)}
                                                disabled={!rel.parent_table}
                                            >
                                                <option value="">선택...</option>
                                                {tableData[rel.parent_table]?.columns?.map(col => (
                                                    <option key={col} value={col}>{col}</option>
                                                ))}
                                            </select>
                                        </div>
                                    </div>
                                    <div className="rel-arrow">↓ FK 참조</div>
                                    <div className="rel-row">
                                        <div className="rel-field">
                                            <label>자식 테이블</label>
                                            <select
                                                value={rel.child_table}
                                                onChange={e => updateRelationship(index, 'child_table', e.target.value)}
                                            >
                                                <option value="">선택...</option>
                                                {Object.keys(tableData).map(name => (
                                                    <option key={name} value={name}>{name}</option>
                                                ))}
                                            </select>
                                        </div>
                                        <div className="rel-field">
                                            <label>자식 키 (FK)</label>
                                            <select
                                                value={rel.child_key}
                                                onChange={e => updateRelationship(index, 'child_key', e.target.value)}
                                                disabled={!rel.child_table}
                                            >
                                                <option value="">선택...</option>
                                                {tableData[rel.child_table]?.columns?.map(col => (
                                                    <option key={col} value={col}>{col}</option>
                                                ))}
                                            </select>
                                        </div>
                                    </div>
                                    <button
                                        className="remove-rel-btn"
                                        onClick={() => removeRelationship(index)}
                                    >
                                        🗑️
                                    </button>
                                </div>
                            ))}

                            <button className="add-rel-btn" onClick={addRelationship}>
                                + 관계 추가
                            </button>
                        </div>

                        <div className="step-actions">
                            <button className="secondary-btn" onClick={() => setStep('upload')}>
                                ← 이전
                            </button>
                            <button
                                className="primary-btn"
                                onClick={startMultiTableTraining}
                                disabled={relationships.length === 0 || training}
                            >
                                {training ? '학습 중...' : '🚀 학습 시작'}
                            </button>
                        </div>
                    </div>
                )}

                {/* Step 3: 학습 완료 */}
                {step === 'complete' && trainingResult && (
                    <div className="step-content complete-step">
                        <div className="success-icon-large">🎉</div>
                        <h3>학습 완료!</h3>

                        <div className="result-summary">
                            <div className="result-item">
                                <span className="label">모델 ID:</span>
                                <span className="value">{trainingResult.modelId}</span>
                            </div>
                            <div className="result-item">
                                <span className="label">모델 타입:</span>
                                <span className="value">{trainingResult.modelType}</span>
                            </div>
                            <div className="result-item">
                                <span className="label">학습 시간:</span>
                                <span className="value">{trainingResult.trainingTime}초</span>
                            </div>
                            <div className="result-item">
                                <span className="label">학습된 테이블:</span>
                                <span className="value">{trainingResult.tables?.join(', ')}</span>
                            </div>
                        </div>

                        <div className="step-actions">
                            <button className="primary-btn" onClick={handleComplete}>
                                ✓ 완료
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default MultiTableLearningModal;
