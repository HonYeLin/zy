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
    public CommandLineRunner initData(AnimalRepository animalRepository) {
        return args -> {
            if (animalRepository.count() == 0) {
                Animal animal = new Animal();
                animal.setName("大橘");
                animal.setBreed("Cat");
                animalRepository.save(animal);
                System.out.println("====== 测试数据 '大橘' 初始化成功！ ======");
            }
        };
    }
}
