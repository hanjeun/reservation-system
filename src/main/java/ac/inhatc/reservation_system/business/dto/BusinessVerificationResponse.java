package ac.inhatc.reservation_system.business.dto;

import ac.inhatc.reservation_system.business.entity.BusinessVerification;
import ac.inhatc.reservation_system.business.entity.BusinessVerification.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessVerificationResponse {

    private Long id;
    private Long memberId;
    private String memberName;
    private String memberEmail;
    private String licenseImageUrl;
    private String businessName;
    private String businessNumber;
    private String memo;
    private VerificationStatus status;
    private String statusDisplayName;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String processedByName;

    public static BusinessVerificationResponse from(BusinessVerification verification) {
        return BusinessVerificationResponse.builder()
                .id(verification.getId())
                .memberId(verification.getMember().getId())
                .memberName(verification.getMember().getName())
                .memberEmail(verification.getMember().getEmail())
                .licenseImageUrl(verification.getLicenseImageUrl())
                .businessName(verification.getBusinessName())
                .businessNumber(verification.getBusinessNumber())
                .memo(verification.getMemo())
                .status(verification.getStatus())
                .statusDisplayName(verification.getStatus().getDisplayName())
                .rejectionReason(verification.getRejectionReason())
                .createdAt(verification.getCreatedAt())
                .processedAt(verification.getProcessedAt())
                .processedByName(verification.getProcessedBy() != null ? 
                        verification.getProcessedBy().getName() : null)
                .build();
    }
}
