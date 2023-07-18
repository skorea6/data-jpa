# 스프링 데이터 JPA
#### 인프런 강의: [실전! 스프링 데이터 JPA](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-%EB%8D%B0%EC%9D%B4%ED%84%B0-JPA-%EC%8B%A4%EC%A0%84/dashboard)

## 롬복
- @Setter: 실무에서 가급적 Setter는 사용하지 않기
- @NoArgsConstructor AccessLevel.PROTECTED: 기본 생성자 막고 싶은데, JPA 스팩상 PROTECTED로 열어두어야 함
- @ToString은 가급적 내부 필드만(연관관계 없는 필드만)

## Introuduce
1. 순수한 JPA 기반 리포지토리
- 기본 CRUD
	- 저장
	- 변경 변경감지 사용 삭제
	- 전체 조회
	- 단건 조회
	- 카운트
- 참고: JPA에서 수정은 변경감지 기능을 사용하면 된다.
- 트랜잭션 안에서 엔티티를 조회한 다음에 데이터를 변경하면, 트랜잭션 종료 시점에 변경감지 기능이 작동
해서 변경된 엔티티를 감지하고 UPDATE SQL을 실행한다


2. 스프링 데이터 JPA 공통 인터페이스 소개
- org.springframework.data.repository.Repository 를 구현한 클래스는 스캔 대상
	- MemberRepository 인터페이스가 동작한 이유:
	- 실제 출력해보기(Proxy)
	- memberRepository.getClass() -> class com.sun.proxy.$ProxyXXX
- @Repository 애노테이션 생략 가능
	- 컴포넌트 스캔을 스프링 데이터 JPA가 자동으로 처리
	- JPA 예외를 스프링 예외로 변환하는 과정도 자동으로 처리


