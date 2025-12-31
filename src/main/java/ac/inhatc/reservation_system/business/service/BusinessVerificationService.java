package ac.inhatc.reservation_system.business.service;

import ac.inhatc.reservation_system.business.dto.BusinessVerificationRequest;
import ac.inhatc.reservation_system.business.dto.BusinessVerificationResponse;
import ac.inhatc.reservation_system.business.entity.BusinessVerification;
import ac.inhatc.reservation_system.business.entity.BusinessVerification.VerificationStatus;
import ac.inhatc.reservation_system.business.repository.BusinessVerificationRepository;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.entity.Role;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import ac.inhatc.reservation_system.store.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessVerificationService {

    private final BusinessVerificationRepository verificationRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;

    /**
     * 사업자 인증 신청
     */
    @Transactional
    public BusinessVerificationResponse submitVerification(Member member, BusinessVerificationRequest request) {
        // 이미 대기중인 요청이 있는지 확인
        if (verificationRepository.existsByMemberAndStatus(member, VerificationStatus.PENDING)) {
            throw new IllegalStateException("이미 대기 중인 사업자 인증 요청이 있습니다.");
        }

        // 이미 사업자인 경우
        if (member.getRole() == Role.BUSINESS) {
            throw new IllegalStateException("이미 사업자로 등록되어 있습니다.");
        }

        // 파일 유효성 검사
        if (request.getLicenseImage() == null || request.getLicenseImage().isEmpty()) {
            throw new IllegalArgumentException("사업자 등록증 이미지를 업로드해주세요.");
        }

        if (request.getBusinessName() == null || request.getBusinessName().trim().isEmpty()) {
            throw new IllegalArgumentException("상호명을 입력해주세요.");
        }

        // 이미지 저장
        String imageUrl = fileStorageService.storeFile(request.getLicenseImage());

        // 인증 요청 생성
        BusinessVerification verification = BusinessVerification.builder()
                .member(member)
                .licenseImageUrl(imageUrl)
                .businessName(request.getBusinessName().trim())
                .businessNumber(request.getBusinessNumber())
                .memo(request.getMemo())
                .status(VerificationStatus.PENDING)
                .build();

        BusinessVerification saved = verificationRepository.save(verification);
        log.info("📝 사업자 인증 신청: memberId={}, verificationId={}", member.getId(), saved.getId());

        return BusinessVerificationResponse.from(saved);
    }

    /**
     * 내 인증 상태 조회
     */
    @Transactional(readOnly = true)
    public Optional<BusinessVerificationResponse> getMyVerificationStatus(Member member) {
        return verificationRepository.findTopByMemberOrderByCreatedAtDesc(member)
                .map(BusinessVerificationResponse::from);
    }

    /**
     * 대기중인 인증 요청 목록 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<BusinessVerificationResponse> getPendingVerifications(Pageable pageable) {
        return verificationRepository.findByStatusOrderByCreatedAtDesc(VerificationStatus.PENDING, pageable)
                .map(BusinessVerificationResponse::from);
    }

    /**
     * 전체 인증 요청 목록 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<BusinessVerificationResponse> getAllVerifications(Pageable pageable) {
        return verificationRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(BusinessVerificationResponse::from);
    }

    /**
     * 상태별 인증 요청 목록 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<BusinessVerificationResponse> getVerificationsByStatus(VerificationStatus status, Pageable pageable) {
        return verificationRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(BusinessVerificationResponse::from);
    }

    /**
     * 인증 요청 상세 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public BusinessVerificationResponse getVerificationDetail(Long id) {
        BusinessVerification verification = verificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 찾을 수 없습니다."));
        return BusinessVerificationResponse.from(verification);
    }

    /**
     * 사업자 인증 승인 (관리자용)
     */
    @Transactional
    public BusinessVerificationResponse approveVerification(Long verificationId, Member admin) {
        // 관리자 권한 확인
        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException("관리자만 승인할 수 있습니다.");
        }

        BusinessVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 찾을 수 없습니다."));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        // 인증 승인
        verification.approve(admin);

        // 회원 권한을 BUSINESS로 변경
        Member member = verification.getMember();
        member.setRole(Role.BUSINESS);
        memberRepository.save(member);

        BusinessVerification saved = verificationRepository.save(verification);
        log.info("✅ 사업자 인증 승인: verificationId={}, memberId={}, adminId={}", 
                verificationId, member.getId(), admin.getId());

        return BusinessVerificationResponse.from(saved);
    }

    /**
     * 사업자 인증 거절 (관리자용)
     */
    @Transactional
    public BusinessVerificationResponse rejectVerification(Long verificationId, Member admin, String reason) {
        // 관리자 권한 확인
        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException("관리자만 거절할 수 있습니다.");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("거절 사유를 입력해주세요.");
        }

        BusinessVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 찾을 수 없습니다."));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        // 인증 거절
        verification.reject(admin, reason.trim());

        BusinessVerification saved = verificationRepository.save(verification);
        log.info("❌ 사업자 인증 거절: verificationId={}, memberId={}, adminId={}, reason={}", 
                verificationId, verification.getMember().getId(), admin.getId(), reason);

        return BusinessVerificationResponse.from(saved);
    }

    /**
     * 대기중인 인증 요청 수 (관리자용)
     */
    @Transactional(readOnly = true)
    public long getPendingCount() {
        return verificationRepository.countByStatus(VerificationStatus.PENDING);
    }

    /**
     * 사업자 인증 신청 취소 (사용자)
     */
    @Transactional
    public void cancelVerification(Member member) {
        BusinessVerification verification = verificationRepository.findTopByMemberOrderByCreatedAtDesc(member)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 찾을 수 없습니다."));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 요청만 취소할 수 있습니다.");
        }

        verificationRepository.delete(verification);
        log.info("🗑️ 사업자 인증 신청 취소: memberId={}, verificationId={}", member.getId(), verification.getId());
    }

    /**
     * 사업자 자격 포기 (사업자 본인)
     */
    @Transactional
    public void resignBusinessRole(Member member) {
        if (member.getRole() != Role.BUSINESS) {
            throw new IllegalStateException("사업자만 자격을 포기할 수 있습니다.");
        }

        // 회원 권한을 USER로 변경
        member.setRole(Role.USER);
        memberRepository.save(member);

        log.info("📝 사업자 자격 포기: memberId={}", member.getId());
    }

    /**
     * 사업자 자격 취소 (관리자)
     */
    @Transactional
    public void revokeBusinessRole(Long memberId, Member admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException("관리자만 사업자 자격을 취소할 수 있습니다.");
        }

        Member targetMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (targetMember.getRole() != Role.BUSINESS) {
            throw new IllegalStateException("해당 회원은 사업자가 아닙니다.");
        }

        // 회원 권한을 USER로 변경
        targetMember.setRole(Role.USER);
        memberRepository.save(targetMember);

        // 해당 회원의 승인된 인증을 거절 상태로 변경
        verificationRepository.findTopByMemberAndStatusOrderByCreatedAtDesc(targetMember, VerificationStatus.APPROVED)
                .ifPresent(v -> {
                    v.reject(admin, "관리자에 의해 사업자 자격이 취소되었습니다.");
                    verificationRepository.save(v);
                });

        log.info("❌ 사업자 자격 취소 (관리자): memberId={}, adminId={}", memberId, admin.getId());
    }
}
