package com.pawtrack;

import com.pawtrack.entity.Animal;
import com.pawtrack.repository.AnimalRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PawTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(PawTrackApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(AnimalRepository animalRepository, com.pawtrack.repository.UserRepository userRepository) {
        return args -> {
            if (animalRepository.count() == 0) {
                Animal animal = new Animal();
                animal.setName("大橘");
                animal.setBreed("Cat");
                animalRepository.save(animal);
                System.out.println("====== 测试数据 '大橘' 初始化成功！ ======");
            }
            if (userRepository.findByUsername("admin").isEmpty()) {
                com.pawtrack.entity.User admin = new com.pawtrack.entity.User();
                admin.setUsername("admin");
                admin.setNickname("超级管理员");
                admin.setRole("ADMIN");
                String salt = com.pawtrack.util.PasswordUtil.generateSalt();
                admin.setSalt(salt);
                admin.setPassword(com.pawtrack.util.PasswordUtil.hashPassword("admin123", salt));
                userRepository.save(admin);
                System.out.println("====== 测试数据 '超级管理员(admin/admin123)' 初始化成功！ ======");
            }
        };
    }
}
