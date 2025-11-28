package ac.inhatc.reservation_system.store.entity;

import ac.inhatc.reservation_system.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "store")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    // 가게 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Member owner;

    @Column(name = "store_name", nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "address")
    private String address;

    @Column(name = "phone")
    private String phone;

    @Column(name = "category")
    private String category;

    @Column(name = "main_image_url")
    private String mainImageUrl;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    // 키워드 (콤마로 구분된 문자열로 저장)
    @Column(name = "keywords", length = 500)
    private String keywords;

    // 상세 이미지 URL들 (콤마로 구분된 문자열로 저장)
    @Column(name = "detail_images", length = 2000)
    private String detailImages;

    // 영업 시간
    @Column(name = "open_time")
    private LocalTime openTime;
    
    @Column(name = "close_time")
    private LocalTime closeTime;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 키워드 편의 메서드
    public List<String> getKeywordList() {
        if (keywords == null || keywords.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(keywords.split(","));
    }

    public void setKeywordList(List<String> keywordList) {
        if (keywordList == null || keywordList.isEmpty()) {
            this.keywords = "";
        } else {
            this.keywords = String.join(",", keywordList);
        }
    }

    // 상세 이미지 편의 메서드
    public List<String> getDetailImageList() {
        if (detailImages == null || detailImages.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(detailImages.split(","));
    }

    public void setDetailImageList(List<String> imageList) {
        if (imageList == null || imageList.isEmpty()) {
            this.detailImages = "";
        } else {
            this.detailImages = String.join(",", imageList);
        }
    }
}
