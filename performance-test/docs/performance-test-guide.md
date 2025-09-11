# 🚀 Routine-IT 성능 테스트 & 최적화 가이드

## 📖 **프로젝트 개요**
이 문서는 Routine-IT의 **3개 핵심 API**(채팅, 개인 루틴, 알림)에 대한 체계적인 성능 최적화 프로세스를 다룹니다.

## 🎯 **핵심 테스트 대상 API**
1. **🔥 채팅 API** - 실시간 메시지 송수신 (최우선)
2. **🔥 개인 루틴 API** - 루틴 CRUD 및 완료 처리 (높은 트래픽)
3. **🔥 알림 API** - 알림 조회 및 상태 관리 (빈번한 폴링)

---

## 📊 **성능 목표 (SLA)**

### **🔥 채팅 API 성능 목표**
```
📋 핵심 메트릭:
- 메시지 조회 TPS: 200+ 
- 메시지 전송 TPS: 100+
- 평균 응답시간: < 300ms
- 95th percentile: < 500ms
- 에러율: < 1%
- 동시 WebSocket 연결: 1000개
```

### **🔥 개인 루틴 API 성능 목표**
```
📋 핵심 메트릭:
- 오늘의 루틴 조회 TPS: 500+
- 루틴 완료 처리 TPS: 300+
- 평균 응답시간: < 200ms
- 95th percentile: < 400ms
- 에러율: < 0.5%
- 동시 사용자: 800명
```

### **🔥 알림 API 성능 목표**
```
📋 핵심 메트릭:
- 미읽은 수 조회 TPS: 1000+
- 읽음 처리 TPS: 200+
- 평균 응답시간: < 100ms
- 95th percentile: < 200ms
- 에러율: < 0.1%
- 폴링 주기: 10초
```

### **시스템 리소스 목표**
- **CPU 사용률**: 70% 이하
- **JVM 힙 메모리**: 80% 이하
- **데이터베이스 커넥션**: 80% 이하
- **응답시간 변동성**: 낮음 유지

---

## 🎭 **5단계 성능 최적화 프로세스**

### **1단계: 벌크 데이터 삽입 (saveAll 활용)**
```java
🎯 목표 데이터량:
- 사용자: 10,000명
- 개인 루틴: 50,000개  
- 채팅 메시지: 100,000개
- 알림: 200,000개

💡 구현 방법:
- Spring Data JPA saveAll() 사용
- 1000건씩 배치 처리로 메모리 최적화
- @Transactional로 일관성 보장
```

### **2단계: 로컬 환경 성능 확인**
```bash
🔍 확인 항목:
- JVM 힙 메모리 사용량 모니터링
- 가비지 컬렉션 빈도 확인
- 데이터베이스 커넥션 풀 상태
- 기본 API 응답시간 측정

📊 모니터링 명령어:
docker stats routine-app --no-stream
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### **3단계: JMeter 부하 테스트**
```
📋 팀원별 시나리오:
👤 팀원1: 채팅 API (300명, 10분)
👤 팀원2: 루틴 API (500명, 15분)  
👤 팀원3: 알림 API (200명, 20분)

🎯 측정 지표:
- TPS (초당 처리량)
- 평균/95th percentile 응답시간
- 에러율 및 에러 유형
- 동시 사용자 처리 능력
```

### **4단계: 병목 진단 및 분석**
```sql
🔍 데이터베이스 병목 진단:
-- 슬로우 쿼리 확인
SELECT query, exec_count, avg_timer_wait/1000000000 as avg_time_sec
FROM performance_schema.events_statements_summary_by_digest 
WHERE avg_timer_wait > 1000000000 
ORDER BY avg_timer_wait DESC LIMIT 10;

-- 인덱스 사용 분석
EXPLAIN SELECT * FROM chat_messages WHERE room_id = ? ORDER BY created_at DESC LIMIT 50;
```

```bash
🔍 애플리케이션 병목 진단:
# JVM 스레드 덤프
curl http://localhost:8080/actuator/threaddump

# 메모리 누수 확인
curl http://localhost:8080/actuator/heapdump -o heapdump.hprof

# 커넥션 풀 상태
curl http://localhost:8080/actuator/metrics/hikaricp.connections
```

### **5단계: 최적화 적용 및 재측정**
```java
// 🚀 캐싱 최적화
@Cacheable(value = "unreadNotificationCount", key = "#userId")
public Long getUnreadNotificationCount(Long userId) { ... }

