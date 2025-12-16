import React, { useState } from 'react';
import './ResultViewer.css';

const ResultViewer = ({ data }) => {
    const [copied, setCopied] = useState(false);

    const handleCopy = () => {
        navigator.clipboard.writeText(JSON.stringify(data, null, 2));
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const handleDownloadSql = () => {
        if (!data) return;

        let sqlContent = "";
        Object.keys(data).forEach(tableName => {
            const rows = data[tableName];
            if (Array.isArray(rows)) {
                rows.forEach(row => {
                    const columns = Object.keys(row);
                    const values = Object.values(row).map(val => {
                        if (val === null) return 'NULL';
                        if (typeof val === 'number') return val;
                        return `'${String(val).replace(/'/g, "''")}'`; // Escape single quotes
                    });
                    sqlContent += `INSERT INTO ${tableName} (${columns.join(", ")}) VALUES (${values.join(", ")});\n`;
                });
                sqlContent += "\n";
            }
        });

        const blob = new Blob([sqlContent], { type: "text/sql;charset=utf-8" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "generated_data.sql";
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
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
                    <button className="download-btn" onClick={handleDownloadSql}>
                        💾 SQL 다운로드
                    </button>
                </div>
            </div>
            <div className="code-container">
                <pre>{JSON.stringify(data, null, 2)}</pre>
            </div>
        </div>
    );
};

export default ResultViewer;
