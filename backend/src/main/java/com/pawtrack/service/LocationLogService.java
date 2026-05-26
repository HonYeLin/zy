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
        String type = request.getType(); // e.g. "Cat", "Dog", "Other"
        String nickname = request.getNickname();
        if (nickname != null) {
            nickname = nickname.trim();
        }
        
        String qrCodeId = request.getQrCodeId();
        if (qrCodeId != null) {
            qrCodeId = qrCodeId.trim();
        }

        // 1. 获取或创建小动物档案
        Animal animal = null;
        if (qrCodeId != null && !qrCodeId.isEmpty()) {
            // 如果提供了二维码，优先通过二维码查找
            java.util.Optional<Animal> optAnimal = animalRepository.findByQrCodeId(qrCodeId);
            if (optAnimal.isPresent()) {
                animal = optAnimal.get();
                // 如果用户提供了新昵称，更新档案的昵称
                if (nickname != null && !nickname.isEmpty()) {
                    animal.setName(nickname);
                    animal = animalRepository.save(animal);
                }
            } else {
                // 如果二维码不存在，则新建
                animal = new Animal();
                String animalName = (nickname == null || nickname.isEmpty()) ? ("扫码" + type + "-" + qrCodeId) : nickname;
                animal.setName(animalName);
                animal.setBreed(type);
                animal.setQrCodeId(qrCodeId);
                animal = animalRepository.save(animal);
            }
        } else {
            // 如果未提供二维码，根据昵称查找/新建
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

            List<Animal> existingAnimals = animalRepository.findByNameAndBreed(animalName, type);
            if (!existingAnimals.isEmpty()) {
                animal = existingAnimals.get(0);
            } else {
                animal = new Animal();
                animal.setName(animalName);
                animal.setBreed(type);
                animal = animalRepository.save(animal);
            }
        }

        // 2. 确定行为标签 (用户选择优先，AI分类兜底)
        String userTag = request.getBehaviorTag();
        BehaviorTag behaviorTag;
        String features = request.getFeatures() != null ? request.getFeatures() : "";
        
        if (userTag != null && !userTag.trim().isEmpty()) {
            try {
                behaviorTag = BehaviorTag.valueOf(userTag.trim().toUpperCase());
            } catch (Exception e) {
                behaviorTag = BehaviorTag.OTHER;
            }
        } else {
            // AI 分类兜底
            String aiTag = aiProvider.classifyBehavior(features);
            try {
                behaviorTag = BehaviorTag.valueOf(aiTag);
            } catch (Exception e) {
                behaviorTag = BehaviorTag.OTHER;
            }
        }

        // 3. 计算时间偏移量 (recorded_at)
        Integer offset = request.getTimeOffset();
        LocalDateTime recordedAt = LocalDateTime.now();
        if (offset != null && offset > 0) {
            recordedAt = recordedAt.minusMinutes(offset);
        }

        // 4. 使用 EntityManager 执行原生 SQL 进行空间 Point 数据的插入 (SRID 4326, 坐标顺序为 latitude, longitude)
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

        // 5. 获取刚插入的数据 ID 并构造返回值
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

    public List<LocationLog> findAllLogs() {
        return locationLogRepository.findAll();
    }
}
