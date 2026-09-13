package com.nguyenquyen.vetautet.ddd.infrastructure.cache.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
public class RedisInfrasServiceImpl  implements RedisInfrasService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Override
    public void setString(String key, String value) {
        if (StringUtils.hasLength(key)) { // null or ''
            return;
        }
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public String getString(String key) {
//        Object result = redisTemplate.opsForValue().get(key);
//        if (result == null) {
//            return null;
//        }
//        return String.valueOf(result);
        return Optional.ofNullable(redisTemplate.opsForValue().get(key))
                .map(String::valueOf)
                .orElse(null);
    }

    @Override
    public void setObject(String key, Object value) {
//        log.info("Set redis::1, {}", key);
        if (!StringUtils.hasLength(key)) { // null or ''
//            log.info("Set redis::null, {}", StringUtils.hasLength(key));
            return;
        }

        try {
            redisTemplate.opsForValue().set(key, value);
        }catch (Exception e){
            log.error("setObject error: key={}, error={}", key, e.getMessage(), e);
        }
//        redisTemplate.opsForValue().set(key, value);
//        // Kiểm tra xem giá trị có được lưu thành công hay không
//        Object result = redisTemplate.opsForValue().get(key);
//        log.info("Set redis::{}", result != null && result.equals(value));
    }

    @Override
    public <T> T getObject(String key, Class<T> targetClass) {
        Object result = redisTemplate.opsForValue().get(key);
//        log.info("get Cache::{}", result);
        if (result == null) {
            return null;
        }
//        try {
//            log.info("get Cache::1{}", JSON.parseObject((String) result, targetClass));
//            return JSON.parseObject((String) result, targetClass);
//        } catch (Exception e) {
//            log.error("error Cache::{}", e);
//            return null;
//        }
        // Nếu kết quả là một LinkedHashMap
        if (result instanceof Map) {
            try {
                // Chuyển đổi LinkedHashMap thành đối tượng mục tiêu
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.convertValue(result, targetClass);
            } catch (IllegalArgumentException e) {
//                log.error("Error converting LinkedHashMap to object: {}", e.getMessage());
                return null;
            }
        }

        // Nếu result là String, thực hiện chuyển đổi bình thường
        if (result instanceof String) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue((String) result, targetClass);
            } catch (JsonProcessingException e) {
//                log.error("Error deserializing JSON to object: {}", e.getMessage());
                return null;
            }
        }

        return null; // hoặc ném ra một ngoại lệ tùy ý
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    @Override
    public void setInt(String key, int value) {
            redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public int getInt(String key) {
        return (int) redisTemplate.opsForValue().get(key);
    }


    // ==========================================
    // CÁC HÀM XỬ LÝ ZSET (Dành cho Auto-Cancel)
    // ==========================================

    @Override
    public Boolean zAdd(String key, String value, double score) {
        try {
            // Ném giá trị vào Sorted Set với Điểm số (Score)
            return redisTemplate.opsForZSet().add(key, value, score);
        } catch (Exception e) {
            log.error("Lỗi khi zAdd vào Redis, key={}", key, e);
            return false;
        }
    }

    @Override
    public Set<String> zRangeByScore(String key, double min, double max, long limit) {
        try {
            // Lấy ra dưới dạng Object
            Set<Object> rawSet = redisTemplate.opsForZSet().rangeByScore(key, min, max, 0, limit);
            if (rawSet == null) return new java.util.HashSet<>();

            // Ép toàn bộ thành String một cách an toàn
            return rawSet.stream()
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            log.error("Lỗi khi zRangeByScore trong Redis, key={}", key, e);
            return new java.util.HashSet<>();
        }
    }

    @Override
    public Long zRemove(String key, Object... values) {
        try {
            // Xóa phần tử khỏi ZSET sau khi đã xử lý xong
            return redisTemplate.opsForZSet().remove(key, values);
        } catch (Exception e) {
            log.error("Lỗi khi zRemove khỏi Redis, key={}", key, e);
            return 0L;
        }
    }
}
