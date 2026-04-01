package com.medical.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeptListVo {
    private Long deptId;
    private Long parentId;
    private String parentName;
    private String name;
    private String code;
    private String manager;
    private String phone;
    private String address;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
    /** 树形接口使用 */
    private List<DeptListVo> children;
}
