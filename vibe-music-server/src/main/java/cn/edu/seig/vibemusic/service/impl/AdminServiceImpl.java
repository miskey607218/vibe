package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.constant.JwtClaimsConstant;
import cn.edu.seig.vibemusic.constant.MessageConstant;
import cn.edu.seig.vibemusic.enumeration.RoleEnum;
import cn.edu.seig.vibemusic.enumeration.UserStatusEnum;
import cn.edu.seig.vibemusic.enumeration.UserTypeEnum;
import cn.edu.seig.vibemusic.mapper.AdminMapper;
import cn.edu.seig.vibemusic.mapper.UserMapper;
import cn.edu.seig.vibemusic.model.dto.AdminDTO;
import cn.edu.seig.vibemusic.model.entity.Admin;
import cn.edu.seig.vibemusic.model.entity.User;
import cn.edu.seig.vibemusic.result.Result;
import cn.edu.seig.vibemusic.service.IAdminService;
import cn.edu.seig.vibemusic.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 管理员服务实现（已迁移到 tb_user 统一管理）
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 管理员注册（同时写入 tb_user，user_type=1）
     *
     * @param adminDTO 管理员信息
     * @return 结果
     */
    @Override
    public Result register(AdminDTO adminDTO) {
        // 1. 检查 tb_user 中用户名是否已存在
        User existUser = userMapper.selectOne(new QueryWrapper<User>().eq("username", adminDTO.getUsername()));
        if (existUser != null) {
            return Result.error(MessageConstant.USERNAME + MessageConstant.ALREADY_EXISTS);
        }

        String passwordMD5 = DigestUtils.md5DigestAsHex(adminDTO.getPassword().getBytes());

        // 2. 写入 tb_user（source of truth）
        User user = new User();
        user.setUsername(adminDTO.getUsername());
        user.setPassword(passwordMD5);
        user.setEmail(adminDTO.getUsername() + "@admin.vibe");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setUserStatus(UserStatusEnum.ENABLE);
        user.setUserType(UserTypeEnum.ADMIN);

        if (userMapper.insert(user) == 0) {
            return Result.error(MessageConstant.REGISTER + MessageConstant.FAILED);
        }

        // 3. 同步写入 tb_admin（兼容保留）
        Admin existAdmin = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", adminDTO.getUsername()));
        if (existAdmin == null) {
            Admin adminRegister = new Admin();
            adminRegister.setUsername(adminDTO.getUsername());
            adminRegister.setPassword(passwordMD5);
            adminMapper.insert(adminRegister);
        }

        return Result.success(MessageConstant.REGISTER + MessageConstant.SUCCESS);
    }

    /**
     * 管理员登录（从 tb_user 验证，要求 user_type=1）
     *
     * @param adminDTO 管理员信息
     * @return 结果
     */
    @Override
    public Result login(AdminDTO adminDTO) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", adminDTO.getUsername()));
        if (user == null || user.getUserType() != UserTypeEnum.ADMIN) {
            // fallback：检查 tb_admin 兼容
            Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", adminDTO.getUsername()));
            if (admin == null) {
                return Result.error(MessageConstant.USERNAME + MessageConstant.ERROR);
            }
            if (DigestUtils.md5DigestAsHex(adminDTO.getPassword().getBytes()).equals(admin.getPassword())) {
                Map<String, Object> claims = new HashMap<>();
                claims.put(JwtClaimsConstant.ROLE, RoleEnum.ADMIN.getRole());
                claims.put(JwtClaimsConstant.ADMIN_ID, admin.getAdminId());
                claims.put(JwtClaimsConstant.USERNAME, admin.getUsername());
                String token = JwtUtil.generateToken(claims);
                stringRedisTemplate.opsForValue().set(token, token, 6, TimeUnit.HOURS);
                return Result.success(MessageConstant.LOGIN + MessageConstant.SUCCESS, token);
            }
            return Result.error(MessageConstant.PASSWORD + MessageConstant.ERROR);
        }

        if (user.getUserStatus() != UserStatusEnum.ENABLE) {
            return Result.error(MessageConstant.ACCOUNT_LOCKED);
        }

        if (DigestUtils.md5DigestAsHex(adminDTO.getPassword().getBytes()).equals(user.getPassword())) {
            Map<String, Object> claims = new HashMap<>();
            claims.put(JwtClaimsConstant.ROLE, RoleEnum.ADMIN.getRole());
            claims.put(JwtClaimsConstant.ADMIN_ID, user.getUserId());
            claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
            claims.put(JwtClaimsConstant.EMAIL, user.getEmail());
            claims.put(JwtClaimsConstant.USER_TYPE, UserTypeEnum.ADMIN.getId());
            String token = JwtUtil.generateToken(claims);
            stringRedisTemplate.opsForValue().set(token, token, 6, TimeUnit.HOURS);
            return Result.success(MessageConstant.LOGIN + MessageConstant.SUCCESS, token);
        }

        return Result.error(MessageConstant.PASSWORD + MessageConstant.ERROR);
    }

    /**
     * 登出
     *
     * @param token 认证token
     * @return 结果
     */
    @Override
    public Result logout(String token) {
        Boolean result = stringRedisTemplate.delete(token);
        if (result != null && result) {
            return Result.success(MessageConstant.LOGOUT + MessageConstant.SUCCESS);
        } else {
            return Result.error(MessageConstant.LOGOUT + MessageConstant.FAILED);
        }
    }
}
