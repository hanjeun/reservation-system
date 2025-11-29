package ac.inhatc.reservation_system.promotion.service;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.entity.Role;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import ac.inhatc.reservation_system.promotion.dto.PromotionDto;
import ac.inhatc.reservation_system.promotion.entity.Promotion;
import ac.inhatc.reservation_system.promotion.repository.PromotionRepository;
import ac.inhatc.reservation_system.store.entity.Store;
import ac.inhatc.reservation_system.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;

    // 전체 홍보글 조회 (정렬 옵션) - 일반 사용자에게는 "추천 가게"로 보임
    public Page<PromotionDto.PromotionResponse> getAllPromotions(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Promotion> promotions;

        switch (sortBy) {
            case "popular":
                promotions = promotionRepository.findAllByOrderByViewCountDesc(pageable);
                break;
            case "likes":
                promotions = promotionRepository.findAllByOrderByLikeCountDesc(pageable);
                break;
            default: // "latest"
                promotions = promotionRepository.findAllByOrderByCreatedAtDesc(pageable);
                break;
        }

        return promotions.map(PromotionDto.PromotionResponse::from);
    }

    // 홍보글 상세 조회 (조회수 증가)
    @Transactional
    public PromotionDto.PromotionResponse getPromotion(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("홍보글을 찾을 수 없습니다."));

        promotion.increaseViewCount();

        return PromotionDto.PromotionResponse.from(promotion);
    }

    // 내가 등록한 가게 목록 조회 (사업자/관리자용)
    public List<PromotionDto.StoreSimpleResponse> getMyStores(Long memberId) {
        List<Store> stores = storeRepository.findByOwnerId(memberId);

        return stores.stream()
                .map(store -> PromotionDto.StoreSimpleResponse.builder()
                        .id(store.getId())
                        .name(store.getName())
                        .category(store.getCategory())
                        .address(store.getAddress())
                        .phone(store.getPhone())
                        .mainImageUrl(store.getMainImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    // 홍보글 작성 (사업자/관리자만 가능)
    @Transactional
    public PromotionDto.PromotionResponse createPromotion(Long memberId, PromotionDto.PromotionRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 사업자 또는 관리자 권한 확인
        if (!member.getRole().equals(Role.BUSINESS) && !member.getRole().equals(Role.ADMIN)) {
            throw new IllegalArgumentException("사업자 또는 관리자만 홍보글을 작성할 수 있습니다.");
        }

        // 가게 확인
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다."));

        // 내 가게인지 확인
        if (!store.getOwner().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인이 등록한 가게만 홍보할 수 있습니다.");
        }

        Promotion promotion = Promotion.builder()
                .member(member)
                .store(store)
                .title(request.getTitle())
                .content(request.getContent())
                .category(Promotion.PromotionCategory.valueOf(request.getCategory()))
                .imageUrl(request.getImageUrl())
                .specialMenu(request.getSpecialMenu())
                .storyHistory(request.getStoryHistory())
                .tags(request.getTags())
                .build();

        Promotion savedPromotion = promotionRepository.save(promotion);
        return PromotionDto.PromotionResponse.from(savedPromotion);
    }

    // 홍보글 수정
    @Transactional
    public PromotionDto.PromotionResponse updatePromotion(Long promotionId, Long memberId, PromotionDto.PromotionRequest request) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("홍보글을 찾을 수 없습니다."));

        if (!promotion.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 홍보글만 수정할 수 있습니다.");
        }

        promotion.setTitle(request.getTitle());
        promotion.setContent(request.getContent());
        promotion.setCategory(Promotion.PromotionCategory.valueOf(request.getCategory()));
        promotion.setImageUrl(request.getImageUrl());
        promotion.setSpecialMenu(request.getSpecialMenu());
        promotion.setStoryHistory(request.getStoryHistory());
        promotion.setTags(request.getTags());

        return PromotionDto.PromotionResponse.from(promotion);
    }

    // 홍보글 삭제
    @Transactional
    public void deletePromotion(Long promotionId, Long memberId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("홍보글을 찾을 수 없습니다."));

        if (!promotion.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 홍보글만 삭제할 수 있습니다.");
        }

        promotionRepository.delete(promotion);
    }

    // 내가 작성한 홍보글 조회
    public Page<PromotionDto.PromotionResponse> getMyPromotions(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Promotion> promotions = promotionRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        return promotions.map(PromotionDto.PromotionResponse::from);
    }
}
