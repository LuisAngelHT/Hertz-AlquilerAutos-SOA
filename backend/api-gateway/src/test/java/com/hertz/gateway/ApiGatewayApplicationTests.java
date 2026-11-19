package com.hertz.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "gateway.internal-secret=test-only-gateway-secret")
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
