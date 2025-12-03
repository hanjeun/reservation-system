package ac.inhatc.reservation_system.customerservice.service;

import ac.inhatc.reservation_system.customerservice.dto.NoticeDTO;
import ac.inhatc.reservation_system.customerservice.dto.NoticeRequestDTO;
import ac.inhatc.reservation_system.customerservice.entity.Notice;
import ac.inhatc.reservation_system.customerservice.repository.NoticeRepository;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;

    // 모든 공지사항 조회 (중요 공지 먼저)
    public List<NoticeDTO> getAllNotices() {
        return noticeRepository.findAllOrderByImportantAndCreatedAt()
                .stream()
                .map(NoticeDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // 공지사항 상세 조회
    @Transactional
    public NoticeDTO getNoticeById(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다. ID: " + id));
        
        // 조회수 증가
        notice.incrementViewCount();
        
        return NoticeDTO.fromEntity(notice);
    }

    // 공지사항 작성 (관리자만)
    @Transactional
    public NoticeDTO createNotice(NoticeRequestDTO requestDTO, String email) {
        Member author = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 관리자 권한 확인 (Member의 isAdmin() 메서드 사용)
        if (!author.isAdmin()) {
            throw new IllegalStateException("관리자만 공지사항을 작성할 수 있습니다.");
        }

        Notice notice = Notice.builder()
                .author(author)
                .title(requestDTO.getTitle())
                .content(requestDTO.getContent())
                .isImportant(requestDTO.getIsImportant() != null ? requestDTO.getIsImportant() : false)
                .build();

        Notice savedNotice = noticeRepository.save(notice);
        return NoticeDTO.fromEntity(savedNotice);
    }

    // 공지사항 수정 (관리자만)
    @Transactional
    public NoticeDTO updateNotice(Long id, NoticeRequestDTO requestDTO, String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 관리자 권한 확인
        if (!member.isAdmin()) {
            throw new IllegalStateException("관리자만 공지사항을 수정할 수 있습니다.");
        }

        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다. ID: " + id));

        notice.setTitle(requestDTO.getTitle());
        notice.setContent(requestDTO.getContent());
        if (requestDTO.getIsImportant() != null) {
            notice.setIsImportant(requestDTO.getIsImportant());
        }

        return NoticeDTO.fromEntity(notice);
    }

    // 공지사항 삭제 (관리자만)
    @Transactional
    public void deleteNotice(Long id, String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 관리자 권한 확인
        if (!member.isAdmin()) {
            throw new IllegalStateException("관리자만 공지사항을 삭제할 수 있습니다.");
        }

        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다. ID: " + id));

        noticeRepository.delete(notice);
    }
}
