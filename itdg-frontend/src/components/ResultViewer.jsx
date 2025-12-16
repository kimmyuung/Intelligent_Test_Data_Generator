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

        switch (format) {
            case 'sql':
                import('../utils/ExportUtils').then(({ default: ExportUtils }) => ExportUtils.downloadSql(data));
                break;
            case 'csv':
                import('../utils/ExportUtils').then(({ default: ExportUtils }) => ExportUtils.downloadCsv(data));
                break;
            case 'xlsx':
                import('../utils/ExportUtils').then(({ default: ExportUtils }) => ExportUtils.downloadExcel(data, 'generated_data', 'xlsx'));
                break;
            case 'xls':
                import('../utils/ExportUtils').then(({ default: ExportUtils }) => ExportUtils.downloadExcel(data, 'generated_data', 'xls'));
                break;
            case 'json':
                import('../utils/ExportUtils').then(({ default: ExportUtils }) => ExportUtils.downloadJson(data));
                break;
            default:
                break;
        }
    };

    if (!data) return null;

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
                            <button onClick={() => handleDownload('sql')}>SQL (.sql)</button>
                            <button onClick={() => handleDownload('csv')}>CSV (.csv)</button>
                            <button onClick={() => handleDownload('xlsx')}>Excel (.xlsx)</button>
                            <button onClick={() => handleDownload('xls')}>Excel (.xls)</button>
                            <button onClick={() => handleDownload('json')}>JSON (.json)</button>
                        </div>
                    </div>
                </div>
            </div>
            <div className="code-container">
                <pre>{JSON.stringify(data, null, 2)}</pre>
            </div>
        </div>
    );
};

export default ResultViewer;