3. 스프링 데이터 JPA 공통 인터페이스 설정
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

}
```


4. JpaRepository 인터페이스: 공통 CRUD 제공
- 제네릭은 <엔티티 타입, 식별자 타입> 설정


## 쿼리 메소드 기능
- 메소드 이름으로 쿼리 생성
  	```java
	public interface MemberRepository extends JpaRepository<Member, Long> {
	    List<Member> findByUsernameAndAgeGreaterThan(String username, int age);
	}
	```
  	- 스프링 데이터 JPA는 메소드 이름을 분석해서 JPQL을 생성하고 실행
	- 참고: 이 기능은 엔티티의 필드명이 변경되면 인터페이스에 정의한 메서드 이름도 꼭 함께 변경해야 한다. 그렇지 않으면 애플리케이션을 시작하는 시점에 오류가 발생한다. 이렇게 애플리케이션 로딩 시점에 오류를 인지할 수 있는 것이 스프링 데이터 JPA의 매우 큰 장점이다.

- 메소드 이름으로 JPA NamedQuery 호출
	- 파라미터가 증가하면 메서드 이름이 매우 지저분, 아래 방법 사용

- @Query : 리파지토리 메소드에 쿼리 정의 파라미터 바인딩, 리파지토리 인터페이스에 쿼리 직접 정의
  	```java
	public interface MemberRepository extends JpaRepository<Member, Long> {
		@Query("select m from Member m where m.username = :username and m.age = :age")
	    	List<Member> findUser(@Param("username") String username, @Param("age") int age);
	}
	```

- 반환 타입
	- List<Member> findListByUsername(String username); => 컬렉션 (결과없음: 빈컬렉션 반환)
	- Member findMemberByUsername(String username); => 단건 (결과없음: null 반환, 2건 이상은 예외 발생)
	- Optional<Member> findOptionalByUsername(String username); => 단건 Optional
    - 반환 타입 제한이 없다.
    - find .. By 에서 '..'에는 아무거나 와도 상관이 없다!

- 페이징과 정렬
	- 페이징과 정렬 파라미터:
		- org.springframework.data.domain.Sort : 정렬 기능
		- org.springframework.data.domain.Pageable : 페이징 기능 (내부에 Sort 포함)
	- 특별한 반환 타입:
		- org.springframework.data.domain.Page : 추가 count 쿼리 결과를 포함하는 페이징
		- org.springframework.data.domain.Slice : 추가 count 쿼리 없이 다음 페이지만 확인 가능(내부적 으로 limit + 1조회)
		- List (자바 컬렉션): 추가 count 쿼리 없이 결과만 반환

	- Page는 1부터 시작이 아니라 0부터 시작이다.
		- PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));

	- count 쿼리 분리 가능
		- count 쿼리가 join을 하지 않아야하는데 하는 경우 성능이 느려지기 때문에 사용하는 경우 아래처럼 분리해서 써야함
		- @Query(value = "select m from Member m", countQuery = "select count(m.username) from Member m")

- 벌크성 수정 쿼리
	- 벌크성 수정, 삭제 쿼리는 @Modifying 어노테이션을 사용
	- 벌크성 쿼리를 실행하고 나서 영속성 컨텍스트 초기화: @Modifying(clearAutomatically = true)

	- 참고: 벌크 연산은 영속성 컨텍스트를 무시하고 실행하기 때문에, 영속성 컨텍스트에 있는 엔티티의 상태와 DB에 엔티티 상태가 달라질 수 있다.
		- 1. 영속성 컨텍스트에 엔티티가 없는 상태에서 벌크 연산을 먼저 실행한다.
		- 2. 부득이하게 영속성 컨텍스트에 엔티티가 있으면 벌크 연산 직후 영속성 컨텍스트를 초기화 한다.

- @EntityGraph
	- 연관된 엔티티들을 SQL 한번에 조회하는 방법
	- member와 team은 지연로딩 관계이다. 따라서 team의 데이터를 조회할 때 마다 쿼리가 실행된다. (N+1 문제 발생)
	- 사실상 페치 조인(FETCH JOIN)의 간편 버전 -> LEFT OUTER JOIN 사용


## JPA Hint & Lock
- 읽기 기능만 사용할때 최적화 방법
- 쿼리 힌트: @QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value ="true"))
- 락: @Lock(LockModeType.PESSIMISTIC_WRITE)


## 사용자 정의 리포지토리 구현
- 사용자 정의 인터페이스:
```java
public interface MemberRepositoryCustom {
      List<Member> findMemberCustom();
}
```

- 사용자 정의 인터페이스 구현 클래스:
```java
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {
    private final EntityManager em;
    @Override
    public List<Member> findMemberCustom() {
        return em.createQuery("select m from Member m").getResultList();
    }
}
```

- 사용자 정의 인터페이스 상속:
```java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

}
```

- 사용자 정의 메서드 호출 코드:
```java
List<Member> result = memberRepository.findMemberCustom();
```

## Auditing
- 엔티티를 생성, 변경할 때 변경한 사람과 시간을 추적하고 싶으면?
	- 등록일
	- 수정일
	- 등록자
	- 수정자


- 스프링 데이터 JPA 사용
	- @EnableJpaAuditing: 스프링 부트 설정 클래스에 적용해야함
	- @EntityListeners(AuditingEntityListener.class): 엔티티에 적용


## 스프링 데이터 JPA 구현체
- @Repository 적용: JPA 예외를 스프링이 추상화한 예외로 변환
- @Transactional 트랜잭션 적용:
	- JPA의 모든 변경은 트랜잭션 안에서 동작
	- 스프링 데이터 JPA는 변경(등록, 수정, 삭제) 메서드를 트랜잭션 처리
	- 서비스 계층에서 트랜잭션을 시작하지 않으면 리파지토리에서 트랜잭션 시작
	- 서비스 계층에서 트랜잭션을 시작하면 리파지토리는 해당 트랜잭션을 전파 받아서 사용
	- 그래서 스프링 데이터 JPA를 사용할 때 트랜잭션이 없어도 데이터 등록, 변경이 가능했음 (사실은 트랜잭션이 리포지토리 계층에 걸려있는 것임)
- @Transactional(readOnly = true):
	- 데이터를 단순히 조회만 하고 변경하지 않는 트랜잭션에서 readOnly = true 옵션을 사용하면 플러시를 생략해서 약간의 성능 향상을 얻을 수 있음


## Projections
- 엔티티 대신에 DTO를 편리하게 조회할 때 사용
- 전체 엔티티가 아니라 만약 회원 이름만 딱 조회하고 싶으면?

- 주의:
	- 프로젝션 대상이 ROOT 엔티티면, JPQL SELECT 절 최적화 가능
	- 프로젝션 대상이 ROOT가 아니면 (1) LEFT OUTER JOIN 처리, (2) 모든 필드를 SELECT해서 엔티티로 조회한 다음에 계산

- 정리:
	- 프로젝션 대상이 ROOT 엔티티면 유용하다.
	- 프로젝션 대상이 ROOT 엔티티를 넘어가면 JPQL SELECT 최적화가 안된다! 실무의 복잡한 쿼리를 해결하기에는 한계가 있다.
	- 실무에서는 단순할 때만 사용하고, 조금만 복잡해지면 QueryDSL을 사용하자


## 네이티브 쿼리
- 가급적 네이티브 쿼리는 사용하지 않는게 좋음, 정말 어쩔 수 없을 때 사용
- 최근에 나온 궁극의 방법 스프링 데이터 Projections 활용
- 페이징 지원
- 반환 타입: Object[], Tuple, DTO(스프링 데이터 인터페이스 Projections 지원)
- 제약:
	- Sort 파라미터를 통한 정렬이 정상 동작하지 않을 수 있음(믿지 말고 직접 처리)
	- JPQL처럼 애플리케이션 로딩 시점에 문법 확인 불가
	- 동적 쿼리 불가
