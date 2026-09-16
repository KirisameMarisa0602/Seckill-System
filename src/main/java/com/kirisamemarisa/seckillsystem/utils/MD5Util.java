package com.kirisamemarisa.seckillsystem.utils;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * 旧账号 MD5 密码工具。新注册一律 BCrypt；本类只给登录升级路径比对历史哈希。
 *
 * <p>两层哈希：先用类内静态盐把明文打成 formPass，再用用户随机盐打成 DBPass。
 * {@link #formPassToDBPass} 认「前端已做过第一层」的提交；{@link #inputPassToDBPass} 认纯明文。
 */
public class MD5Util {
    /** Apache Commons Codec 的 MD5 十六进制摘要。 */
    public static String md5(String src) {
        return DigestUtils.md5Hex(src);
    }

    /** 旧前端固定盐，与用户表里的随机 salt 不是一回事。 */
    private static final String salt = "1a2b3c4d";

    /**
     * 明文 → 表单密码。取静态盐的第 1、3、6、5 个字符夹在密码两侧再 MD5。
     */
    public static String inputPassToFormPass(String inputPass) {
        String str = "" + salt.charAt(0) + salt.charAt(2) + inputPass + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    /**
     * 表单密码 → 库内哈希。同样用随机盐的 charAt(0/2/5/4) 混入，所以随机盐长度必须 ≥ 6。
     */
    public static String formPassToDBPass(String formPass, String randomSalt) {
        String str = "" + randomSalt.charAt(0) + randomSalt.charAt(2) + formPass + randomSalt.charAt(5) + randomSalt.charAt(4);
        return md5(str);
    }

    /** 明文直接走到库内哈希，等于上面两步连起来。 */
    public static String inputPassToDBPass(String inputPass, String randomSalt) {
        String formPass = inputPassToFormPass(inputPass);
        return formPassToDBPass(formPass, randomSalt);
    }
}
