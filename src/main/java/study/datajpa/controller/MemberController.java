package study.datajpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;
import study.datajpa.repository.MemberRepository;

import javax.annotation.PostConstruct;

@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberRepository memberRepository;

    // [도메인 클래스 컨버터 사용 전]
    @GetMapping("/members/{id}")
    public String findMember(@PathVariable("id") Long id) {
        Member member = memberRepository.findById(id).get();
        return member.getUsername();
    }

    // [도메인 클래스 컨버터 사용 후] (편하고 간단간단할때만 쓰자. 좋은건 아님.)
    // 무조건 조회용으로만 써야함.
    @GetMapping("/members2/{id}")
    public String findMember2(@PathVariable("id") Member member) {
        return member.getUsername();
    }


    // Paging을 쉽게 하는 방법
    // 기본값 즉, Global 값은 application.yml 에서 변경 가능 (default-page-size: 10, max-page-size: 2000)
    // Global 을 사용하지 않을시 컨트롤러 여기서 @PageableDefault 이용
    // http://localhost:8080/members?page=0&size=3&sort=username,desc
    @GetMapping("/members")
    public Page<MemberDto> list(@PageableDefault(size = 12, sort = "username", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Member> page = memberRepository.findAll(pageable);
//        Page<MemberDto> map = page.map(member -> new MemberDto(member.getId(), member.getUsername(), null));
        Page<MemberDto> map = page.map(member -> new MemberDto(member)); // 람다로 바꿔도 됨.

        return map;
    }


//    @PostConstruct
    public void init() {
        for (int i = 0; i < 100; i++) {
            memberRepository.save(new Member("user" + i, i));
        }
    }
}
