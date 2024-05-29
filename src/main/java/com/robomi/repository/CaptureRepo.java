package com.robomi.repository;

import com.robomi.entity.CaptureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaptureRepo extends JpaRepository<CaptureEntity, Long> {
    @Query("SELECT c FROM CaptureEntity c WHERE c.status <> 0")
    List<CaptureEntity> findWarningCaptures();

    @Query("SELECT c FROM CaptureEntity c WHERE c.name = :name")
    List<CaptureEntity> findCapturesByName(@Param("name") String name);
}
