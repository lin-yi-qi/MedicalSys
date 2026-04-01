package com.medical.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeptSaveDto {
    private Long parentId;
    @NotBlank(message = "科室名称不能为空")
    @Size(max = 100, message = "科室名称过长")
    private String name;
    @Size(max = 50, message = "科室代码过长")
    private String code;
    @Size(max = 50, message = "负责人过长")
    private String manager;
    @Size(max = 20, message = "电话过长")
    private String phone;
    @Size(max = 200, message = "地址过长")
    private String address;
    private Integer sortOrder;
    private Integer status;
    @Size(max = 500, message = "备注过长")
    private String remark;
}
