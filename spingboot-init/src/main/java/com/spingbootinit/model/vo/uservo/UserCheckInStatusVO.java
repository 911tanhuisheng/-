package com.spingbootinit.model.vo.uservo;

import lombok.Data;

/**
 * 签到相关接口的返回体（给前端下拉框、签到按钮用）。
 * <ul>
 *   <li>GET /user/check-in/status：只读查询，不修改数据</li>
 *   <li>POST /user/check-in：签到成功后同样返回本结构，便于前端立刻刷新展示</li>
 * </ul>
 */
@Data
public class UserCheckInStatusVO {
    /** 按「上海时区」的自然日判断：lastCheckInDate 是否等于今天 */
    private boolean checkedToday;
    /** 历史累计签到成功次数（整数，null 在业务层会规范成 0 再返回） */
    private Integer checkInCount;
    /** 当前积分总额 */
    private Integer points;
}
