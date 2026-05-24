package cn.edu.seig.vibemusic.model.entity;

import cn.edu.seig.vibemusic.constant.MessageConstant;
import cn.edu.seig.vibemusic.enumeration.UserStatusEnum;
import cn.edu.seig.vibemusic.enumeration.UserTypeEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serial;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户表（统一管理普通用户和管理员）
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("tb_user")
public class User extends BaseAccount {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long userId;

    /**
     * 用户手机号
     * 手机号格式：1开头，11位数字
     */
    @Pattern(regexp = "^1[3456789]\\d{9}$", message = MessageConstant.PHONE + MessageConstant.FORMAT_ERROR)
    @TableField("phone")
    private String phone;

    /**
     * 用户邮箱
     */
    @NotBlank(message = MessageConstant.EMAIL + MessageConstant.NOT_NULL)
    @Email(message = MessageConstant.EMAIL + MessageConstant.FORMAT_ERROR)
    @TableField("email")
    private String email;

    /**
     * 用户头像
     */
    @TableField("user_avatar")
    private String userAvatar;

    /**
     * 用户简介
     * 用户简介格式：100 字以内
     */
    @Pattern(regexp = "^.{0,100}$", message = MessageConstant.WORD_LIMIT_ERROR)
    @TableField("introduction")
    private String introduction;

    /**
     * 用户创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 用户修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 用户状态：0-启用，1-禁用
     */
    @TableField("status")
    private UserStatusEnum userStatus;

    /**
     * 用户类型：0-普通用户，1-管理员
     */
    @TableField("user_type")
    private UserTypeEnum userType;

}
