package com.kuncode.kuncodepicturebackend.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.kuncode.kuncodepicturebackend.common.BaseResponse;
import com.kuncode.kuncodepicturebackend.common.ResultUtils;
import com.kuncode.kuncodepicturebackend.exception.BusinessException;
import com.kuncode.kuncodepicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    @ResponseBody
    public BaseResponse<?> notLoginExceptionHandler(NotLoginException e){
        log.error("NotLoginException",e);
        return ResultUtils.error(e.getCode(),e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    @ResponseBody
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e){
        log.error("NotPermissionException",e);
        return ResultUtils.error(e.getCode(),e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public BaseResponse<?> businessExceptionHandler(BusinessException e){
        log.error("BusinessException",e);
        return ResultUtils.error(e.getCode(),e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public BaseResponse<?> bindException(BindException e){
        log.error("BindException",e);
        BindingResult bindingResult = e.getBindingResult();
        List<ObjectError> allErrors = bindingResult.getAllErrors();
        // 暂时只返回一个错误信息即可
        return ResultUtils.error(ErrorCode.PARAMS_ERROR,allErrors.get(0).getDefaultMessage());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<?> Exception(Exception e){
        log.error("Exception",e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR,"系统错误");
    }

}
