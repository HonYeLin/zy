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
import com.pawtrack.analysis.AnalysisService;
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
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;

@Service
@RequiredArgsConstructor
public class LocationLogService {

    private final LocationLogRepository locationLogRepository;
    private final AnimalRepository animalRepository;
    private final IAIProvider aiProvider;
    private final AnimalLifeNarrativeRepository animalLifeNarrativeRepository;
    private final AnalysisService analysisService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${ai.dashscope.api-key}")
    private String dashscopeApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

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
            // 如果未提供二维码
            // A. 首先检查用户是否提供了明确的昵称，且该昵称和种类在数据库中已经存在（尊重用户的手动选择）
            if (nickname != null && !nickname.trim().isEmpty()) {
                List<Animal> exactMatches = animalRepository.findByNameAndBreed(nickname.trim(), type);
                if (!exactMatches.isEmpty()) {
                    animal = exactMatches.get(0);
                    System.out.println("====== 精确匹配到已有名称的实体: ID=" + animal.getId() + ", 昵称=" + animal.getName() + " ======");
                }
            }
            
            // B. 如果精确匹配没有找到实体，我们才进行 Gemini AI 特征识别比对
            boolean shouldMatch = true;
            if (request.getSkipAiMatch() != null && request.getSkipAiMatch()) {
                shouldMatch = false;
                System.out.println("====== skipAiMatch=true, 跳过后台二次 AI 文本比对 ======");
            } else if (request.getPhotoUrl() != null && !request.getPhotoUrl().isEmpty()) {
                shouldMatch = false;
                System.out.println("====== 上传了照片，跳过后台二次 AI 文本比对 ======");
            }

