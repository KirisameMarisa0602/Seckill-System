package com.kirisamemarisa.seckillsystem.utils;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {
    //调用 apache 底层的加密工具，扔进去一块原石（字符串），吐出一长串乱码（MD5哈希值）
    public static String md5(String src) {
        return DigestUtils.md5Hex(src);
    }

    //静态盐
    private static final String salt = "1a2b3c4d";

    //用户的明文密码（inputPass） 转换成 表单提交密码（formPass）
    public static String inputPassToFormPass(String inputPass) {
        String str = "" + salt.charAt(0) + salt.charAt(2) + inputPass + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    //表单提交的密码（formPass） 转换成 数据库最终存储的密码（DBPass）
    public static String formPassToDBPass(String formPass, String randomSalt) {
        String str = "" + randomSalt.charAt(0) + randomSalt.charAt(2) + formPass + randomSalt.charAt(5) + randomSalt.charAt(4);
        return md5(str);
    }

    //把前面两步（穿两件防弹衣）合并成了一步的“全自动加密流水线”
    public static String inputPassToDBPass(String inputPass, String randomSalt) {
        String formPass = inputPassToFormPass(inputPass);
        return formPassToDBPass(formPass, randomSalt);
    }
}