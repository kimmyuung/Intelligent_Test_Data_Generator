import React, { useState } from 'react';
import './ResultViewer.css';

const ResultViewer = ({ data }) => {
    const [copied, setCopied] = useState(false);

    const handleCopy = () => {
        navigator.clipboard.writeText(JSON.stringify(data, null, 2));
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    if (!data) return null;

    return (
        <div className="result-viewer">
            <div className="result-header">
                <h3>🎉 Generated Data</h3>
                <button className={`copy-btn ${copied ? 'copied' : ''}`} onClick={handleCopy}>
                    {copied ? 'Copied!' : 'Copy JSON'}
                </button>
            </div>
            <div className="code-container">
                <pre>{JSON.stringify(data, null, 2)}</pre>
            </div>
        </div>
    );
};

export default ResultViewer;
