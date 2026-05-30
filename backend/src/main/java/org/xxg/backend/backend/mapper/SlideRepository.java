package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.Slide;
import java.util.List;

public interface SlideRepository extends JpaRepository<Slide, Integer> {
    List<Slide> findByStatusTrueOrderBySortOrderAsc();
}
