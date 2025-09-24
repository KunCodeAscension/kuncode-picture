package com.kuncode.kuncodepicturebackend.common;

import com.kuncode.kuncodepicturebackend.exception.ErrorCode;

public class ResultUtils {

    /**
     * 成功
     *
     * @param data 数据
     * @param <T> 数据类型
     * @param message 响应信息
     * @return 响应
     */
    public static <T> BaseResponse<T> success(T data,String message){
        return new BaseResponse<>(0,data,message);
    }


    /**
     * 成功
     *
     * @param data 数据
     * @param <T> 数据类型
     * @return 响应
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0,data,"ok");
    }

    /**
     * 失败
     *
     * @param errorCode 失败错误码
     * @param message 失败信息
     * @return 响应
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode,String message){
        return new BaseResponse<>(errorCode.getCode(),null,message);
    }

    /**
     * 失败
     *
     * @param errorCode 失败错误码
     * @return 响应
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode.getCode(),null,errorCode.getMessage());
    }

    /**
     * 失败
     *
     * @param code 错误码
     * @param message 错误信息
     * @return 响应
     */
    public static <T> BaseResponse<T> error(int code,String message){
        return new BaseResponse<>(code,null,message);
    }


}
