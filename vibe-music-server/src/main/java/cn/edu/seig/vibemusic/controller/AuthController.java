package cn.edu.seig.vibemusic.controller;

import cn.edu.seig.vibemusic.constant.MessageConstant;
import cn.edu.seig.vibemusic.model.dto.LoginDTO;
import cn.edu.seig.vibemusic.result.Result;
import cn.edu.seig.vibemusic.service.IUserService;
import cn.edu.seig.vibemusic.util.BindingResultUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统一认证控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private IUserService userService;

    /**
     * 统一登录（支持用户名或邮箱 + 密码）
     * 根据用户类型返回不同角色 token
     */
    @PostMapping("/login")
    public Result login(@RequestBody @Valid LoginDTO loginDTO, BindingResult bindingResult) {
        String errorMessage = BindingResultUtil.handleBindingResultErrors(bindingResult);
        if (errorMessage != null) {
            return Result.error(errorMessage);
        }
        return userService.unifiedLogin(loginDTO);
    }

}