// 🚀 배치 업데이트  
@Modifying
@Query("UPDATE PersonalRoutine r SET r.isCompleted = true WHERE r.id IN :ids")
void batchCompleteRoutines(@Param("ids") List<Long> ids);

// 🚀 비동기 처리
@Async
public void sendNotificationAsync(NotificationDto notification) { ... }
```

---

## 📋 **테스트 시나리오 상세**

### **시나리오 1: 채팅 API 집중 테스트**
```yaml
🎯 목적: 실시간 채팅 시스템의 대량 동시 처리 성능 측정
👥 부하: 300명 동시 사용자, 램프업 60초, 지속시간 10분

📝 플로우:
1. GET /api/chat/rooms - 채팅방 목록 조회
2. GET /api/chat/rooms/{id}/messages?page=0&size=50 - 메시지 조회
3. POST /api/chat/rooms/{id}/messages - 메시지 전송  
4. WebSocket /ws/chat/{roomId} - 실시간 채팅 연결

⏱️ 사용자 패턴:
- 메시지 조회: 2-5초 간격
- 메시지 전송: 5-10초 간격
- WebSocket 연결: 세션 유지
```

### **시나리오 2: 개인 루틴 API 집중 테스트**
```yaml
🎯 목적: 루틴 완료 처리의 대량 동시 업데이트 성능 측정
👥 부하: 500명 동시 사용자, 램프업 90초, 지속시간 15분

📝 플로우:
1. GET /api/personal-routines/today - 오늘의 루틴 조회
2. GET /api/personal-routines?page=0&size=20 - 루틴 목록 조회
3. POST /api/personal-routines/{id}/complete - 루틴 완료 처리
4. GET /api/personal-routines/calendar?year=2025&month=9 - 달력 뷰

⏱️ 사용자 패턴:
- 조회 API: 70% (빈번한 확인)
- 완료 처리: 25% (주요 액션)
- 달력 뷰: 5% (주기적 확인)
```

### **시나리오 3: 알림 API 집중 테스트**
```yaml
🎯 목적: 알림 폴링 및 상태 관리의 실시간 처리 성능 측정
👥 부하: 200명 동시 사용자, 램프업 30초, 지속시간 20분

📝 플로우:
1. GET /api/notifications/unread/count - 미읽은 알림 수 (폴링)
2. GET /api/notifications?page=0&size=20 - 알림 목록 조회
3. PUT /api/notifications/{id}/read - 읽음 처리
4. DELETE /api/notifications/{id} - 알림 삭제

⏱️ 사용자 패턴:
- 미읽은 수 조회: 10초 간격 (폴링)
- 알림 목록: 30초 간격
- 읽음/삭제: 사용자 액션에 따라
```

---

## 📈 **성능 측정 지표**

### **핵심 측정 지표**
```
📊 응답시간 지표:
- Average Response Time (평균 응답시간)
- 95th Percentile (상위 5% 응답시간)
- 99th Percentile (상위 1% 응답시간)
- Maximum Response Time (최대 응답시간)

📊 처리량 지표:
- TPS (Transactions Per Second)
- Throughput (bytes/sec)
- Concurrent Users (동시 사용자)

📊 안정성 지표:
- Error Rate (에러율 %)
- Success Rate (성공률 %)
- Error Distribution (에러 유형별 분포)
```

### **시스템 메트릭**
```
💻 애플리케이션 메트릭:
- JVM Heap Memory Usage
- GC Frequency and Duration  
- Active Thread Count
- Connection Pool Usage

🗄️ 데이터베이스 메트릭:
- Query Execution Time
- Connection Pool Status
- Slow Query Count
- Lock Wait Time

🌐 네트워크 메트릭:
- Request/Response Size
- Network Latency
- WebSocket Connection Count
```

---

## 🎭 **테스트 단계별 실행 계획**

### **Phase 1: 기능 검증 테스트 (Smoke Test)**
```bash
🎯 목적: 기본 기능 동작 확인
👥 부하: 10명 동시사용자, 5분간
✅ 성공 기준: 에러율 0%, 모든 API 정상 응답

# 실행 명령어
jmeter -n -t scenarios/smoke-test.jmx -l results/smoke-results.jtl
```

### **Phase 2: 개별 API 부하 테스트**
```bash
🎯 목적: 각 API별 개별 성능 측정
👥 부하: API별 목표 사용자 수
✅ 성공 기준: 각 API 성능 목표 달성

