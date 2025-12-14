import React, { useState } from 'react';
import './SourceSelectionStep.css';

const SourceSelectionStep = ({ onNext }) => {
    const [selectedTab, setSelectedTab] = useState('db');
    const [formData, setFormData] = useState({
        url: 'jdbc:postgresql://localhost:5432/itdg',
        username: 'itdg',
        password: '',
        gitUrl: 'https://github.com/sukh115/GDD',
        file: null
    });

    const handleInputChange = (e) => {
        const { name, value, files } = e.target;
        if (name === 'file') {
            setFormData(prev => ({ ...prev, file: files[0] }));
        } else {
            setFormData(prev => ({ ...prev, [name]: value }));
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        let payload = { type: selectedTab };
        if (selectedTab === 'db') {
            payload = { ...payload, url: formData.url, username: formData.username, password: formData.password };
        } else if (selectedTab === 'git') {
            payload = { ...payload, gitUrl: formData.gitUrl };
        } else if (selectedTab === 'upload') {
            payload = { ...payload, file: formData.file };
        }

        onNext(payload);
    };

    return (
        <div className="source-selection-container">
            <div className="tabs">
                <button
                    className={`tab-btn ${selectedTab === 'db' ? 'active' : ''}`}
                    onClick={() => setSelectedTab('db')}
                >
                    🗄️ 데이터베이스 연결
                </button>
                <button
                    className={`tab-btn ${selectedTab === 'git' ? 'active' : ''}`}
                    onClick={() => setSelectedTab('git')}
                >
                    🐙 GitHub 리포지토리
                </button>
                <button
                    className={`tab-btn ${selectedTab === 'upload' ? 'active' : ''}`}
                    onClick={() => setSelectedTab('upload')}
                >
                    📁 로컬 프로젝트 업로드
                </button>
            </div>

            <form className="selection-form" onSubmit={handleSubmit}>
                {selectedTab === 'db' && (
                    <div className="tab-content">
                        <h3>데이터베이스 정보 입력</h3>
                        <div className="form-group">
                            <label>JDBC URL</label>
                            <input
                                type="text" name="url"
                                value={formData.url} onChange={handleInputChange}
                                placeholder="jdbc:postgresql://localhost:5432/mydb" required
                            />
                        </div>
                        <div className="form-group">
                            <label>Username</label>
                            <input
                                type="text" name="username"
                                value={formData.username} onChange={handleInputChange} required
                            />
                        </div>
                        <div className="form-group">
                            <label>Password</label>
                            <input
                                type="password" name="password"
                                value={formData.password} onChange={handleInputChange} required
                            />
                        </div>
                    </div>
                )}

                {selectedTab === 'git' && (
                    <div className="tab-content">
                        <h3>GitHub 리포지토리 주소</h3>
                        <div className="form-group">
                            <label>Repository URL</label>
                            <input
                                type="url" name="gitUrl"
                                value={formData.gitUrl} onChange={handleInputChange}
                                placeholder="https://github.com/username/repo" required
                            />
                        </div>
                        <p className="hint">
                            * 공개 리포지토리(Public)만 지원됩니다.<br />
                            * Java(JPA) 또는 SQL(DDL) 파일이 포함되어 있어야 분석 가능합니다.
                        </p>
                    </div>
                )}

                {selectedTab === 'upload' && (
                    <div className="tab-content">
                        <h3>프로젝트 압축 파일 업로드</h3>
                        <div className="dropbox">
                            <input
                                type="file" name="file"
                                accept=".zip" onChange={handleInputChange} required
                            />
                            <p>프로젝트 폴더를 .zip으로 압축하여 업로드하세요.</p>
                        </div>
                        <p className="hint">
                            * .sql 파일이나 Java Entity 클래스가 포함된 프로젝트여야 합니다.
                        </p>
                    </div>
                )}

                <button type="submit" className="next-btn">
                    다음 단계로 (분석 시작) 👉
                </button>
            </form>
        </div>
    );
};

export default SourceSelectionStep;
