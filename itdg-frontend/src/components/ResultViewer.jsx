import React, { useState } from 'react';
import './ResultViewer.css';

const ResultViewer = ({ data }) => {
    const [copied, setCopied] = useState(false);

    const handleCopy = () => {
        navigator.clipboard.writeText(JSON.stringify(data, null, 2));
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const handleDownload = (format) => {
        if (!data) return;

        import('../utils/ExportUtils').then(({ default: ExportUtils }) => {
            switch (format) {
                case 'sql': ExportUtils.downloadSql(data); break;
                case 'csv': ExportUtils.downloadCsv(data); break;
                case 'xlsx': ExportUtils.downloadExcel(data, undefined, 'xlsx'); break;
                case 'xls': ExportUtils.downloadExcel(data, undefined, 'xls'); break;
                case 'json': ExportUtils.downloadJson(data); break;
                default: break;
            }
        });
    };

    if (!data) return null;

    // 데이터 유효성 검사: 데이터 객체가 있고, 하나 이상의 테이블에 데이터가 들어있어야 함
    const hasData = Object.keys(data).length > 0 && Object.values(data).some(arr => Array.isArray(arr) && arr.length > 0);

    if (!hasData) {
        return (
            <div className="result-viewer error-state">
                <div className="error-report-card">
                    <div className="error-header">
                        <span className="error-icon">⚠️</span>
                        <h2>Test Data Generation Failed</h2>
                    </div>

                    <div className="error-summary">
                        <p><strong>Status:</strong> Process Completed (200 OK)</p>
                        <p><strong>Result:</strong> 0 Rows Generated</p>
                        <p className="description">
                            테스트 데이터 생성 프로세스는 정상적으로 종료되었으나, 결과 데이터셋이 비어있습니다.<br />
                            이는 시스템 오류가 아닌 <strong>논리적 제약 조건(Logical Constraint)</strong>이나 <strong>스키마 분석(Schema Analysis)</strong>의 한계일 가능성이 높습니다.
                        </p>
                    </div>

                    <div className="diagnostics-section">
                        <h3>🔍 Diagnostic Report (Troubleshooting)</h3>

                        <div className="diagnostic-item">
                            <h4>1. Foreign Key Integrity Violation (참조 무결성 위배 가능성)</h4>
                            <p>
                                자식 테이블(Child Table) 생성을 위해서는 부모 테이블(Parent Table)의 데이터가 메모리에 먼저 로드되어 있어야 합니다.
                                <br />- <strong>Check:</strong> 현재 선택된 테이블셋에 FK 부모 테이블이 포함되어 있는지 확인하십시오.
                                <br />- <strong>Action:</strong> 스키마 리뷰 단계에서 부모 테이블을 함께 선택하거나, 참조 무결성을 일시적으로 무시할 수 있는 옵션을 검토하세요.
                            </p>
                        </div>

                        <div className="diagnostic-item">
                            <h4>2. Schema Analysis Limitation (분석 실패)</h4>
                            <p>
                                <code>itdg-analyzer</code>가 리포지토리의 엔티티 관계를 완전히 파싱하지 못했을 수 있습니다.
                                <br />- <strong>Detection:</strong> 복잡한 JPA 연관관계(`@OneToMany`, `@ManyToMany` 등)나 MyBatis XML 매핑이 누락되었을 수 있습니다.
                                <br />- <strong>Action:</strong> 지원되지 않는 ORM 패턴이나 데이터 타입(`UserDefinedType`, `Geometry` 등)이 있는지 소스 코드를 확인해 주세요.
                            </p>
                        </div>

                        <div className="diagnostic-item">
                            <h4>3. Constraints Satisfaction Failure (제약 조건 충돌)</h4>
                            <p>
                                데이터 생성기가 주어진 제약 조건(Unique, Not Null, Length)을 만족하는 랜덤 값을 생성하는 데 실패했을 수 있습니다.
                                <br />- <strong>Example:</strong> `boolean` 타입 컬럼에 5개 이상의 유니크 값을 요구하는 경우 등 논리적으로 불가능한 요청.
                            </p>
                        </div>
                    </div>

                    <div className="error-actions">
                        <p className="guide-text">위 내용을 참고하여 설정을 변경하고 다시 시도해 주십시오.</p>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="result-viewer">
            <div className="result-header">
                <h3>🎉 생성된 데이터 완료</h3>
                <div className="button-group">
                    <button className={`copy-btn ${copied ? 'copied' : ''}`} onClick={handleCopy}>
                        {copied ? '복사됨!' : 'JSON 복사'}
                    </button>

                    <div className="dropdown">
                        <button className="download-btn">💾 다운로드</button>
                        <div className="dropdown-content">
                            <button onClick={() => handleDownload('xlsx')}>Excel (.xlsx)</button>
                            <button onClick={() => handleDownload('csv')}>CSV (.csv)</button>
                            <button onClick={() => handleDownload('json')}>JSON (.json)</button>
                            <button onClick={() => handleDownload('sql')}>SQL (.sql)</button>
                        </div>
                    </div>
                </div>
            </div>

            <div className="result-content">
                {Object.keys(data).map((tableName) => {
                    const rows = data[tableName];
                    if (!Array.isArray(rows) || rows.length === 0) {
                        return (
                            <div key={tableName} className="table-section">
                                <h4>{tableName} <span className="row-count">(0 rows)</span></h4>
                                <div className="no-data">데이터가 없습니다.</div>
                            </div>
                        );
                    }

                    const columns = Object.keys(rows[0]);

                    return (
                        <div key={tableName} className="table-section">
                            <div className="table-header-info">
                                <h4>📦 테이블: {tableName}</h4>
                                <span className="row-count">{rows.length} rows generated</span>
                            </div>

                            <div className="table-responsive">
                                <table className="data-table">
                                    <thead>
                                        <tr>
                                            <th>#</th>
                                            {columns.map((col) => (
                                                <th key={col}>{col}</th>
                                            ))}
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {rows.slice(0, 100).map((row, idx) => (
                                            <tr key={idx}>
                                                <td className="row-idx">{idx + 1}</td>
                                                {columns.map((col) => (
                                                    <td key={col} title={String(row[col])}>
                                                        {formatValue(row[col])}
                                                    </td>
                                                ))}
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                            {rows.length > 100 && (
                                <div className="more-rows-alert">
                                    ⚠️ 성능을 위해 상위 100개 행만 표시됩니다. 전체 데이터는 다운로드하여 확인하세요.
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

// 헬퍼 함수: 값 포맷팅
const formatValue = (val) => {
    if (val === null || val === undefined) return <span className="null-val">NULL</span>;
    if (typeof val === 'boolean') return val ? 'TRUE' : 'FALSE';
    if (typeof val === 'object') return JSON.stringify(val);
    const str = String(val);
    return str.length > 30 ? str.substring(0, 27) + '...' : str;
};

export default ResultViewer;
