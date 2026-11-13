package com.easy.common.mapper.membermapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easy.common.bean.AI_conversation;
import org.apache.ibatis.annotations.Select;


public interface IAI_conversationMapper extends BaseMapper<AI_conversation> {
    @Select("SELECT COUNT(DISTINCT session_id) FROM ai_conversation WHERE deleted =0")
    Long countSessions();

}
