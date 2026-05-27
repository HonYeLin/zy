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
import java.io.File;
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
        String features = request.getFeatures() != null ? request.getFeatures() : "";

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
                animal.setDescription(features);
                animal = animalRepository.save(animal);
            }
        } else {
            // 如果未提供二维码，读取该种类的所有动物，用 Gemini 判断是否为已有实体
            List<Animal> sameBreedAnimals = animalRepository.findByBreed(type);
            Long matchedId = aiProvider.matchExistingAnimal(type, features, sameBreedAnimals);
            
            if (matchedId != null) {
                java.util.Optional<Animal> matchedAnimal = animalRepository.findById(matchedId);
                if (matchedAnimal.isPresent()) {
                    animal = matchedAnimal.get();
                    System.out.println("====== Gemini 成功识别为已有实体: ID=" + matchedId + ", 昵称=" + animal.getName() + " ======");
                    // 如果原本没有昵称或者用户指定了新名字，则更新昵称
                    if (nickname != null && !nickname.isEmpty() && !animal.getName().equals(nickname)) {
                        animal.setName(nickname);
                        animal = animalRepository.save(animal);
                    }
                }
            }
            
            if (animal == null) {
                // 如果没有匹配到已有实体，则新建一个
                animal = new Animal();
                String animalName = (nickname == null || nickname.isEmpty()) ? ("未知" + ("Cat".equalsIgnoreCase(type) ? "猫猫" : ("Dog".equalsIgnoreCase(type) ? "狗子" : "小动物"))) : nickname;
                animal.setName(animalName);
                animal.setBreed(type);
                animal.setDescription(features);
                animal = animalRepository.save(animal); // 先保存以生成 ID
                
                animal.setQrCodeId(animal.getBreed() + "-" + animal.getId());
                animal = animalRepository.save(animal); // 再次保存更新唯一标识 ID
                System.out.println("====== 新建小动物实体且赋予唯一标识: " + animal.getQrCodeId() + " ======");
            }
        }

        // 优化整合实体特征 (如果已有特征描述，则与新特征进行融合)
        if (features != null && !features.trim().isEmpty()) {
            if (animal.getDescription() != null && !animal.getDescription().trim().isEmpty() && !animal.getDescription().equals(features)) {
                String optimizedFeatures = aiProvider.optimizeAnimalFeatures(animal.getDescription(), features);
                animal.setDescription(optimizedFeatures);
                animal = animalRepository.save(animal);
                System.out.println("====== 特征描述融合优化成功: " + optimizedFeatures + " ======");
            } else if (animal.getDescription() == null || animal.getDescription().trim().isEmpty()) {
                animal.setDescription(features);
                animal = animalRepository.save(animal);
            }
        }

        // 2. 确定行为标签 (用户选择优先，AI分类兜底)
        String userTag = request.getBehaviorTag();
        BehaviorTag behaviorTag;
        
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

        // 3.5. 处理照片重命名与转移 (将临时照片重命名为 唯一标识id+上传时间 并移至正式照片目录)
        String finalPhotoUrl = request.getPhotoUrl();
        if (finalPhotoUrl != null && finalPhotoUrl.contains("/images/unclassified/")) {
            try {
                String tempFilename = finalPhotoUrl.substring(finalPhotoUrl.lastIndexOf("/") + 1);
                
                String userDir = System.getProperty("user.dir");
                File baseDir = new File(userDir);
                if (!userDir.endsWith("backend")) {
                    baseDir = new File(baseDir, "backend");
                }
                
                File sourceTempFile = new File(baseDir, "src/main/resources/static/images/unclassified/" + tempFilename);
                File targetTempFile = new File(baseDir, "target/classes/static/images/unclassified/" + tempFilename);
                
                File tempFileToUse = sourceTempFile.exists() ? sourceTempFile : (targetTempFile.exists() ? targetTempFile : null);
                
                if (tempFileToUse != null) {
                    String ext = "";
                    if (tempFilename.contains(".")) {
                        ext = tempFilename.substring(tempFilename.lastIndexOf("."));
                    } else {
                        ext = ".jpg";
                    }
                    
                    String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    // 唯一标识结构为 "种类-该实体唯一ID"
                    String targetFilename = animal.getQrCodeId() + "_" + timestamp + ext;
                    
                    File sourceDestFile = new File(baseDir, "src/main/resources/static/images/" + targetFilename);
                    File targetDestFile = new File(baseDir, "target/classes/static/images/" + targetFilename);
                    
                    // 确保目标父文件夹存在
                    if (!sourceDestFile.getParentFile().exists()) {
                        sourceDestFile.getParentFile().mkdirs();
                    }
                    if (!targetDestFile.getParentFile().exists()) {
                        targetDestFile.getParentFile().mkdirs();
                    }
                    
                    // 拷贝至正式图片文件夹 (src & target)
                    java.nio.file.Files.copy(tempFileToUse.toPath(), sourceDestFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    java.nio.file.Files.copy(tempFileToUse.toPath(), targetDestFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    
                    // 删除临时文件夹中的照片
                    java.nio.file.Files.deleteIfExists(sourceTempFile.toPath());
                    java.nio.file.Files.deleteIfExists(targetTempFile.toPath());
                    
                    finalPhotoUrl = "http://localhost:8080/images/" + targetFilename;
                }
            } catch (Exception e) {
                System.err.println("照片转移及重命名失败: " + e.getMessage());
                e.printStackTrace();
            }
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
        .setParameter("photoUrl", finalPhotoUrl)
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
        log.setPhotoUrl(finalPhotoUrl);
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

                LocationLog latestLog = allLogs.get(0);
                LocationLog previousLog = allLogs.size() > 1 ? allLogs.get(1) : null;

                // 2. 获取已有的所有日记记录 (作为故事连续性参考)
                List<AnimalLifeNarrative> existingNarratives = animalLifeNarrativeRepository.findByAnimalIdOrderByStartTimeDesc(animalId);
                
                // 2.5. 幂等性校验：如果最新足迹已经生成过日记，则不再生成
                if (existingNarratives != null) {
                    boolean alreadyExists = existingNarratives.stream().anyMatch(n -> n.getStartTime().equals(latestLog.getRecordedAt()));
                    if (alreadyExists) {
                        System.out.println("====== 足迹记录时间 " + latestLog.getRecordedAt() + " 的日记已经存在，跳过生成 ======");
                        return;
                    }
                }

                StringBuilder historySb = new StringBuilder();
                if (existingNarratives != null && !existingNarratives.isEmpty()) {
                    // 按时间升序拼接历史，作为上下文给大模型参考
                    for (int i = existingNarratives.size() - 1; i >= 0; i--) {
                        historySb.append("- ").append(existingNarratives.get(i).getNarrativeContent()).append("\n");
                    }
                }

                // 3. 构建精心设计的 Prompt，引导大模型按要求输出
                String breedRole = "Cat".equalsIgnoreCase(breed) ? "猫咪" : ("Dog".equalsIgnoreCase(breed) ? "狗子" : "小生命");
                String nickname = latestLog.getAnimal().getName();
                
                StringBuilder sb = new StringBuilder();
                sb.append("你是一个校园小动物生活记录分析师。请为校园里的").append(breedRole).append("“").append(nickname).append("”写一句话的最新生活记录。\n\n");
                sb.append("【已有历史生活记录】（作为故事连续性参考，请不要重复它们，而是在此基础上进行故事延伸）：\n");
                if (historySb.length() > 0) {
                    sb.append(historySb.toString());
                } else {
                    sb.append("(暂无历史记录)\n");
                }
                sb.append("\n【本次新观察到的活动】:\n");
                sb.append("- 观察时间: ").append(latestLog.getRecordedAt()).append("\n");
                sb.append("- 行为状态: ").append(latestLog.getBehaviorLabel()).append("\n");
                sb.append("- 环境/特征描述: ").append(latestLog.getDescription() != null ? latestLog.getDescription() : "无").append("\n\n");
                
                if (previousLog != null) {
                    sb.append("【上次观察到的活动】:\n");
                    sb.append("- 观察时间: ").append(previousLog.getRecordedAt()).append("\n");
                    sb.append("- 行为状态: ").append(previousLog.getBehaviorLabel()).append("\n");
                    sb.append("- 环境/特征描述: ").append(previousLog.getDescription() != null ? previousLog.getDescription() : "无").append("\n\n");
                }
                
                sb.append("【写作要求】:\n");
                sb.append("1. 只能写一段，字数控制在50-100字，必须简短。\n");
                sb.append("2. 绝对不能使用任何排版或Markdown标记（如 #, *, - 等，不要加粗或使用任何项目符号，直接输出纯文本）。\n");
                sb.append("3. 写作风格和格式：描述现在的行为和环境，联想猜测上次记录的连贯性，外加一点点俏皮和拟人化的猜测。\n");
                sb.append("   - 严禁出现像“观察时间：...”，“状态：...”这种死板的陈述，要用流畅的口语段落。\n");
                sb.append("   - 参考范例：\"这只大橘现在在爬在石板上睡觉，上次见到它还是下午在草地上晒太阳，或许刚才才吃饱了正在做美梦呢\"\n");
                sb.append("4. 请直接输出这一段话，不要带有任何其他前缀、后缀、说明性或过渡文字。");

                // 4. 调用 Gemini 行为学分析引擎生成故事
                String narrativeText = aiProvider.reasonBehavior(sb.toString());

                if (narrativeText != null && !narrativeText.trim().isEmpty() && !narrativeText.startsWith("推理失败：")) {
                    // 过滤可能包含的僵硬排版字符
                    narrativeText = narrativeText.replaceAll("[#\\*\\-`]", "").trim();

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