            if (animal == null && shouldMatch) {
                List<Animal> sameBreedAnimals = animalRepository.findByBreed(type);
                Long matchedId = aiProvider.matchExistingAnimal(type, features, sameBreedAnimals);
                
                if (matchedId != null) {
                    java.util.Optional<Animal> matchedAnimal = animalRepository.findById(matchedId);
                    if (matchedAnimal.isPresent()) {
                        animal = matchedAnimal.get();
                        System.out.println("====== Gemini 成功识别为已有实体: ID=" + matchedId + ", 昵称=" + animal.getName() + " ======");
                        // 如果原本没有昵称或者用户指定了新名字，且该名字不为空，更新昵称
                        if (nickname != null && !nickname.isEmpty() && !animal.getName().equals(nickname)) {
                            animal.setName(nickname);
                            animal = animalRepository.save(animal);
                        }
                    }
                }
            }

            
            // C. 如果依然没有匹配到任何实体，则新建一个
            if (animal == null) {
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

        // 3.5. 处理照片重命名与转移 (将临时照片重命名并移至正式照片目录分类文件夹中)
        String finalPhotoUrl = request.getPhotoUrl();
        if (finalPhotoUrl != null && finalPhotoUrl.contains("/images/unclassified/")) {
            try {
                String tempFilename = finalPhotoUrl.substring(finalPhotoUrl.lastIndexOf("/") + 1);
                
                String userDir = System.getProperty("user.dir");
                File baseDir = new File(userDir);
                if (!userDir.endsWith("backend")) {
                    baseDir = new File(baseDir, "backend");
                }
                
                File uploadsDir = new File(baseDir, "uploads/images");
                File tempFile = new File(uploadsDir, "unclassified/" + tempFilename);
                
                if (tempFile.exists()) {
                    String ext = "";
                    if (tempFilename.contains(".")) {
                        ext = tempFilename.substring(tempFilename.lastIndexOf("."));
                    } else {
                        ext = ".jpg";
                    }
                    
                    String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    // 唯一标识结构为 "种类-该实体唯一ID"
                    String targetFilename = animal.getQrCodeId() + "_" + timestamp + ext;
                    
                    File animalDir = new File(uploadsDir, animal.getQrCodeId());
                    if (!animalDir.exists()) {
                        animalDir.mkdirs();
                    }
                    File destFile;
                    String relativeUrl;
                    
                    // 通过数据库历史记录判断是新实体还是已有实体（而非依赖文件夹是否存在）
                    List<LocationLog> existingLogs = locationLogRepository.findByAnimalIdOrderByRecordedAtDesc(animal.getId());
                    boolean isNewEntity = (existingLogs == null || existingLogs.isEmpty());
                    
                    if (isNewEntity) {
                        // 若为新实体（数据库中尚无任何打卡记录），照片直接存放在实体根目录下
                        destFile = new File(animalDir, targetFilename);
                        relativeUrl = "/images/" + animal.getQrCodeId() + "/" + targetFilename;
                        System.out.println("====== 新实体创建图片存储文件夹: " + animalDir.getAbsolutePath() + " ======");
                    } else {
                        // 若已有存储该实体图片的文件夹（数据库中已有历史记录），则分类（按行为）存储
                        String category = (behaviorTag != null) ? behaviorTag.name().toLowerCase() : "other";
                        File categoryDir = new File(animalDir, category);
                        if (!categoryDir.exists()) {
                            categoryDir.mkdirs();
                        }
                        destFile = new File(categoryDir, targetFilename);
                        relativeUrl = "/images/" + animal.getQrCodeId() + "/" + category + "/" + targetFilename;
                        System.out.println("====== 已有实体分类存储图片: " + destFile.getAbsolutePath() + " ======");
                    }
                    
                    // 移动文件
                    java.nio.file.Files.move(tempFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    finalPhotoUrl = relativeUrl;
                } else {
                    System.err.println("未找到临时上传的图片文件: " + tempFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("照片转移及重命名失败: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 4. 使用 EntityManager 执行原生 SQL 进行空间 Point 数据的插入 (SRID 4326, 坐标顺序为 latitude, longitude)
        entityManager.createNativeQuery(
            "INSERT INTO animal_logs (animal_id, user_id, location, behavior_tag, photo_url, description, scene_description, recorded_at) " +
            "VALUES (:animalId, :userId, ST_GeomFromText(:wkt, 4326), :behaviorTag, :photoUrl, :description, :sceneDescription, :recordedAt)"
        )
        .setParameter("animalId", animal.getId())
        .setParameter("userId", null) // 游客模式
        .setParameter("wkt", String.format("POINT(%f %f)", request.getLatitude(), request.getLongitude())) // SRID 4326: POINT(lat lng)
        .setParameter("behaviorTag", behaviorTag.name())
        .setParameter("photoUrl", finalPhotoUrl)
        .setParameter("description", features)
        .setParameter("sceneDescription", request.getSceneDescription())
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

        // 6. 异步后台生成 AI 生活记录日记与 AI 行为总结简介
        // 必须在事务提交后执行，否则异步线程可能读不到刚插入的打卡数据，也可能由于并发读取和保存 Animal 实体，覆盖掉刚才设置的 avatar_url
        final Long finalAnimalId = animal.getId();
        final String finalAnimalBreed = animal.getBreed();
        
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        generateNarrativeAsync(finalAnimalId, finalAnimalBreed);
                        CompletableFuture.runAsync(() -> {
                            try {
                                analysisService.updateAnimalSummary(finalAnimalId);
                            } catch (Exception e) {
                                System.err.println("更新动物 AI 简介失败: " + e.getMessage());
                            }
                        });
                    }
                }
            );
        } else {
            generateNarrativeAsync(finalAnimalId, finalAnimalBreed);
            CompletableFuture.runAsync(() -> {
                try {
                    analysisService.updateAnimalSummary(finalAnimalId);
                } catch (Exception e) {
                    System.err.println("更新动物 AI 简介失败: " + e.getMessage());
                }
            });
        }
        // 8. 触发头像更新：如果是第一次记录，直接同步设置头像，无需AI对比，提高响应速度并确保事务一致
        if (finalPhotoUrl != null && !finalPhotoUrl.trim().isEmpty() && !finalPhotoUrl.contains("/images/unclassified/")) {
            if (animal.getAvatarUrl() == null || animal.getAvatarUrl().trim().isEmpty()) {
                animal.setAvatarUrl(finalPhotoUrl);
                animal = animalRepository.save(animal);
                System.out.println("====== 同步设置初始头像: " + animal.getName() + " -> " + finalPhotoUrl + " ======");
            } else {
                // 如果已有头像，且新老头像不同，才在事务提交后异步进行AI头像甄选对比
                if (!animal.getAvatarUrl().equals(finalPhotoUrl)) {
                    final Long animalId = animal.getId();
                    final String photoUrlForAvatar = finalPhotoUrl;
                    if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                            new org.springframework.transaction.support.TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    CompletableFuture.runAsync(() -> {
                                        evaluateAndSelectAvatar(animalId, photoUrlForAvatar);
                                    });
                                }
                            }
                        );
                    } else {
                        CompletableFuture.runAsync(() -> {
                            evaluateAndSelectAvatar(animalId, photoUrlForAvatar);
                        });
                    }
                }
            }
        }

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
                String latestDesc = latestLog.getSceneDescription() != null && !latestLog.getSceneDescription().isEmpty() 
                    ? latestLog.getSceneDescription() 
                    : (latestLog.getDescription() != null ? latestLog.getDescription() : "无");

                sb.append("\n【本次新观察到的活动】:\n");
                sb.append("- 观察时间: ").append(latestLog.getRecordedAt()).append("\n");
                sb.append("- 行为状态: ").append(latestLog.getBehaviorLabel()).append("\n");
                sb.append("- 画面环境行为描述: ").append(latestDesc).append("\n\n");
                
                if (previousLog != null) {
                    String previousDesc = previousLog.getSceneDescription() != null && !previousLog.getSceneDescription().isEmpty() 
                        ? previousLog.getSceneDescription() 
                        : (previousLog.getDescription() != null ? previousLog.getDescription() : "无");

                    sb.append("【上次观察到的活动】:\n");
                    sb.append("- 观察时间: ").append(previousLog.getRecordedAt()).append("\n");
                    sb.append("- 行为状态: ").append(previousLog.getBehaviorLabel()).append("\n");
                    sb.append("- 画面环境行为描述: ").append(previousDesc).append("\n\n");
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
                    narrative.setModelVersion("deepseek-v4-pro");
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

    /**
     * 自动使用 AI 挑选具有代表性的动物头像照片，如果当前头像不存在直接绑定，如果存在则利用 Qwen-VL-Max 进行两图对比并异步更新
     */
    public void evaluateAndSelectAvatar(Long animalId, String newPhotoUrl) {
        try {
            java.util.Optional<Animal> optAnimal = animalRepository.findById(animalId);
            if (!optAnimal.isPresent()) {
                return;
            }
            Animal animal = optAnimal.get();

            // 1. 如果当前没有头像，则直接设置新照片为头像
            if (animal.getAvatarUrl() == null || animal.getAvatarUrl().trim().isEmpty()) {
                animal.setAvatarUrl(newPhotoUrl);
                animalRepository.save(animal);
                System.out.println("====== AI头像初始化: " + animal.getName() + " 直接绑定第一张照片为头像: " + newPhotoUrl + " ======");
                return;
            }

            String currentAvatarUrl = animal.getAvatarUrl();
            if (currentAvatarUrl.equals(newPhotoUrl)) {
                return; // 已经是同一张图，无需比对
            }

            // 2. 加载两张图的本地文件
            File currentFile = getLocalFileFromUrl(currentAvatarUrl);
            File newFile = getLocalFileFromUrl(newPhotoUrl);

            if (currentFile == null || !currentFile.exists()) {
                // 如果当前头像文件不存在，直接更新为新照片
                animal.setAvatarUrl(newPhotoUrl);
                animalRepository.save(animal);
                System.out.println("====== AI头像更新: 当前头像文件不存在，直接绑定新照片为头像 ======");
                return;
            }

            if (newFile == null || !newFile.exists()) {
                return; // 新文件不存在，无法比对
            }

            // 3. 将两张图转换为 Base64
            byte[] currentBytes = java.nio.file.Files.readAllBytes(currentFile.toPath());
            byte[] newBytes = java.nio.file.Files.readAllBytes(newFile.toPath());

            String currentMime = getMimeType(currentFile.getName());
            String newMime = getMimeType(newFile.getName());

            String currentDataUri = "data:" + currentMime + ";base64," + Base64.getEncoder().encodeToString(currentBytes);
            String newDataUri = "data:" + newMime + ";base64," + Base64.getEncoder().encodeToString(newBytes);

            // 4. 调用 Qwen-VL-Max 进行对比
            String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

            String promptText = "你是一个精密的动物照片评估助手。下面是同一只校园动物的两张照片：\n" +
                    "图片A (当前头像)\n" +
                    "图片B (新观察照片)\n" +
                    "请选择最适合作为该小动物在图鉴/卡片上显示的“代表性头像”的照片。\n" +
                    "挑选标准：\n" +
                    "1. 头像应该清晰，能清楚看清该动物的面部特征、五官或整体轮廓。\n" +
                    "2. 尽量选择正面、没有遮挡、没有严重运动模糊、光线良好的照片。\n" +
                    "3. 避免过度拉伸、裁剪不当或主体过小的照片。\n\n" +
                    "如果你认为图片B（新照片）明显比图片A更适合、更清晰、更具代表性，请回复字母 \"B\"。\n" +
                    "如果你认为图片A更好，或者两张照片差不多（没有显著提升），请回复字母 \"A\"。\n\n" +
                    "注意：请【仅】输出单个大写字母 \"A\" 或 \"B\"，不要包含任何解释、前缀、标点符号或Markdown格式。";

            Map<String, Object> textPart = Map.of(
                "type", "text",
                "text", promptText
            );

            Map<String, Object> imagePartA = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", currentDataUri)
            );

            Map<String, Object> imagePartB = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", newDataUri)
            );

            Map<String, Object> message = Map.of(
                "role", "user",
                "content", List.of(textPart, imagePartA, imagePartB)
            );

            Map<String, Object> requestBody = Map.of(
                "model", "qwen-vl-max",
                "messages", List.of(message)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + dashscopeApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    if (msg != null && msg.containsKey("content")) {
                        String content = (String) msg.get("content");
                        if (content != null) {
                            content = content.trim().toUpperCase();
                            System.out.println("====== Qwen-VL-Max 头像比对结果: " + content + " ======");
                            if (content.contains("B")) {
                                animal.setAvatarUrl(newPhotoUrl);
                                animalRepository.save(animal);
                                System.out.println("====== AI头像更新: " + animal.getName() + " 更换为更具代表性的新照片: " + newPhotoUrl + " ======");
                            } else {
                                System.out.println("====== AI头像保持: " + animal.getName() + " 维持当前头像不变 ======");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("AI头像自动甄选失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private File getLocalFileFromUrl(String photoUrl) {
        if (photoUrl == null || !photoUrl.contains("/images/")) {
            return null;
        }
        String relativePath = photoUrl.substring(photoUrl.indexOf("/images/") + "/images/".length());
        String userDir = System.getProperty("user.dir");
        File baseDir = new File(userDir);
        if (!userDir.endsWith("backend")) {
            baseDir = new File(baseDir, "backend");
        }
        File file = new File(baseDir, "uploads/images/" + relativePath);
        if (file.exists()) {
            return file;
        }
        File srcFile = new File(baseDir, "src/main/resources/static/images/" + relativePath);
        if (srcFile.exists()) {
            return srcFile;
        }
        return null;
    }

    private String getMimeType(String filename) {
        if (filename == null) return "image/jpeg";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}
