package com.learn.ssm.pojo;

import org.apache.ibatis.annotations.Select;

public interface RoleMapper {
    @Select("select id, rolename as roleName, note from t_role where id= #{id}")
    public Role getRole (Long id);
}
