package com.medical.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理员创建用户
 */
@Data
public class UserCreateDto {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度为 3～32 个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为 6～64 个字符")
    private String password;

    @Size(max = 50, message = "姓名最多 50 个字符")
    private String name;

    @Size(max = 100, message = "邮箱过长")
    private String email;

    @Size(max = 20, message = "手机号过长")
    private String mobilePhone;

    @NotEmpty(message = "请至少选择一个角色")
    private List<Long> roleIds;
}
