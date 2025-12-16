package com.itdg.analyzer.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

@Slf4j
@Service
public class EncryptionService {

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
            log.info("RSA Key Pair generated successfully.");
        } catch (Exception e) {
            log.error("Failed to generate RSA keys", e);
            throw new RuntimeException(e);
        }
    }

    public String getPublicKey() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Invalid encrypted text");
        }
    }
}