# 실행 순서
jmeter -n -t scenarios/chat-performance-test.jmx -l results/chat-results.jtl
jmeter -n -t scenarios/routine-performance-test.jmx -l results/routine-results.jtl  
jmeter -n -t scenarios/notification-performance-test.jmx -l results/notification-results.jtl
```

### **Phase 3: 통합 부하 테스트**
```bash
🎯 목적: 모든 API 동시 실행시 성능 측정
👥 부하: 전체 1000명 동시사용자
✅ 성공 기준: 시스템 전체 안정성 유지

# 실행 명령어
jmeter -n -t scenarios/integrated-load-test.jmx -l results/integrated-results.jtl
```

### **Phase 4: 최적화 후 재측정**
```bash
🎯 목적: 최적화 효과 검증
👥 부하: 동일한 조건으로 재테스트
✅ 성공 기준: 성능 개선 효과 확인

# 비교 분석
./scripts/collect-results.sh
./scripts/compare-before-after.sh
```

---

## 🔧 **팀원별 작업 가이드**

### **👤 팀원 1 (채팅 API 담당)**
```
📁 담당 파일:
├── jmeter/scenarios/chat-performance-test.jmx
├── jmeter/test-data/chat-test-data.csv  
└── optimization/chat-optimization.sql

🎯 최적화 포인트:
- 채팅 메시지 페이징 최적화
- WebSocket 연결 관리
- 실시간 메시지 전송 성능
- 채팅방별 인덱스 최적화
```

### **👤 팀원 2 (루틴 API 담당)**
```
📁 담당 파일:
├── jmeter/scenarios/routine-performance-test.jmx
├── jmeter/test-data/routine-test-data.csv
└── optimization/routine-optimization.sql

🎯 최적화 포인트:
- N+1 쿼리 문제 해결
- 루틴 완료 배치 처리
- 달력 뷰 성능 최적화  
- 사용자별 루틴 조회 최적화
```

### **👤 팀원 3 (알림 API 담당)**
```
📁 담당 파일:
├── jmeter/scenarios/notification-performance-test.jmx
├── jmeter/test-data/notification-test-data.csv
└── optimization/notification-optimization.sql

🎯 최적화 포인트:
- 미읽은 알림 수 캐싱
- 알림 폴링 최적화
- 읽음 처리 배치 업데이트
- 알림 타입별 인덱스 전략
```

---

## 📊 **모니터링 및 분석**

### **실시간 모니터링**
```
📈 Grafana 대시보드: http://localhost:3000
├── API Response Time by Endpoint
├── TPS (Transactions Per Second)  
├── Error Rate by API
├── JVM Memory Usage
├── Database Connection Pool
└── WebSocket Active Connections

📊 Prometheus 메트릭: http://localhost:9090
├── http_server_requests_seconds
├── jvm_memory_used_bytes
├── hikaricp_connections_active
└── custom_api_metrics
```

### **결과 분석 도구**
```bash
# HTML 리포트 생성
jmeter -g results/chat-results.jtl -o reports/chat-html-report/

# 결과 비교 분석
./scripts/analyze-results.py

# 성능 트렌드 분석
./scripts/trend-analysis.py --period 7days
```

---

## 🎯 **성공 기준 및 평가**

### **개별 API 성공 기준**
```
✅ 채팅 API:
- 메시지 조회 TPS > 200
- 평균 응답시간 < 300ms
- 에러율 < 1%

✅ 루틴 API:  
- 오늘의 루틴 TPS > 500
- 평균 응답시간 < 200ms
- 에러율 < 0.5%

✅ 알림 API:
- 미읽은 수 조회 TPS > 1000  
- 평균 응답시간 < 100ms
- 에러율 < 0.1%
```

### **통합 시스템 성공 기준**
```
✅ 전체 시스템:
- 동시 사용자 1000명 처리
- 시스템 안정성 유지
- 메모리 누수 없음
- 데이터베이스 커넥션 고갈 없음
```

---

### **참고 자료**
- [JMeter 성능 테스트 가이드](docs/jmeter-scenarios-guide.md)
- [분산 설정 가이드](docs/distributed-setup.md)
- [최적화 체크리스트](docs/optimization-checklist.md)

---