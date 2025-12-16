import React, { useState } from 'react';
import axios from 'axios';
import ResultViewer from './ResultViewer';
import SourceSelectionStep from './SourceSelectionStep';
import SchemaReviewStep from './SchemaReviewStep';
import './OrchestratorForm.css';

const OrchestratorForm = () => {
    const [step, setStep] = useState(1); // 1: Source, 2: Review, 3: Result
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // 분석된 스키마 데이터 (Step 1 -> 2)
    const [schemaMetadata, setSchemaMetadata] = useState(null);
    // 생성된 데이터 (Step 2 -> 3)
    const [generationResult, setGenerationResult] = useState(null);

    // Step 1 완료: 소스 선택 및 분석 요청
    const handleSourceSelected = async (sourcePayload) => {
        setLoading(true);
        setError(null);

        try {
            let response;
            if (sourcePayload.type === 'db') {
                response = await axios.post('http://localhost:8082/api/analyze', {
                    url: sourcePayload.url,
                    username: sourcePayload.username,
                    password: sourcePayload.password,
                    driverClassName: "org.postgresql.Driver" // TODO: Detect from URL
                });
            } else if (sourcePayload.type === 'git') {
                response = await axios.post('http://localhost:8082/api/analyze/git', {
                    url: sourcePayload.gitUrl
                });
            } else if (sourcePayload.type === 'upload') {
                const formData = new FormData();
                formData.append('file', sourcePayload.file);
                response = await axios.post('http://localhost:8082/api/analyze/upload', formData, {
                    headers: { 'Content-Type': 'multipart/form-data' }
                });
            }

            if (response && response.data.success) {
                setSchemaMetadata(response.data.data);
                setStep(2);
            } else {
                setError(response?.data?.message || '분석에 실패했습니다.');
            }
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || err.message || '서버 통신 오류');
        } finally {
            setLoading(false);
        }
    };

    // Step 2 완료: 데이터 생성 요청
    const handleGenerateData = async (config) => {
        setLoading(true);
        setError(null);

        try {
            // Orchestrator에 보낼 최종 페이로드 구성
            // 현재 Orchestrator는 DB 연결 정보를 원하지만, 
            // 여기서는 "분석된 메타데이터"를 기반으로 생성하라고 요청해야 함.
            // *중요*: Analyzer가 DB 없이 분석한 경우(Git/File), 실제 DB Connection이 없을 수 있음.
            // Generator가 순수 DTO 기반 생성을 지원하도록 백엔드 수정 필요할 수 있음.
            // 일단은 메타데이터 전체를 orchestrator에 넘기는 구조로 가정.

            const payload = {
                tables: config.tables,
                // 시드나 기타 전역 설정이 필요하다면 여기서 추가
            };

            const response = await axios.post('http://localhost:8081/api/orchestrator/process-metadata', payload);

            if (response.data.success) {
                setGenerationResult(response.data.data);
                setStep(3);
            } else {
                setError(response.data.message || '데이터 생성 실패');
            }
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || '데이터 생성 요청 중 오류 발생');
        } finally {
            setLoading(false);
        }
    };

    // 로딩 화면
    if (loading) {
        return (
            <div className="orchestrator-container loading-view">
                <div className="spinner"></div>
                <p>작업을 처리 중입니다... 🚀</p>
                {step === 1 && <p className="sub-text">소스 코드를 분석하고 있습니다. (Git Clone / Parsing)</p>}
                {step === 2 && <p className="sub-text">AI가 테스트 데이터를 생성하고 있습니다.</p>}
            </div>
        );
    }

    return (
        <div className="orchestrator-container">
            {step === 1 && (
                <div className="step-wrapper fade-in">
                    <h2>Step 1. 소스 선택</h2>
                    <p className="step-desc">데이터를 생성할 원천 소스를 선택해주세요.</p>
                    <SourceSelectionStep onNext={handleSourceSelected} />
                </div>
            )}

            {step === 2 && schemaMetadata && (
                <div className="step-wrapper fade-in">
                    <h2>Step 2. 스키마 검토 및 설정</h2>
                    <SchemaReviewStep
                        schemaData={schemaMetadata}
                        onNext={handleGenerateData}
                        onBack={() => setStep(1)}
                    />
                </div>
            )}

            {step === 3 && generationResult && (
                <div className="step-wrapper fade-in">
                    <h2>Step 3. 생성 결과</h2>
                    <ResultViewer data={generationResult} />
                    <button className="reset-btn" onClick={() => setStep(1)}>
                        🔄 처음으로 돌아가기
                    </button>
                </div>
            )}

            {/* Error/Result Modal */}
            {error && (
                <div className="modal-overlay">
                    <div className="modal-content error-modal fade-in">
                        <div className="modal-icon">ℹ️</div>
                        <h3>분석 결과 안내</h3>
                        <p>{error}</p>
                        <button className="close-btn" onClick={() => setError(null)}>
                            확인 후 돌아가기
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default OrchestratorForm;
