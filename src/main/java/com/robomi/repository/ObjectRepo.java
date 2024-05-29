package com.robomi.repository;

import com.robomi.entity.ObjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObjectRepo extends JpaRepository<ObjectEntity, Long> {
    @Query("SELECT o FROM ObjectEntity o WHERE o.display = :display")
    List<ObjectEntity> findByObjectByDisplay(@Param("display") Long display);
}
