package com.example.order_project_1;

import gaarason.database.generator.GeneralGenerator;
import gaarason.database.generator.Generator;
import gaarason.database.generator.appointment.Style;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@FixMethodOrder(MethodSorters.JVM)
class OrderProject1ApplicationTests {

    @Resource
    GeneralGenerator generator;

    // 执行此方法即可生成
    @Test
    public void generate() {
        // set
        // 风格切换
        generator.setStyle(Style.ENTITY);
        // set
        generator.setOutputDir("./src/test/java/");     // 所有生成文件的路径

        generator.setNamespace("com.example.order_project_1.models");                  // 所有生成文件的所属命名空间
        generator.setSpringBoot(Generator.SpringBootVersion.THREE);                             // 是否生成spring boot相关注解
        generator.setCorePoolSize(20);                  // 所用的线程数
        generator.setSwagger(false);                    // 是否生成swagger相关注解
        generator.setValidator(false);                  // 是否生成validator相关注解
        generator.setJdkDependVersion(Generator.JdkDependVersion.JAKARTA);  // jdk依赖使用的包是 javax 还是 jakarta ?


        generator.setEntityStaticField(false);          // 是否在实体中生成静态字段
        generator.setBaseEntityDir("base");             // 实体父类的相对路径
        generator.setBaseEntityFields("id");            // 实体父类存在的字段
        generator.setBaseEntityName("BaseEntity");      // 实体父类的类名
        generator.setEntityDir("entity");               // 实体的相对路径
        generator.setEntityPrefix("");                  // 实体的类名前缀
        generator.setEntitySuffix("");                  // 实体的类名后缀

//      generator.setColumnDisSelectable("created_at", "updated_at");             // 字段, 不可查询
//
//      generator.setColumnFill(FieldFill.NotFill.class, "created_at", "updated_at");  // 字段, 填充方式
//
//      generator.setColumnStrategy(FieldStrategy.Default.class, "created_at", "updated_at");   // 字段, 使用策略
//      generator.setColumnInsertStrategy(FieldStrategy.Never.class, "created_at", "updated_at");   // 字段, 新增使用策略
//      generator.setColumnUpdateStrategy(FieldStrategy.Never.class, "created_at", "updated_at");   // 字段, 更新使用策略
//      generator.setColumnConditionStrategy(FieldStrategy.Default.class, "created_at", "updated_at");   // 字段, 条件使用策略
//
//      generator.setColumnConversion(FieldConversion.Default.class, "created_at", "updated_at");   // 字段, 序列化与反序列化方式

        generator.setBaseModelDir("base");              // 模型父类的相对路径
        generator.setBaseModelName("BaseModel");        // 模型父类的类名
        generator.setModelDir("model");                 // 模型的相对路径
        generator.setModelPrefix("");                   // 模型的类名前缀
        generator.setModelSuffix("Model");              // 模型的类名后缀

        // 执行
        generator.run();
    }
}
