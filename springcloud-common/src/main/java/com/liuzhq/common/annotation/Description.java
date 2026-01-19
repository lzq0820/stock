package com.liuzhq.common.annotation;

import java.lang.annotation.*;

/**
 * 描述性注解，用于标注类、方法、属性的说明信息
 * 支持运行时获取注解信息，可标注在类、方法、字段（属性）上
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD}) // 支持类、方法、属性
@Retention(RetentionPolicy.RUNTIME) // 运行时保留，可通过反射获取
@Documented // 生成Javadoc时包含该注解
@Inherited // 允许子类继承父类的该注解（仅对类注解生效）
public @interface Description {

    /**
     * 核心属性：描述文本
     * @return 类/方法/属性的描述信息
     */
    String value() default "";

    /**
     * 扩展属性：作者（可选）
     * @return 标注者/开发者
     */
    String author() default "";

    /**
     * 扩展属性：版本（可选）
     * @return 标注的版本信息
     */
    String version() default "1.0";
}