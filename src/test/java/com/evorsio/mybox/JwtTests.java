package com.evorsio.mybox;

import com.evorsio.mybox.common.properties.AuthJwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(locations = "classpath:application-test.yml")
public class JwtTests {

    @Autowired
    private AuthJwtProperties authJwtProperties;

    @Test
    void testJwtSecretIsLoaded() {
        assertNotNull(authJwtProperties.getSecret(), "JWT Secret should not be null");
        System.out.println("JWT Secret successfully loaded: " + authJwtProperties.getSecret());
    }
}
