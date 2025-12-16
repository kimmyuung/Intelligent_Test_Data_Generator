import React from 'react';
import './ErrorModal.css';

const ErrorModal = ({ isOpen, message, onClose }) => {
    if (!isOpen) return null;

    return (
        <div className="error-modal-overlay" onClick={onClose}>
            <div className="error-modal-content" onClick={e => e.stopPropagation()}>
                <div className="error-modal-icon">⚠️</div>
                <h3 className="error-modal-title">오류가 발생했습니다</h3>
                <p className="error-modal-message">{message}</p>
                <button className="error-modal-close-btn" onClick={onClose}>
                    닫기
                </button>
            </div>
        </div>
    );
};

export default ErrorModal;
