package com.pawtrack.repository;

import com.pawtrack.entity.LocationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationLogRepository extends JpaRepository<LocationLog, Long> {

    // Native MySQL query to calculate distance and find logs within radius (in meters) using the spatial location index
    @Query(value = "SELECT * FROM animal_logs l WHERE ST_Distance_Sphere(l.location, ST_GeomFromText(:wkt, 4326)) <= :radius", nativeQuery = true)
    List<LocationLog> findLogsWithinRadius(@Param("wkt") String wkt, @Param("radius") Double radius);
    
    // Find recent logs for an animal
    List<LocationLog> findByAnimalIdOrderByRecordedAtDesc(Long animalId);
}
