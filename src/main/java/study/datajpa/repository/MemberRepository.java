package study.datajpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;

import javax.persistence.Lob;
import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, JpaSpecificationExecutor {
    // 이렇게 이름의 정규식만 잘 맞춰주면, 스프링 데이터 JPA가 알아서 sql 문을 만들어주는 간결성!
    List<Member> findByUsernameAndAgeGreaterThan(String username, int age);

    // 전체에서 Top3 가져오기
    List<Member> findTop3HelloBy();

    // 직접 쿼리 작성 , Query에 오타를 쳐도 애플리케이션 로딩 시점에 에러를 발생시켜준다. 미리 파싱을 시켜준다.
    // 아래는 복잡한 메소드 이름이 생겼을때 쿼리를 직접 쳐줄수 있는 아주 막강하고 좋은 기능!
    @Query("select m from Member m where m.username = :username and m.age = :age")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);

    // [단순히 값 하나를 조회]
    @Query("select m.username from Member m")
    List<String> findUsernameList();

    // [DTO로 직접 조회] DTO 로 반환하기 위해서 new..
    @Query("select new study.datajpa.dto.MemberDto(m.id, m.username, t.name) from Member m join m.team t")
    List<MemberDto> findMemberDto();

    // [파라미터 바인딩]
    @Query("select m from Member m where m.username in :names")
    List<Member> findByNames(@Param("names") Collection<String> names);

    // find .. By 에서 '..'에는 아무거나 와도 상관이 없다!
    // [반환타입] 반환타입이 제한 없다.
    List<Member> findListByUsername(String username); // 컬렉션 (결과없음: 빈컬렉션 반환)
    Member findMemberByUsername(String username); // 단건 (결과없음: null 반환, 2건 이상은 예외 발생)
    Optional<Member> findOptionalByUsername(String username); // 단건 Optional

    // [실무 중요] count 쿼리 분리 가능 (count 쿼리가 join을 하지 않아야하는데 하는 경우 성능이 느려지기 때문에 사용하는 경우 아래처럼 써야함)
    // @Query(value = "select m from Member m", countQuery = "select count(m.username) from Member m")
    Page<Member> findByAge(int age, Pageable pageable);

    // [벌크성 수정 쿼리]
    // 벌크성 수정, 삭제 쿼리는 @Modifying 어노테이션을 사용
    // 벌크성 쿼리를 실행하고 나서 영속성 컨텍스트 초기화: @Modifying(clearAutomatically = true)
    @Modifying(clearAutomatically = true)
    @Query("update Member m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age") int age);

    // [Fetch Join만 이용]
    @Query("select m from Member m join fetch m.team")
    List<Member> findMemberFetchJoin();

    // [엔티티 그래프] 공통 메서드 오버라이드
    @Override
    @EntityGraph(attributePaths = {"team"})
    List<Member> findAll();

    // [엔티티 그래프] JPQL + 엔티티 그래프
    @EntityGraph(attributePaths = {"team"})
    @Query("select m from Member m")
    List<Member> findMemberEntityGraph();

    // fetch join 과 함께 : Entity Graph
    // [엔티티 그래프] 메서드 이름으로 쿼리에서 특히 편리하다.
    @EntityGraph(attributePaths = {"team"})
    List<Member> findEntityGraphByUsername(@Param("username") String username);


    // [JPA Hint & Lock]
    // 읽기 기능만 사용할때 최적화 방법
    @QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Member findReadOnlyByUsername(String username);

    // 아래 방법으로도 읽기 기능만 제한 가능
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Member> findLockByUsername(String username);

    List<UsernameOnlyDto> findProjectionsByUsername(@Param("username") String username);


    // [네이티브 쿼리] => 되도록이면 사용X
    @Query(value = "select * from member where username = ?", nativeQuery = true)
    Member findByNativeQuery(String username);

    @Query(value = "select m.member_id as id, m.username, t.name as teamName" +
            " from member m left join team t",
            countQuery = "select count(*) from member",
            nativeQuery = true)
    Page<MemberProjection> findByNativeProjection(Pageable pageable);
}
