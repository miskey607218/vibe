package cn.edu.seig.vibemusic.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserTypeEnum {

    USER(0, "普通用户"),
    ADMIN(1, "管理员");

    @EnumValue
    private final Integer id;
    private final String name;

    UserTypeEnum(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

}
