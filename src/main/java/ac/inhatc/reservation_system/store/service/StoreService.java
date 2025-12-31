package ac.inhatc.reservation_system.store.service;

import ac.inhatc.reservation_system.favorite.repository.FavoriteRepository;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.promotion.repository.PromotionRepository;
import ac.inhatc.reservation_system.reservation.repository.ReservationRepository;
import ac.inhatc.reservation_system.review.repository.ReviewRepository;
import ac.inhatc.reservation_system.store.repository.StoreRepository;
import ac.inhatc.reservation_system.store.dto.StoreCreateRequest;
import ac.inhatc.reservation_system.store.dto.StoreResponse;
import ac.inhatc.reservation_system.store.dto.StoreUpdateRequest;
import ac.inhatc.reservation_system.store.entity.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class StoreService {

    private final StoreRepository storeRepository;
    private final FileStorageService fileStorageService;
    private final ReservationRepository reservationRepository;
    private final FavoriteRepository favoriteRepository;
    private final PromotionRepository promotionRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 가게 등록
     */
    @Transactional
    public StoreResponse createStore(StoreCreateRequest request, Member owner) {
        if (owner == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("가게 이름은 필수입니다.");
        }

        // 메인 이미지 저장
        String mainImageUrl = null;
        if (request.getMainImage() != null && !request.getMainImage().isEmpty()) {
            mainImageUrl = fileStorageService.storeFile(request.getMainImage());
        }

        // 상세 이미지들 저장
        List<String> detailImageUrls = new ArrayList<>();
        if (request.getDetailImages() != null && !request.getDetailImages().isEmpty()) {
            for (MultipartFile file : request.getDetailImages()) {
                if (file != null && !file.isEmpty()) {
                    detailImageUrls.add(fileStorageService.storeFile(file));
                }
            }
        }

        Store store = Store.builder()
                .owner(owner)
                .name(request.getName().trim())
                .description(request.getDescription())
                .address(request.getAddress())
                .phone(request.getPhone())
                .category(request.getCategory())
                .mainImageUrl(mainImageUrl)
                .rating(0.0)
                .reviewCount(0)
                .noShowDeposit(request.getNoShowDeposit() != null ? request.getNoShowDeposit() : 0)
                .fullRefundDays(request.getFullRefundDays() != null ? request.getFullRefundDays() : 3)
                .partialRefundDays(request.getPartialRefundDays() != null ? request.getPartialRefundDays() : 1)
                .partialRefundRate(request.getPartialRefundRate() != null ? request.getPartialRefundRate() : 50)
                .build();

        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            store.setKeywordList(request.getKeywords());
        }

        if (!detailImageUrls.isEmpty()) {
            store.setDetailImageList(detailImageUrls);
        }

        if (request.getOpenTime() != null && request.getCloseTime() != null) {
            store.setOpenTime(request.getOpenTime());
            store.setCloseTime(request.getCloseTime());
        }

        Store savedStore = storeRepository.save(store);
        log.info("가게 등록 완료: ID={}", savedStore.getId());

        return StoreResponse.from(savedStore);
    }

    /**
     * 내가 등록한 가게 목록 조회
     */
    @Transactional(readOnly = true)
    public List<StoreResponse> getMyStores(Member member) {
        List<Store> stores = storeRepository.findByOwnerOrderByCreatedAtDesc(member);
        return stores.stream()
                .map(StoreResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 가게 상세 조회
     */
    @Transactional(readOnly = true)
    public StoreResponse getStore(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다."));
        return StoreResponse.from(store);
    }

    /**
     * 가게 수정
     */
    @Transactional
    public StoreResponse updateStore(Long id, StoreUpdateRequest request, Member member) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다."));
        
        if (store.getOwner() != null && !store.getOwner().getId().equals(member.getId())) {
            throw new IllegalArgumentException("가게를 수정할 권한이 없습니다.");
        }
        
        if (request.getName() != null) store.setName(request.getName());
        if (request.getDescription() != null) store.setDescription(request.getDescription());
        if (request.getAddress() != null) store.setAddress(request.getAddress());
        if (request.getPhone() != null) store.setPhone(request.getPhone());
        if (request.getCategory() != null) store.setCategory(request.getCategory());
        if (request.getNoShowDeposit() != null) store.setNoShowDeposit(request.getNoShowDeposit());
        if (request.getFullRefundDays() != null) store.setFullRefundDays(request.getFullRefundDays());
        if (request.getPartialRefundDays() != null) store.setPartialRefundDays(request.getPartialRefundDays());
        if (request.getPartialRefundRate() != null) store.setPartialRefundRate(request.getPartialRefundRate());
        
        if (request.getKeywords() != null) {
            store.setKeywordList(request.getKeywords());
        }
        
        // 메인 이미지 처리
        if (request.getMainImage() != null && !request.getMainImage().isEmpty()) {
            // 새 이미지가 업로드된 경우: 기존 이미지 삭제 후 새 이미지 저장
            if (store.getMainImageUrl() != null) {
                fileStorageService.deleteFile(store.getMainImageUrl());
            }
            store.setMainImageUrl(fileStorageService.storeFile(request.getMainImage()));
        } else if (request.getExistingMainImageUrl() != null && !request.getExistingMainImageUrl().isEmpty()) {
            // 기존 이미지 URL 유지
            store.setMainImageUrl(request.getExistingMainImageUrl());
        }
        // 둘 다 없으면 기존 이미지 그대로 유지 (아무 작업 안 함)

        // 상세 이미지 처리
        List<String> finalDetailImages = new ArrayList<>();

        // 1. 기존에 유지할 이미지들 추가
        if (request.getExistingDetailImageUrls() != null && !request.getExistingDetailImageUrls().isEmpty()) {
            finalDetailImages.addAll(request.getExistingDetailImageUrls());
        }

        // 2. 새로 업로드된 이미지들 추가
        if (request.getDetailImages() != null && !request.getDetailImages().isEmpty()) {
            for (MultipartFile file : request.getDetailImages()) {
                if (file != null && !file.isEmpty()) {
                    finalDetailImages.add(fileStorageService.storeFile(file));
                }
            }
        }

        // 3. 삭제된 기존 이미지들 파일 삭제 (기존 목록에 있었으나 새 목록에 없는 것들)
        List<String> currentDetailImages = store.getDetailImageList();
        if (currentDetailImages != null) {
            for (String existingUrl : currentDetailImages) {
                if (!finalDetailImages.contains(existingUrl)) {
                    fileStorageService.deleteFile(existingUrl);
                }
            }
        }

        // 4. 최종 상세 이미지 목록 설정
        store.setDetailImageList(finalDetailImages);
        
        return StoreResponse.from(storeRepository.save(store));
    }

    /**
     * 가게 삭제
     */
    @Transactional
    public void deleteStore(Long id, Member member) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다."));

        if (store.getOwner() != null && !store.getOwner().getId().equals(member.getId())) {
            throw new IllegalArgumentException("가게를 삭제할 권한이 없습니다.");
        }

        // 1. 관련 데이터 먼저 삭제 (외래 키 제약 조건 해결)
        log.info("🗑️ 가게 삭제 시작: storeId={}", id);
        
        // 리뷰 삭제
        reviewRepository.deleteByStoreId(id);
        log.info("  - 리뷰 삭제 완료");
        
        // 예약 삭제
        reservationRepository.deleteByStoreId(id);
        log.info("  - 예약 삭제 완료");
        
        // 찜 삭제
        favoriteRepository.deleteByStoreId(id);
        log.info("  - 찜 삭제 완료");
        
        // 홍보글 삭제
        promotionRepository.deleteByStoreId(id);
        log.info("  - 홍보글 삭제 완료");

        // 2. 이미지 파일 삭제
        if (store.getMainImageUrl() != null) {
            fileStorageService.deleteFile(store.getMainImageUrl());
        }
        store.getDetailImageList().forEach(fileStorageService::deleteFile);
        log.info("  - 이미지 파일 삭제 완료");

        // 3. 가게 삭제
        storeRepository.delete(store);
        log.info("✅ 가게 삭제 완료: storeId={}", id);
    }

    /**
     * 키워드로 가게 검색
     */
    @Transactional(readOnly = true)
    public List<StoreResponse> searchStores(String keyword, String sort) {
        List<Store> stores;

        if (keyword == null || keyword.trim().isEmpty()) {
            stores = getAllStoresSorted(sort);
        } else {
            stores = storeRepository.searchStores(keyword.trim());
            stores = sortStores(stores, sort);
        }

        return stores.stream()
                .map(StoreResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 전체 가게 조회 (정렬 적용)
     */
    private List<Store> getAllStoresSorted(String sort) {
        if (sort == null) sort = "rating";
        
        return switch (sort) {
            case "recent" -> storeRepository.findAllByOrderByCreatedAtDesc();
            case "reviews" -> storeRepository.findAllByOrderByReviewCountDesc();
            default -> storeRepository.findAllByOrderByRatingDesc();
        };
    }

    /**
     * 가게 리스트 정렬
     */
    private List<Store> sortStores(List<Store> stores, String sort) {
        if (sort == null) sort = "rating";
        
        return switch (sort) {
            case "recent" -> stores.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
            case "reviews" -> stores.stream()
                    .sorted((a, b) -> Integer.compare(b.getReviewCount(), a.getReviewCount()))
                    .collect(Collectors.toList());
            default -> stores.stream()
                    .sorted((a, b) -> Double.compare(
                            b.getRating() != null ? b.getRating() : 0.0,
                            a.getRating() != null ? a.getRating() : 0.0))
                    .collect(Collectors.toList());
        };
    }
}
