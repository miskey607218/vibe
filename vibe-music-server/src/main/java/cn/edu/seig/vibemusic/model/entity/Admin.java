package cn.edu.seig.vibemusic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * <p>
 * 管理员实体（兼容保留，实际数据已合并到 tb_user）
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("tb_admin")
public class Admin extends BaseAccount {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 管理员 id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long adminId;

}
