package com.evorsio.mybox;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith 核心测试
 * 用于验证模块架构和依赖关系
 */
class CoreModulithTests {

    @Test
    void verifiesModularStructure() {
        // 验证模块结构是否符合 Spring Modulith 规范
        ApplicationModules.of(MyboxApplication.class).verify();
    }
}
