package com.talkweb.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @Test
    @DisplayName("驗證一次性密碼生成邏輯 (長度為 8 碼且由指定英數字組成)")
    void testGenerateOneTimePassword() {
        for (int i = 0; i < 50; i++) {
            String otp = AuthService.generateOneTimePassword();
            assertNotNull(otp);
            assertEquals(8, otp.length(), "一次性密碼長度應為 8 碼");
            assertTrue(otp.matches("^[ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789]{8}$"),
                    "一次性密碼格式符合不含易混淆字元的字元集");
        }
    }
}
