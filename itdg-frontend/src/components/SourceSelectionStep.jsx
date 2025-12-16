import React, { useState } from 'react';
import './SourceSelectionStep.css';

const SourceSelectionStep = ({ onNext }) => {
    const [selectedTab, setSelectedTab] = useState('git');
    const [formData, setFormData] = useState({
        // url: 'jdbc:postgresql://localhost:5432/itdg',
        // username: 'itdg',
        // password: '',
        gitUrl: '', // Default cleared
        file: null
    });

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData({ ...formData, [name]: value });
    };

    const handleFileChange = (e) => {
        setFormData({ ...formData, file: e.target.files[0] });
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        let payload = { type: selectedTab };
        /* 
        if (selectedTab === 'db') {
            payload = { ...payload, url: formData.url, username: formData.username, password: formData.password };
        } else 
        */
        if (selectedTab === 'git') {
            payload = { ...payload, gitUrl: formData.gitUrl };
        } else if (selectedTab === 'upload') {
            payload = { ...payload, file: formData.file };
        }

        onNext(payload);
    };

    return (
        <div className="source-selection-container">
            <div className="tabs">
                {/* 
                <button
                    className={`tab - btn ${ selectedTab === 'db' ? 'active' : '' } `}
                    onClick={() => setSelectedTab('db')}
                >
                    🗄️ 데이터베이스 연결
                </button>
                */}
                <button
                    className={`tab - btn ${selectedTab === 'git' ? 'active' : ''} `}
                    onClick={() => setSelectedTab('git')}
                >
                    <span className="icon">🐙</span> GitHub 리포지토리
                </button>
                <button
                    className={`tab - btn ${selectedTab === 'upload' ? 'active' : ''} `}
                    onClick={() => setSelectedTab('upload')}
                >
                    <span className="icon">📂</span> 프로젝트 업로드
                </button>
            </div>

            <form className="selection-form" onSubmit={handleSubmit}>
                {/* 
                {selectedTab === 'db' && (
                    <div className="tab-content">
                        <h3>데이터베이스 정보 입력</h3>
                        <div className="form-group">
                            <label>데이터베이스 주소 (JDBC URL)</label>
                            <input
                                type="text" name="url"
                                value={formData.url} onChange={handleInputChange}
                                placeholder="jdbc:postgresql://localhost:5432/mydb" required
                            />
                        </div>
                        <div className="form-group">
                            <label>사용자명 (Username)</label>
                            <input
                                type="text" name="username"
                                value={formData.username} onChange={handleInputChange} required
                            />
                        </div>
                        <div className="form-group">
                            <label>비밀번호 (Password)</label>
                            <input
                                type="password" name="password"
                                value={formData.password} onChange={handleInputChange} required
                            />
                        </div>
                    </div>
                )}
                */}

                {selectedTab === 'git' && (
                    <div className="tab-content">
                        <h3>GitHub 리포지토리 분석</h3>
                        <p className="description">
                            GitHub 리포지토리 주소를 입력하면, 코드를 분석하여 데이터베이스 스키마를 추출합니다.
                        </p>
                        <div className="form-group">
                            <label className="label-with-tooltip">
                                리포지토리 주소 (Git URL)
                                <div className="tooltip-container">
                                    <span className="help-icon">?</span>
                                    <div className="tooltip-content">
                                        <p>GitHub 페이지의 <strong>Code</strong> 버튼을 눌러 주소를 복사하세요.</p>
                                        <img src="/images/git-clone-help.png" alt="Git URL Help" className="help-image" />
                                    </div>
                                </div>
                            </label>
                            <input
                                type="text" name="gitUrl"
                                value={formData.gitUrl} onChange={handleInputChange}
                                placeholder="https://github.com/username/repository.git" required
                            />
                        </div>
                    </div>
                )}

                {selectedTab === 'upload' && (
                    <div className="tab-content">
                        <h3>로컬 프로젝트 업로드</h3>
                        <p className="description">
                            로컬 프로젝트 폴더를 압축(zip)하여 업로드하세요. (Java/JPA, Python/Django 등)
                        </p>
                        <div className="form-group">
                            <label>프로젝트 압축 파일 (.zip)</label>
                            <input
                                type="file" name="file"
                                accept=".zip" onChange={handleFileChange} required
                            />
                        </div>
                    </div>
                )}

                <button type="submit" className="next-btn" disabled={
                    (selectedTab === 'git' && !formData.gitUrl) ||
                    (selectedTab === 'upload' && !formData.file)
                }>
                    다음 단계로 이동 👉
                </button>
            </form>
        </div>
    );
};

export default SourceSelectionStep;
