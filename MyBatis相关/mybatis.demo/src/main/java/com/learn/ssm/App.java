package com.learn.ssm;

import com.learn.ssm.pojo.Role;
import com.learn.ssm.pojo.RoleMapper;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.io.InputStream;

public class App {
    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory = null;
        String resource = "mybatis-config.xml";
        InputStream inputStream;
        inputStream = App.class.getClassLoader().getResourceAsStream(resource);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            // 你的应用逻辑代码
//            Role role = sqlSession.selectOne("com.learn.ssm.pojo.RoleMapper.getRole", 1L);
            RoleMapper roleMapper = sqlSession.getMapper(RoleMapper.class);
            Role role = roleMapper.getRole(1L);
            sqlSession.commit();//提交事务
        } catch (Exception ex) {
            sqlSession.rollback();//回滚事务
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }

        //数据库连接池信息
//        PooledDataSource dataSource = new PooledDataSource();
//        dataSource.setDriver("com.mysql.jdbc.Driver");
//        dataSource.setUsername("root");
//        dataSource.setPassword("123456");
//        dataSource.setUrl("jdbc:mysql://localhost:3306/ssm");
//        dataSource.setDefaultAutoCommit(false);
//        //采用 MyBatis 的 JDBC 事务方式
//        TransactionFactory transactionFactory = new JdbcTransactionFactory();
//        Environment environment = new Environment("development", transactionFactory, dataSource);
//        //创建 Configuration对象
//        Configuration configuraton = new Configuration(environment);
//        //注册一个 MyBatis 上下文别名
//        configuraton.getTypeAliasRegistry().registerAlias("role ", Role.class);
//        //加入一个映射器
////        configuraton.addMapper(RoleMapper.class);
//        //使用 SqlSession FactoryBuilder 构建 SqlSessionFactory
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuraton);
    }
}
