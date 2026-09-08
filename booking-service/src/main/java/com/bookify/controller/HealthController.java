package com.bookify.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    @GetMapping("/health")
    public String health() {
        Map<String, String> map = new HashMap<String, String>();

        try {
            Integer i = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            if(i != null){
                map.put("postgres", "UP");
                map.put("postgres-result", i.toString());
            }
        } catch(Exception e) {
            throw new RuntimeException("Postgres failed. ", e);
        }

        try {
            String ping = redisConnectionFactory.getConnection().ping();

            if(ping != null){
                map.put("ping", "UP");
                map.put("ping-result", ping);
            }
        } catch (Exception e) {
            throw new RuntimeException("Redis failed. ", e);
        }


        return map.toString();
    }

}
