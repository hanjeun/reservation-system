package ac.inhatc.reservation_system.store.dto;

import ac.inhatc.reservation_system.store.entity.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponse {
    
    private Long id;
    private String name;
    private String description;
    private String address;
    private String phone;
    private String category;
    private String mainImageUrl;
    private List<String> detailImages;
    private List<String> keywords;
    private LocalTime openTime;
    private LocalTime closeTime;
    private Double rating;
    private Integer reviewCount;
    private Integer noShowDeposit;  // 노쇼 방지금
    
    // ========== 환불 정책 ==========
    private Integer fullRefundDays;     // 전액 환불 가능 일수
    private Integer partialRefundDays;  // 부분 환불 가능 일수
    private Integer partialRefundRate;  // 부분 환불 비율 (%)
    
    private LocalDateTime createdAt;

    public static StoreResponse from(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .description(store.getDescription())
                .address(store.getAddress())
                .phone(store.getPhone())
                .category(store.getCategory())
                .mainImageUrl(store.getMainImageUrl())
                .detailImages(store.getDetailImageList())
                .keywords(store.getKeywordList())
                .openTime(store.getOpenTime())
                .closeTime(store.getCloseTime())
                .rating(store.getRating())
                .reviewCount(store.getReviewCount())
                .noShowDeposit(store.getNoShowDeposit())
                .fullRefundDays(store.getFullRefundDays())
                .partialRefundDays(store.getPartialRefundDays())
                .partialRefundRate(store.getPartialRefundRate())
                .createdAt(store.getCreatedAt())
                .build();
    }
}
