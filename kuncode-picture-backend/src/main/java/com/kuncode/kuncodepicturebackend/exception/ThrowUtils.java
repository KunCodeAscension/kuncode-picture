package com.kuncode.kuncodepicturebackend.exception;

/**
 * 异常抛出工具类
 */
public class ThrowUtils {

    /**
     * 条件成立抛出异常
     *
     * @param condition 条件
     * @param exception 抛出的异常
     */
    public static void throwIf(boolean condition, RuntimeException exception) throws BusinessException {
        if (condition) {
            throw exception;
        }
    }

    /**
     * 条件成立抛出异常
     *
     * @param condition 条件
     * @param errorCode 响应错误码
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) throws BusinessException {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件成立抛出异常
     * 
     * @param condition 条件
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode,String message) throws BusinessException {
        throwIf(condition, new BusinessException(errorCode,message));
    }

}
