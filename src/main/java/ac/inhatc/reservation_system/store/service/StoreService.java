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
        
        if (request.getKeywords() != null) {
            store.setKeywordList(request.getKeywords());
        }
        
        if (request.getMainImage() != null && !request.getMainImage().isEmpty()) {
            if (store.getMainImageUrl() != null) {
                fileStorageService.deleteFile(store.getMainImageUrl());
            }
            store.setMainImageUrl(fileStorageService.storeFile(request.getMainImage()));
        }
        
        if (request.getDetailImages() != null && !request.getDetailImages().isEmpty()) {
            store.getDetailImageList().forEach(fileStorageService::deleteFile);
            
            List<String> detailImageUrls = new ArrayList<>();
            for (MultipartFile file : request.getDetailImages()) {
                if (file != null && !file.isEmpty()) {
                    detailImageUrls.add(fileStorageService.storeFile(file));
                }
            }
            store.setDetailImageList(detailImageUrls);
        }
        
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

        if (store.getMainImageUrl() != null) {
            fileStorageService.deleteFile(store.getMainImageUrl());
        }
        store.getDetailImageList().forEach(fileStorageService::deleteFile);

        storeRepository.delete(store);
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
