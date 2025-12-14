import React, { useState } from 'react';
import axios from 'axios';
import ResultViewer from './ResultViewer';
import './OrchestratorForm.css';

const OrchestratorForm = () => {
    const [formData, setFormData] = useState({
        url: 'jdbc:postgresql://localhost:5432/itdg',
        username: 'itdg',
        password: '',
        rowCount: 5,
        seed: 12345
    });

    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setResult(null);

        try {
            const payload = {
                dbConnection: {
                    url: formData.url,
                    username: formData.username,
                    password: formData.password,
                    driverClassName: "org.postgresql.Driver" // Defaulting to PG for now
                },
                rowCount: parseInt(formData.rowCount),
                seed: parseInt(formData.seed)
            };

            // Orchestrator API call
            // Note: In real setup, might need proxy or CORS config. Assuming localhost access for now.
            const response = await axios.post('http://localhost:8081/api/orchestrator/process', payload);

            if (response.data.success) {
                setResult(response.data.data);
            } else {
                setError(response.data.message || 'Unknown error occurred');
            }
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || err.message || 'Failed to connect to Orchestrator');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="orchestrator-container">
            <form className="glass-form" onSubmit={handleSubmit}>
                <div className="form-group">
                    <label>Database URL</label>
                    <input
                        type="text"
                        name="url"
                        value={formData.url}
                        onChange={handleChange}
                        placeholder="jdbc:postgresql://localhost:5432/mydb"
                        required
                    />
                </div>

                <div className="form-row">
                    <div className="form-group">
                        <label>Username</label>
                        <input
                            type="text"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Password</label>
                        <input
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            required
                        />
                    </div>
                </div>

                <div className="form-row">
                    <div className="form-group">
                        <label>Row Count</label>
                        <input
                            type="number"
                            name="rowCount"
                            value={formData.rowCount}
                            onChange={handleChange}
                            min="1"
                            max="1000"
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Seed (Optional)</label>
                        <input
                            type="number"
                            name="seed"
                            value={formData.seed}
                            onChange={handleChange}
                        />
                    </div>
                </div>

                <button type="submit" className="generate-btn" disabled={loading}>
                    {loading ? (
                        <span className="spinner"></span>
                    ) : (
                        '🚀 Generate Data'
                    )}
                </button>

                {error && <div className="error-message">⚠️ {error}</div>}
            </form>

            {result && <ResultViewer data={result} />}
        </div>
    );
};

export default OrchestratorForm;
