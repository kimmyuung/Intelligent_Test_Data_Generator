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


    React.useEffect(() => {
        // Fetch Public Key on component mount
        import('../utils/EncryptionUtils').then(({ default: EncryptionUtils }) => {
            EncryptionUtils.fetchPublicKey();
        });
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();

        let payload = { type: selectedTab };

        if (selectedTab === 'db') {
            // Encrypt Password
            const { default: EncryptionUtils } = await import('../utils/EncryptionUtils');
            const encryptedPassword = EncryptionUtils.encrypt(formData.password);

            if (!encryptedPassword) {
                alert("보안 키를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
                return;
            }

            payload = {
                ...payload,
                url: formData.url,
                username: formData.username,
                password: encryptedPassword
            };
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
                    <span className="icon">🗄️</span> 데이터베이스 연결
                </button>
                <button
                    className={`tab-btn ${selectedTab === 'git' ? 'active' : ''}`}
                    onClick={() => setSelectedTab('git')}
                >
                    <span className="icon">🐙</span> GitHub 리포지토리
                </button>
                <button
                    className={`tab-btn ${selectedTab === 'upload' ? 'active' : ''}`}
                    onClick={() => setSelectedTab('upload')}
                >
                    <span className="icon">📂</span> 프로젝트 업로드
                </button>
            </div>

            <form className="selection-form" onSubmit={handleSubmit}>
                {selectedTab === 'db' && (
                    <div className="tab-content fade-in">
                        <h3>데이터베이스 정보 입력</h3>
                        <p className="description">
                            운영 중인 데이터베이스에 직접 접속하여 스키마를 분석합니다.
                            <br />
                            <span className="security-note">🔒 비밀번호는 RSA 알고리즘으로 안전하게 암호화되어 전송됩니다.</span>
                        </p>
                        <div className="form-group">
                            <label>데이터베이스 주소 (JDBC URL)</label>
                            <input
                                type="text" name="url"
                                value={formData.url || ''} onChange={handleInputChange}
                                placeholder="jdbc:postgresql://localhost:5432/mydb" required
                            />
                        </div>
                        <div className="form-group">
                            <label>사용자명 (Username)</label>
                            <input
                                type="text" name="username"
                                value={formData.username || ''} onChange={handleInputChange} required
                            />
                        </div>
                        <div className="form-group">
                            <label>비밀번호 (Password)</label>
                            <input
                                type="password" name="password"
                                value={formData.password || ''} onChange={handleInputChange} required
                            />
                        </div>
                    </div>
                )}
            </form>
        </div>
    );
};

export default SourceSelectionStep;
