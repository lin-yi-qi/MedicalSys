package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.domain.entity.Nurse;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NurseMapper extends BaseMapper<Nurse> {
}
