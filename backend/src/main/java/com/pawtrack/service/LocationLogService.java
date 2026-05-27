package com.pawtrack.service;

import com.pawtrack.entity.Animal;
import com.pawtrack.entity.BehaviorTag;
import com.pawtrack.entity.LocationLog;
import com.pawtrack.entity.LocationLogCreateRequest;
import com.pawtrack.entity.AnimalLifeNarrative;
import com.pawtrack.repository.AnimalRepository;
import com.pawtrack.repository.LocationLogRepository;
import com.pawtrack.repository.AnimalLifeNarrativeRepository;
import com.pawtrack.analysis.IAIProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class LocationLogService {

    private final LocationLogRepository locationLogRepository;
    private final AnimalRepository animalRepository;
    private final IAIProvider aiProvider;
    private final AnimalLifeNarrativeRepository animalLifeNarrativeRepository;

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

        // 6. 异步后台生成 AI 生活记录日记，免去对主线程的 IO 阻塞
        generateNarrativeAsync(animal.getId(), animal.getBreed());

        return log;
    }

    /**
     * 异步生成 AI 生活记录叙事并保存 (不对主线程造成任何网络请求阻塞)
     */
    public void generateNarrativeAsync(Long animalId, String breed) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 获取该动物的全部足迹记录 (按时间降序)
                List<LocationLog> allLogs = locationLogRepository.findByAnimalIdOrderByRecordedAtDesc(animalId);
                if (allLogs == null || allLogs.isEmpty()) return;

                // 2. 转换成简洁的 JSON 数据结构
                List<Map<String, Object>> records = new ArrayList<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                
                // 正序排列数据，构建时间轨迹
                for (int i = allLogs.size() - 1; i >= 0; i--) {
                    LocationLog log = allLogs.get(i);
                    Map<String, Object> record = new HashMap<>();
                    record.put("time", log.getRecordedAt() != null ? log.getRecordedAt().format(formatter) : "未知时间");
                    record.put("place", (log.getDescription() != null && !log.getDescription().trim().isEmpty()) ? log.getDescription().trim() : "经纬度(" + log.getLongitude() + "," + log.getLatitude() + ")");
                    record.put("behavior", log.getBehaviorLabel());
                    records.add(record);
                }

                String jsonLogs = new ObjectMapper().writeValueAsString(records);

                // 3. 动态配置 Prompt
                String role = "Cat".equalsIgnoreCase(breed) ? "猫咪" : ("Dog".equalsIgnoreCase(breed) ? "狗子" : "小生命");
                String prompt = String.format(
                    "你是校园%s分析师，请根据以下提供的 JSON 轨迹数据，以该%s的第一人称视角，写一篇幽默、生动、温暖感人、极富趣味的生活日记（字数在150-300字）。请不要输出任何与日记无关的解释性前言或尾注，直接以日记内容输出，可以带有一点可爱的语气词和 Emoji 喔！\n\n轨迹数据 JSON：\n%s",
                    role, role, jsonLogs
                );

                // 4. 调用 Gemini 行为学分析引擎生成故事
                String narrativeText = aiProvider.reasonBehavior(prompt);

                if (narrativeText != null && !narrativeText.trim().isEmpty() && !narrativeText.startsWith("推理失败：")) {
                    // 5. 数据持久化入库
                    AnimalLifeNarrative narrative = new AnimalLifeNarrative();
                    narrative.setAnimalId(animalId);
                    narrative.setNarrativeContent(narrativeText);
                    narrative.setStartTime(allLogs.get(allLogs.size() - 1).getRecordedAt()); // 最早的时间
                    narrative.setEndTime(allLogs.get(0).getRecordedAt()); // 最新的时间
                    narrative.setSummaryType("DAILY");
                    narrative.setModelVersion("gemini-flash-latest");
                    narrative.setTokenUsage(0);
                    
                    animalLifeNarrativeRepository.save(narrative);
                    System.out.println("====== AI 暖心故事日记已为小动物(ID: " + animalId + ") 异步生成并入库！ ======");
                }
            } catch (Exception e) {
                System.err.println("异步生成 AI 生活记录日记失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
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
