package ac.inhatc.reservation_system.member.service;


import ac.inhatc.reservation_system.business.repository.BusinessVerificationRepository;
import ac.inhatc.reservation_system.community.repository.CommunityCommentRepository;
import ac.inhatc.reservation_system.community.repository.CommunityPostRepository;
import ac.inhatc.reservation_system.community.repository.PostLikeRepository;
import ac.inhatc.reservation_system.config.jwt.entity.RefreshToken;
import ac.inhatc.reservation_system.config.jwt.repository.RefreshTokenRepository;
import ac.inhatc.reservation_system.config.oauth2.OAuthUnlinkService;
import ac.inhatc.reservation_system.customerservice.repository.InquiryRepository;
import ac.inhatc.reservation_system.customerservice.repository.NoticeRepository;
import ac.inhatc.reservation_system.email.service.EmailVerificationService;
import ac.inhatc.reservation_system.favorite.repository.FavoriteRepository;
import ac.inhatc.reservation_system.member.dto.MemberDto;
import ac.inhatc.reservation_system.member.dto.MemberResponse;
import ac.inhatc.reservation_system.member.dto.MemberUpdateRequest;
import ac.inhatc.reservation_system.member.entity.AuthProvider;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.entity.Role;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import ac.inhatc.reservation_system.promotion.repository.PromotionRepository;
import ac.inhatc.reservation_system.reservation.repository.ReservationRepository;
import ac.inhatc.reservation_system.review.repository.ReviewRepository;
import ac.inhatc.reservation_system.store.entity.Store;
import ac.inhatc.reservation_system.store.repository.StoreRepository;
import ac.inhatc.reservation_system.store.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder bCryptPasswordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final OAuthUnlinkService oAuthUnlinkService;  // OAuth 연동 해제 서비스

    // 회원 삭제 시 필요한 Repository들
    private final StoreRepository storeRepository;
    private final ReservationRepository reservationRepository;
    private final FavoriteRepository favoriteRepository;
    private final PromotionRepository promotionRepository;
    private final ReviewRepository reviewRepository;
    private final InquiryRepository inquiryRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final PostLikeRepository postLikeRepository;
    private final NoticeRepository noticeRepository;
    private final FileStorageService fileStorageService;
    private final BusinessVerificationRepository businessVerificationRepository;

    public Long save(MemberDto memberDto) {
        // 이미 가입된 이메일인지 확인
        if (memberRepository.findByEmail(memberDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 이메일 인증 여부 확인
        if (!emailVerificationService.isEmailVerified(memberDto.getEmail())) {
            throw new IllegalArgumentException("이메일 인증이 필요합니다.");
        }

        return memberRepository.save(Member.builder()
                .name(memberDto.getName())
                .email(memberDto.getEmail())
                .password(bCryptPasswordEncoder.encode(memberDto.getPassword()))
                .role(memberDto.getRole()) // 권한 추가
                .provider(AuthProvider.LOCAL)  // 일반 회원가입은 LOCAL
                .build()).getId();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
    }

    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
    }

    @Transactional
    public MemberResponse updateMember(Long memberId, MemberUpdateRequest request) {
        log.info("✏️ 회원 정보 수정: memberId={}", memberId);

        Member member = findById(memberId);

        // 이름 수정
        if (request.getName() != null && !request.getName().isEmpty()) {
            member.setName(request.getName());
        }

        // 이메일 수정 (중복 체크)
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!member.getEmail().equals(request.getEmail())) {
                if (memberRepository.findByEmail(request.getEmail()).isPresent()) {
                    throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                }
                member.setEmail(request.getEmail());
            }
        }

        // 비밀번호 수정 (입력된 경우에만, OAuth 사용자가 아닌 경우만)
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            // OAuth 사용자는 비밀번호 변경 불가
            if (member.isOAuthUser()) {
                throw new IllegalArgumentException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
            }
            if (request.getPassword().length() < 8) {
                throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
            }
            if (!request.getPassword().equals(request.getPasswordConfirm())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
            member.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));
        }
        
        // 권한 수정
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                Role newRole = Role.valueOf(request.getRole().toUpperCase());
                member.setRole(newRole);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 권한입니다: " + request.getRole());
            }
        }
        
        Member updated = memberRepository.save(member);
        log.info("✅ 회원 정보 수정 완료: memberId={}", memberId);
        
        return MemberResponse.from(updated);
    }

    @Transactional
    public void deleteMember(Long memberId) {
        log.info("🗑️ 회원 삭제 시작: memberId={}", memberId);

        // 0. 회원 조회
        Member member = findById(memberId);

        // 0-1. OAuth 연동 해제 (소셜 로그인 사용자인 경우)
        if (member.isOAuthUser()) {
            log.info("🔓 OAuth 연동 해제 시도: provider={}", member.getProvider());
            boolean unlinkResult = oAuthUnlinkService.unlinkOAuth(member);
            if (unlinkResult) {
                log.info("✅ OAuth 연동 해제 성공");
            } else {
                log.warn("⚠️ OAuth 연동 해제 실패 (탈퇴는 계속 진행)");
            }
        }

        // 1. 회원이 소유한 가게들 조회
        List<Store> ownedStores = storeRepository.findByOwnerId(memberId);
        
        // 2. 각 가게의 관련 데이터 삭제
        for (Store store : ownedStores) {
            Long storeId = store.getId();
            
            // 가게의 리뷰 삭제
            reviewRepository.deleteByStoreId(storeId);
            
            // 가게의 예약 삭제
            reservationRepository.deleteByStoreId(storeId);
            
            // 가게의 찜 삭제
            favoriteRepository.deleteByStoreId(storeId);
            
            // 가게의 홍보글 삭제
            promotionRepository.deleteByStoreId(storeId);
            
            // 가게 이미지 파일 삭제
            if (store.getMainImageUrl() != null) {
                fileStorageService.deleteFile(store.getMainImageUrl());
            }
            store.getDetailImageList().forEach(fileStorageService::deleteFile);
            
            log.info("  - 가게 관련 데이터 삭제 완료: storeId={}", storeId);
        }
        
        // 3. 회원이 소유한 가게 삭제
        storeRepository.deleteAll(ownedStores);
        log.info("  - 소유 가게 삭제 완료: count={}", ownedStores.size());

        // 4. 회원의 리뷰 삭제
        reviewRepository.deleteByMemberId(memberId);
        log.info("  - 회원 리뷰 삭제 완료");

        // 5. 회원의 예약 삭제
        reservationRepository.deleteByMemberId(memberId);
        log.info("  - 회원 예약 삭제 완료");

        // 6. 회원의 찜 삭제
        favoriteRepository.deleteByMemberId(memberId);
        log.info("  - 회원 찜 삭제 완료");

        // 7. 회원의 홍보글 삭제
        promotionRepository.deleteByMemberId(memberId);
        log.info("  - 회원 홍보글 삭제 완료");

        // 8. 회원의 문의 삭제
        inquiryRepository.deleteByMemberId(memberId);
        log.info("  - 회원 문의 삭제 완료");

        // 9. 커뮤니티 관련 삭제 (순서 중요: 좋아요/댓글 -> 게시글)
        // 9-1. 회원이 작성한 게시글 ID 조회
        List<Long> postIds = communityPostRepository.findPostIdsByAuthorId(memberId);
        
        if (!postIds.isEmpty()) {
            // 9-2. 해당 게시글들의 좋아요 삭제
            postLikeRepository.deleteByPostIds(postIds);
            
            // 9-3. 해당 게시글들의 댓글 삭제
            communityCommentRepository.deleteByPostIds(postIds);
        }
        
        // 9-4. 회원이 누른 좋아요 삭제
        postLikeRepository.deleteByMemberId(memberId);
        
        // 9-5. 회원의 댓글 삭제
        communityCommentRepository.deleteByAuthorId(memberId);
        
        // 9-6. 회원의 게시글 삭제
        communityPostRepository.deleteByAuthorId(memberId);
        log.info("  - 커뮤니티 데이터 삭제 완료");

        // 10. 공지사항 삭제 (관리자인 경우)
        noticeRepository.deleteByAuthorId(memberId);
        log.info("  - 공지사항 삭제 완료");

        // 11. 사업자 인증 요청 삭제
        businessVerificationRepository.deleteByMemberId(memberId);
        log.info("  - 사업자 인증 요청 삭제 완료");

        // 12. 리프레쉬 토큰 삭제
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(memberId);
        refreshTokenRepository.deleteAll(tokens);
        log.info("  - Refresh Token 삭제 완료: count={}", tokens.size());

        // 13. 회원 삭제
        memberRepository.deleteById(memberId);
        log.info("✅ 회원 삭제 완료: memberId={}", memberId);
    }
}
