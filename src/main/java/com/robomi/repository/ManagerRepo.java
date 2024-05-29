package com.robomi.repository;

import com.robomi.entity.ManagerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerRepo extends JpaRepository<ManagerEntity, Long> {
    @Query("SELECT m FROM ManagerEntity m WHERE m.type = :type")
    List<ManagerEntity> findManagerByType(@Param("type") Long type);
}
