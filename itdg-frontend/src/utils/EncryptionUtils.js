import JSEncrypt from 'jsencrypt';
import axios from 'axios';

/**
 * RSA Encryption Utility using JSEncrypt.
 * Fetches Public Key from backend and encrypts data.
 */
const EncryptionUtils = {
    publicKey: null,

    /**
     * Fetch the public key from the backend.
     * Should be called during component initialization.
     */
    fetchPublicKey: async () => {
        try {
            // Adjust port if needed. Analyzer runs on 8082 by default.
            const response = await axios.get('http://localhost:8082/api/analyze/public-key');
            if (response.data && response.data.success) {
                EncryptionUtils.publicKey = response.data.data; // Expecting Base64 string
                console.log("RSA Public Key fetched successfully.");
            } else {
                console.error("Failed to fetch public key:", response.data);
            }
        } catch (error) {
            console.error("Error fetching public key:", error);
        }
    },

    /**
     * Encrypts a string using the fetched public key.
     * @param {string} text - The text to encrypt (e.g., password)
     * @returns {string|null} - The encrypted base64 string, or null if key not set.
     */
    encrypt: (text) => {
        if (!EncryptionUtils.publicKey) {
            console.error("Public Key not set. Call fetchPublicKey() first.");
            return null;
        }

        const encryptor = new JSEncrypt();
        encryptor.setPublicKey(EncryptionUtils.publicKey);
        return encryptor.encrypt(text);
    }
};

export default EncryptionUtils;
