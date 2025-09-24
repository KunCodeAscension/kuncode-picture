package com.kuncode.kuncodepicturebackend.controller;

import com.kuncode.kuncodepicturebackend.common.BaseResponse;
import com.kuncode.kuncodepicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseBody
@RequestMapping("/health")
public class MainController {

    @GetMapping
    public BaseResponse<String> Health() {
        return ResultUtils.success("test");
    }

}
