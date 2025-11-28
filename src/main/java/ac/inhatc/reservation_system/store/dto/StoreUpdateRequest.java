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
    
    // 이미지 업데이트 (선택적)
    private MultipartFile mainImage;
    private List<MultipartFile> detailImages;
    
    // 기존 이미지 URL 유지 여부
    private Boolean keepMainImage;
    private List<String> keepDetailImages;
}
