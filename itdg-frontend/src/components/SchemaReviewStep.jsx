import React, { useState, useEffect } from 'react';
import SampleUploadModal from './SampleUploadModal';
import './SchemaReviewStep.css';

const SchemaReviewStep = ({ schemaData, onNext, onBack }) => {
    const [selectedTables, setSelectedTables] = useState({});
    const [tableSettings, setTableSettings] = useState({});

    // ML Learning State
    const [learningModalOpen, setLearningModalOpen] = useState(false);
    const [currentTableForLearning, setCurrentTableForLearning] = useState(null);
    const [learnedData, setLearnedData] = useState({}); // { tableName: { fileId, stats } }

    useEffect(() => {
        // 초기화: 모든 테이블 선택 및 기본 rowCount 설정
        const initialSelected = {};
        const initialSettings = {};

        if (schemaData && schemaData.tables) {
            schemaData.tables.forEach(table => {
                initialSelected[table.tableName] = true;
                initialSettings[table.tableName] = { rowCount: 5 };
            });
        }
        setSelectedTables(initialSelected);
        setTableSettings(initialSettings);
    }, [schemaData]);

    const handleCheckboxChange = (tableName) => {
        setSelectedTables(prev => ({
            ...prev,
            [tableName]: !prev[tableName]
        }));
    };

    const handleRowCountChange = (tableName, value) => {
        setTableSettings(prev => ({
            ...prev,
            [tableName]: { ...prev[tableName], rowCount: parseInt(value) || 0 }
        }));
    };

    const handleGenerate = () => {
        // 선택된 테이블만 필터링하고 설정값을 병합하여 전달
        const finalTables = schemaData.tables
            .filter(t => selectedTables[t.tableName])
            .map(t => ({
                ...t,
                targetRowCount: tableSettings[t.tableName]?.rowCount || 5,
                learningData: learnedData[t.tableName] // Include learned stats if available
            }));

        onNext({ tables: finalTables });
    };

    const openLearningModal = (tableName) => {
        setCurrentTableForLearning(tableName);
        setLearningModalOpen(true);
    };

    const handleLearningComplete = (tableName, result) => {
        setLearnedData(prev => ({
            ...prev,
            [tableName]: result
        }));
    };

    if (!schemaData) return <div>데이터 로딩 중...</div>;

    const { projectInfo } = schemaData;

    return (
        <div className="schema-review-container">
            {/* 프로젝트 요약 배지 */}
            <div className={`project-badge ${projectInfo?.language?.toLowerCase() || 'unknown'}`}>
                <span className="lang-icon">
                    {projectInfo?.language === 'Java' ? '☕' :
                        projectInfo?.language === 'SQL' ? '🗃️' :
                            projectInfo?.language === 'Go' ? '🐹' :
                                projectInfo?.language === 'Swift' ? '🍎' :
                                    projectInfo?.language === 'Kotlin' ? '🟣' :
                                        projectInfo?.language === 'C/C++' ? '🇨' :
                                            projectInfo?.language === 'Python' ? '🐍' :
                                                projectInfo?.language === 'Node.js/TypeScript' ? '🟩' : '📁'}
                </span>
                <div className="badge-info">
                    <strong>감지된 프로젝트: {projectInfo?.language || '알 수 없음'}</strong>
                    <span>{projectInfo?.framework || '프레임워크 미감지'}</span>
                    <span className="file-count">
                        (파일 {projectInfo?.analyzedFiles}/{projectInfo?.totalFiles}개 분석됨)
                    </span>
                </div>
            </div>

            <h3>📊 분석된 스키마 검토 (Schema Review)</h3>
            <p className="description">
                데이터를 생성할 테이블을 선택하고, 각 테이블마다 생성할 행(Row) 수를 지정하세요.
            </p>

            <div className="table-list">
                {schemaData.tables.map(table => (
                    <div key={table.tableName} className={`table-card ${selectedTables[table.tableName] ? 'selected' : ''}`}>
                        <div className="card-header">
                            <label className="checkbox-label">
                                <input
                                    type="checkbox"
                                    checked={!!selectedTables[table.tableName]}
                                    onChange={() => handleCheckboxChange(table.tableName)}
                                />
                                <span className="table-name">{table.tableName}</span>
                            </label>
                            {table.primaryKeys && table.primaryKeys.length > 0 && (
                                <span className="pk-badge">PK: {table.primaryKeys.join(', ')}</span>
                            )}
                        </div>

                        {selectedTables[table.tableName] && (
                            <div className="card-body">
                                <ul className="column-list">
                                    {table.columns.map(col => (
                                        <li key={col.name} className="column-item">
                                            <span className="col-name">{col.name}</span>
                                            <span className="col-type">{col.dataType}</span>
                                        </li>
                                    ))}
                                </ul>
                                <div className="card-footer">
                                    <label>생성 개수:</label>
                                    <input
                                        type="number"
                                        min="1" max="1000"
                                        value={tableSettings[table.tableName]?.rowCount || 5}
                                        onChange={(e) => handleRowCountChange(table.tableName, e.target.value)}
                                    />
                                </div>
                                <div className="learning-section">
                                    <button 
                                        className={`learn-btn ${learnedData[table.tableName] ? 'learned' : ''}`}
                                        onClick={() => openLearningModal(table.tableName)}
                                    >
                                        {learnedData[table.tableName] ? '✅ 학습 완료' : '📈 데이터 학습시키기'}
                                    </button>
                                </div>
                            </div>
                            </div>
                        )}
                    </div>
                ))}
            </div>

            <div className="action-buttons">
                <button className="back-btn" onClick={onBack}>👈 다시 선택</button>
                <button className="generate-btn" onClick={handleGenerate}>
                    ✨ 데이터 생성하기 ({Object.values(selectedTables).filter(v => v).length}개 테이블)
                </button>
            </div>
            {
        learningModalOpen && (
            <SampleUploadModal
                tableName={currentTableForLearning}
                onClose={() => setLearningModalOpen(false)}
                onAnalyzeComplete={handleLearningComplete}
            />
        )
    }
        </div >
    );
};

export default SchemaReviewStep;
