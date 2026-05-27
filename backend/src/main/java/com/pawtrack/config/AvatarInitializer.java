package com.pawtrack.config;

import com.pawtrack.entity.Animal;
import com.pawtrack.entity.LocationLog;
import com.pawtrack.repository.AnimalRepository;
import com.pawtrack.repository.LocationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class AvatarInitializer implements ApplicationRunner {

    private final AnimalRepository animalRepository;
    private final LocationLogRepository locationLogRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 在后台异步扫描并初始化无头像的历史小动物实体
        CompletableFuture.runAsync(() -> {
            try {
                // 等待系统及数据库完全启动就绪
                Thread.sleep(5000);
                System.out.println("====== [AI头像初始化任务] 开始扫描历史小动物档案... ======");
                List<Animal> animals = animalRepository.findAll();
                int count = 0;
                for (Animal animal : animals) {
                    if (animal.getAvatarUrl() == null || animal.getAvatarUrl().trim().isEmpty()) {
                        // 寻找其历史足迹照片
                        List<LocationLog> logs = locationLogRepository.findByAnimalIdOrderByRecordedAtDesc(animal.getId());
                        if (logs != null && !logs.isEmpty()) {
                            // 寻找第一张有照片的足迹
                            String selectedPhotoUrl = null;
                            for (LocationLog log : logs) {
                                if (log.getPhotoUrl() != null && !log.getPhotoUrl().trim().isEmpty() && !log.getPhotoUrl().contains("/images/unclassified/")) {
                                    selectedPhotoUrl = log.getPhotoUrl();
                                    break;
                                }
                            }
                            if (selectedPhotoUrl != null) {
                                // 找到了一张历史照片，触发头像设定（如果有更多，在未来的标记中会自动用 AI 进行对比和优化）
                                animal.setAvatarUrl(selectedPhotoUrl);
                                animalRepository.save(animal);
                                count++;
                                System.out.println("====== [AI头像初始化任务] 已为小动物 " + animal.getName() + " 绑定历史照片为头像: " + selectedPhotoUrl + " ======");
                            }
                        }
                    }
                }
                System.out.println("====== [AI头像初始化任务] 扫描完毕，共初始化了 " + count + " 个小动物的头像。 ======");
            } catch (Exception e) {
                System.err.println("====== [AI头像初始化任务] 执行失败: " + e.getMessage() + " ======");
                e.printStackTrace();
            }
        });
    }
}
