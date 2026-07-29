package com.kirisamemarisa.seckillsystem.utils;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {
    public static String md5(String src) {
        return DigestUtils.md5Hex(src);
    }
    private static final String salt = "1a2b3c4d";
    public static String inputPassToFormPass(String inputPass) {
        String str = "" + salt.charAt(0) + salt.charAt(2) + inputPass + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }
    public static String formPassToDBPass(String formPass, String randomSalt) {
        String str = "" + randomSalt.charAt(0) + randomSalt.charAt(2) + formPass + randomSalt.charAt(5) + randomSalt.charAt(4);
        return md5(str);
    }
    public static String inputPassToDBPass(String inputPass, String randomSalt) {
        String formPass = inputPassToFormPass(inputPass);
        return formPassToDBPass(formPass, randomSalt);
    }
}