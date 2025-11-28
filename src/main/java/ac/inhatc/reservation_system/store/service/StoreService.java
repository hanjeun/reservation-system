package ac.inhatc.reservation_system.store.service;

import ac.inhatc.reservation_system.member.entity.Member;
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

    /**
     * 가게 등록
     */
    @Transactional
    public StoreResponse createStore(StoreCreateRequest request, Member owner) {
        log.info("🏪 가게 등록 시작");
        log.info("  - 가게 이름: {}", request.getName());
        log.info("  - 소유자: {}", owner != null ? owner.getName() : "NULL");
        log.info("  - 카테고리: {}", request.getCategory());
        log.info("  - 주소: {}", request.getAddress());
        log.info("  - 전화번호: {}", request.getPhone());
        log.info("  - 설명: {}", request.getDescription());
        log.info("  - 키워드 개수: {}", request.getKeywords() != null ? request.getKeywords().size() : 0);
        log.info("  - 메인 이미지: {}", request.getMainImage() != null ? request.getMainImage().getOriginalFilename() : "없음");

        // Owner null 체크
        if (owner == null) {
            log.error("❌ 소유자 정보가 없습니다. 로그인이 필요합니다.");
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        // 필수 필드 검증
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("가게 이름은 필수입니다.");
        }

        // 메인 이미지 저장
        String mainImageUrl = null;
        if (request.getMainImage() != null && !request.getMainImage().isEmpty()) {
            try {
                mainImageUrl = fileStorageService.storeFile(request.getMainImage());
                log.info("📷 메인 이미지 저장 성공: {}", mainImageUrl);
            } catch (Exception e) {
                log.error("❌ 메인 이미지 저장 실패", e);
                throw new RuntimeException("이미지 저장에 실패했습니다: " + e.getMessage());
            }
        }

        // 상세 이미지들 저장
        List<String> detailImageUrls = new ArrayList<>();
        if (request.getDetailImages() != null && !request.getDetailImages().isEmpty()) {
            for (MultipartFile file : request.getDetailImages()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String url = fileStorageService.storeFile(file);
                        detailImageUrls.add(url);
                        log.info("📷 상세 이미지 저장 성공: {}", url);
                    } catch (Exception e) {
                        log.error("❌ 상세 이미지 저장 실패", e);
                    }
                }
            }
        }

        // Store 엔티티 생성
        try {
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
                    .build();

            log.info("✅ Store 엔티티 생성 완료");

            // 키워드 설정
            if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
                store.setKeywordList(request.getKeywords());
                log.info("✅ 키워드 설정 완료: {}", request.getKeywords());
            }
            
            // 상세 이미지 설정
            if (!detailImageUrls.isEmpty()) {
                store.setDetailImageList(detailImageUrls);
                log.info("✅ 상세 이미지 설정 완료: {}개", detailImageUrls.size());
            }

            // 영업 시간 설정
            if (request.getOpenTime() != null && request.getCloseTime() != null) {
                store.setOpenTime(request.getOpenTime());
                store.setCloseTime(request.getCloseTime());
                log.info("✅ 영업 시간 설정 완료: {} - {}", request.getOpenTime(), request.getCloseTime());
            }

            // 저장
            log.info("💾 DB에 저장 시도...");
            Store savedStore = storeRepository.save(store);
            log.info("✅ 가게 등록 완료: ID={}, 이름={}", savedStore.getId(), savedStore.getName());

            return StoreResponse.from(savedStore);
            
        } catch (Exception e) {
            log.error("❌ 가게 등록 중 오류 발생", e);
            throw new RuntimeException("가게 등록에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 내가 등록한 가게 목록 조회
     */
    @Transactional(readOnly = true)
    public List<StoreResponse> getMyStores(Member member) {
        log.info("📋 내 가게 목록 조회: memberId={}", member.getId());
        
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
        log.info("✏️ 가게 수정: storeId={}, memberId={}", id, member.getId());
        
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다."));
        
        // 소유자 확인
        if (store.getOwner() != null && !store.getOwner().getId().equals(member.getId())) {
            throw new IllegalArgumentException("가게를 수정할 권한이 없습니다.");
        }
        
        // 기본 정보 수정
        if (request.getName() != null) store.setName(request.getName());
        if (request.getDescription() != null) store.setDescription(request.getDescription());
        if (request.getAddress() != null) store.setAddress(request.getAddress());
        if (request.getPhone() != null) store.setPhone(request.getPhone());
        if (request.getCategory() != null) store.setCategory(request.getCategory());
        
        // 키워드 수정
        if (request.getKeywords() != null) {
            store.setKeywordList(request.getKeywords());
        }
        
        // 메인 이미지 수정
        if (request.getMainImage() != null && !request.getMainImage().isEmpty()) {
            if (store.getMainImageUrl() != null) {
                fileStorageService.deleteFile(store.getMainImageUrl());
            }
            String mainImageUrl = fileStorageService.storeFile(request.getMainImage());
            store.setMainImageUrl(mainImageUrl);
            log.info("📷 메인 이미지 업데이트: {}", mainImageUrl);
        }
        
        // 상세 이미지 수정
        if (request.getDetailImages() != null && !request.getDetailImages().isEmpty()) {
            store.getDetailImageList().forEach(fileStorageService::deleteFile);
            
            List<String> detailImageUrls = new ArrayList<>();
            for (MultipartFile file : request.getDetailImages()) {
                if (file != null && !file.isEmpty()) {
                    String url = fileStorageService.storeFile(file);
                    detailImageUrls.add(url);
                }
            }
            store.setDetailImageList(detailImageUrls);
            log.info("📷 상세 이미지 업데이트: {}개", detailImageUrls.size());
        }
        
        Store updatedStore = storeRepository.save(store);
        log.info("✅ 가게 수정 완료: ID={}", updatedStore.getId());
        
        return StoreResponse.from(updatedStore);
    }

    /**
     * 가게 삭제
     */
    @Transactional
    public void deleteStore(Long id, Member member) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다."));

        // 소유자 확인
        if (store.getOwner() != null && !store.getOwner().getId().equals(member.getId())) {
            throw new IllegalArgumentException("가게를 삭제할 권한이 없습니다.");
        }

        // 이미지 파일 삭제
        if (store.getMainImageUrl() != null) {
            fileStorageService.deleteFile(store.getMainImageUrl());
        }
        store.getDetailImageList().forEach(fileStorageService::deleteFile);

        storeRepository.delete(store);
        log.info("🗑️ 가게 삭제 완료: ID={}", id);
    }

    /**
     * 키워드로 가게 검색
     */
    @Transactional(readOnly = true)
    public List<StoreResponse> searchStores(String keyword, String sort) {
        log.info("🔍 가게 검색: keyword={}, sort={}", keyword, sort);

        List<Store> stores;

        if (keyword == null || keyword.trim().isEmpty()) {
            stores = getAllStoresSorted(sort);
        } else {
            stores = storeRepository.searchStores(keyword.trim());
            stores = sortStores(stores, sort);
        }

        log.info("✅ 검색 결과: {}개", stores.size());
        
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
