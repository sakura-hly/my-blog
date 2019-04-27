package com.learn.ssm;

import org.apache.ibatis.reflection.factory.DefaultObjectFactory;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class MyObjectFactory extends DefaultObjectFactory {

    private static final long serialVersionUID = -3215697574350565442L;

    Logger log = Logger.getLogger(MyObjectFactory.class.getName());

    private Object temp = null;

    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);
        log.info("初始化参数: [" + properties.toString() + "]");
    }

    @Override
    public <T> T create(Class<T> type) {
        T result = super.create(type);
        log.info("创建对象 " + result.toString());
        return result;
    }

    @Override
    public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        T result = super.create(type, constructorArgTypes, constructorArgs);
        log.info("创建对象 " + result.toString());
        temp = result;
        return result;
    }

    @Override
    public <T> boolean isCollection(Class<T> type) {
        return super.isCollection(type);
    }
}
