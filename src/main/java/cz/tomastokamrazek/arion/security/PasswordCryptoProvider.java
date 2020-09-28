package cz.tomastokamrazek.arion.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordCryptoProvider {

    @Autowired
    PasswordEncoder passwordEncoder;
    
    //TODO Remove dev data
    private static final String PASSWORD = "#$PTInk2n3";
    private static final String SALT = "a357b3f20eff0b5d";

    public String encrypt(String string) {
        if (StringUtils.isNotBlank(string)) {
            TextEncryptor encryptor = Encryptors.text(PASSWORD, SALT);
            return encryptor.encrypt(string);
        } else {
            throw new IllegalArgumentException("Empty string to encrypt.");
        }
    }

    public String decrypt(String string) {
        if (StringUtils.isNotBlank(string)) {
            TextEncryptor decryptor = Encryptors.text(PASSWORD, SALT);
            return decryptor.decrypt(string);
        } else {
            throw new IllegalArgumentException("Empty string to decrypt.");
        }

    }
    
    public String encode(CharSequence password) {
        return passwordEncoder.encode(password);
    }

}
