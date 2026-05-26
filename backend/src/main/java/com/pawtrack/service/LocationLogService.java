package com.pawtrack.service;

import com.pawtrack.entity.Animal;
import com.pawtrack.entity.BehaviorTag;
import com.pawtrack.entity.LocationLog;
import com.pawtrack.entity.LocationLogCreateRequest;
import com.pawtrack.repository.AnimalRepository;
import com.pawtrack.repository.LocationLogRepository;
import com.pawtrack.analysis.IAIProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationLogService {

    private final LocationLogRepository locationLogRepository;
    private final AnimalRepository animalRepository;
    private final IAIProvider aiProvider;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public LocationLog saveLog(LocationLogCreateRequest request) {
        // 1. 获取或创建小动物档案
        String type = request.getType(); // e.g. "Cat", "Dog", "Other"
        String nickname = request.getNickname();
        if (nickname != null) {
            nickname = nickname.trim();
        }
        
        String animalName;
        if (nickname == null || nickname.isEmpty()) {
            if ("Cat".equalsIgnoreCase(type)) {
                animalName = "未知猫猫";
            } else if ("Dog".equalsIgnoreCase(type)) {
                animalName = "未知狗子";
            } else {
                animalName = "未知小动物";
            }
        } else {
            animalName = nickname;
        }

        // 查询数据库中是否已存在同名且同分类的小动物
        List<Animal> existingAnimals = animalRepository.findByNameAndBreed(animalName, type);
        Animal animal;
        if (!existingAnimals.isEmpty()) {
            animal = existingAnimals.get(0);
        } else {
            animal = new Animal();
            animal.setName(animalName);
            animal.setBreed(type);
            animal = animalRepository.save(animal);
        }

        // 2. 调用 AI 预测行为标签
        String features = request.getFeatures() != null ? request.getFeatures() : "";
        String aiTag = aiProvider.classifyBehavior(features);
        BehaviorTag behaviorTag;
        try {
            behaviorTag = BehaviorTag.valueOf(aiTag);
        } catch (Exception e) {
            behaviorTag = BehaviorTag.OTHER;
        }

        // 3. 使用 EntityManager 执行原生 SQL 进行空间 Point 数据的插入 (SRID 4326, 坐标顺序为 latitude, longitude)
        LocalDateTime recordedAt = LocalDateTime.now();
        
        entityManager.createNativeQuery(
            "INSERT INTO animal_logs (animal_id, user_id, location, behavior_tag, photo_url, description, recorded_at) " +
            "VALUES (:animalId, :userId, ST_GeomFromText(:wkt, 4326), :behaviorTag, :photoUrl, :description, :recordedAt)"
        )
        .setParameter("animalId", animal.getId())
        .setParameter("userId", null) // 游客模式
        .setParameter("wkt", String.format("POINT(%f %f)", request.getLatitude(), request.getLongitude())) // SRID 4326: POINT(lat lng)
        .setParameter("behaviorTag", behaviorTag.name())
        .setParameter("photoUrl", request.getPhotoUrl())
        .setParameter("description", features)
        .setParameter("recordedAt", recordedAt)
        .executeUpdate();

        // 4. 获取刚插入的数据 ID 并构造返回值
        Number lastId = (Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
        
        LocationLog log = new LocationLog();
        log.setId(lastId.longValue());
        log.setAnimal(animal);
        log.setLatitude(request.getLatitude());
        log.setLongitude(request.getLongitude());
        log.setBehaviorTag(behaviorTag);
        log.setDescription(features);
        log.setPhotoUrl(request.getPhotoUrl());
        log.setRecordedAt(recordedAt);
        return log;
    }

    public List<LocationLog> findRecentByAnimalId(Long animalId) {
        return locationLogRepository.findByAnimalIdOrderByRecordedAtDesc(animalId);
    }

    public List<LocationLog> findWithinRadius(Double lat, Double lng, Double radius) {
        // SRID 4326 geometry point representation in WKT is: POINT(lat lng)
        String wkt = String.format("POINT(%f %f)", lat, lng);
        return locationLogRepository.findLogsWithinRadius(wkt, radius);
    }
}
