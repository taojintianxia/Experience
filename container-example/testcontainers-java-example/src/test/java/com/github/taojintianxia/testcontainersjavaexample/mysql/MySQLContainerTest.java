package com.github.taojintianxia.testcontainersjavaexample.mysql;

import com.github.taojintianxia.testcontainersjavaexample.mysql.container.MySQLContainer;
import org.junit.Test;

/**
 * @author Nianjun Sun
 * @date 2019/9/10 13:06
 */
public class MySQLContainerTest {

    @Test
    public void containerStartsAndPublicPortIsAvailable() {
        try (MySQLContainer container = new MySQLContainer()) {
            container.withEnv("MYSQL_ROOT_PASSWORD", "root");
            container.start();
        }
    }
}
