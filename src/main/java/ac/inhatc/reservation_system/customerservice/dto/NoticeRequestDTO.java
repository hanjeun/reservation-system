package ac.inhatc.reservation_system.customerservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeRequestDTO {
    
    private String title;
    private String content;
    private Boolean isImportant;
}
