package ac.inhatc.reservation_system.customerservice.repository;

import ac.inhatc.reservation_system.customerservice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    
    // 중요 공지 먼저, 그 다음 최신순 정렬
    @Query("SELECT n FROM Notice n ORDER BY n.isImportant DESC, n.createdAt DESC")
    List<Notice> findAllOrderByImportantAndCreatedAt();
    
    // 중요 공지만 조회
    List<Notice> findByIsImportantTrueOrderByCreatedAtDesc();
}
