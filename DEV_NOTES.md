# BluffBall 개발 유의사항

> 작업 시작 전 반드시 이 파일을 확인한다.

---

## 레이어 접근 규칙

| 레이어 | Repository 접근 | Entity 접근 | 비고 |
|---|---|---|---|
| Controller | ❌ | ❌ | Service 메서드 호출만 |
| Service | ❌ | ❌ | usecase 조립 + 이벤트 발행만 |
| usecase/reader | ✅ | ✅ | 읽기 전용 |
| usecase/validator | ❌ | ❌ | 값 규칙 검증만 (인자로 받은 원시 값만 사용) |
| usecase/executor | ✅ | ✅ | 쓰기 전용 |

**핵심 원칙: Entity와 Repository는 usecase 하위 레이어에서만 접근 가능하다.**

---

## Executor 반환 타입 규칙

- Executor 메서드는 Entity를 반환하지 않는다.
- 생성/수정 후 필요한 경우 **ID(String/Long) 또는 원시 값**만 반환한다.
- Service에서 추가 조회가 필요하면 반환받은 ID를 Reader에 넘긴다.

```java
// ❌ 잘못된 예
public MatchInfo createSingleMatch(...) { return matchInfoRepository.save(...); }

// ✅ 올바른 예
public String createSingleMatch(...) {
    matchInfoRepository.save(...);
    return matchSessionId;
}
```

---

## Service 레이어 규칙

- Entity 타입을 import하거나 변수로 선언하지 않는다.
- 다른 도메인의 데이터가 필요한 경우 해당 ID를 받아 Reader를 통해 조회한다.
- WebSocket 이벤트 발행(SimpMessagingTemplate)은 Service에서만 수행한다.
- Reader 메서드 중 Entity를 반환하는 것(예: `getById`)은 Executor/Reader 내부에서만 호출한다.
  - Service는 Reader의 **원시값·ID·DTO 반환 메서드**만 호출한다.
  - 예: `matchInfoReader.getCurrentPitcherUserId(...)`, `gameStateReader.isTop(...)`, `cardReader.getCoordinateNumber(...)`
- HitEventExecutor는 자체 완결(self-contained) 설계로 Reader를 주입받아 내부적으로 조회한다.
  - Service는 ID와 Request만 전달하면 된다.

---

## Reader 메서드 규칙

| 메서드 유형 | 반환 타입 | 호출 가능 레이어 |
|---|---|---|
| `getById` | Entity | Executor·Reader 내부만 |
| `getCurrentXxxUserId`, `isTop`, `getTurnNumber` 등 | 원시값(Long/int/boolean) | Service ✅ |
| `getPitchCardDetails(List<Long>)` | DTO(record) | Service ✅ |

**규칙**: Service 코드에서 Entity 타입 변수가 존재하면 안 된다.

---

## 주석 작성 규칙

- **모든 public·private 메서드**에 역할·파라미터·반환값·예외를 Javadoc으로 작성한다.
- **클래스 필드·상수**에 용도를 한 줄 주석으로 작성한다.
- **Service 내부 분기·핵심 로컬 변수**에는 왜 필요한지 한 줄 인라인 주석을 달 수 있다.
- 자명한 getter/setter·Lombok 생성 코드에는 주석을 생략한다.
- `DEV_NOTES.md`와 달리 API 문서(Swagger `@Operation`)와 Javadoc은 역할이 겹치더라도 **코드 독자를 위해 Javadoc을 우선** 작성한다.
