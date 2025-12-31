package ac.inhatc.reservation_system.store.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoreUpdateRequest {
    
    private String name;
    private String description;
    private String address;
    private String phone;
    private String category;
    
    // 키워드 (콤마 구분 문자열 또는 리스트)
    private List<String> keywords;
    
    // 새 이미지 업로드 (선택적)
    private MultipartFile mainImage;
    private List<MultipartFile> detailImages;

    // 기존 이미지 URL 유지
    private String existingMainImageUrl;
    private List<String> existingDetailImageUrls;

    // 노쇼 방지금 (0원이면 무료)
    private Integer noShowDeposit;

    // ========== 환불 정책 ==========
    // 전액 환불 가능 일수 (예약일 N일 전까지 전액 환불)
    private Integer fullRefundDays;

    // 부분 환불 가능 일수 (예약일 N일 전까지 부분 환불)
    private Integer partialRefundDays;

    // 부분 환불 비율 (퍼센트, 예: 50 = 50%)
    private Integer partialRefundRate;
}
