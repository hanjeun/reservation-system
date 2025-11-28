package ac.inhatc.reservation_system.store.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StoreCreateRequest {
    
    private String name;
    private String description;
    private String address;
    private String phone;
    private String category;
    
    // 키워드 리스트
    private List<String> keywords = new ArrayList<>();
    
    // 영업 시간
    private LocalTime openTime;
    private LocalTime closeTime;

    // 메인 이미지 파일
    private MultipartFile mainImage;
    
    // 상세 이미지 파일들
    private List<MultipartFile> detailImages = new ArrayList<>();
}
