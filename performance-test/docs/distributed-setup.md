# 🔧 Routine-IT 고부하 성능 테스트 설정 가이드

## 📖 **개요**
이 문서는 Routine-IT 애플리케이션의 성능 테스트를 위한 JMeter 분산 환경 구성 가이드입니다.
실제로는 **단일 서버에서 고부하 테스트**를 수행하며, IP 공개 없이 안전하게 협업할 수 있는 방법을 제공합니다.

## 🎯 **테스트 전략**
- **분산 테스트 대신**: 단일 서버에서 멀티 스레드 활용
- **팀 협업 방식**: JMX 파일 공유를 통한 협업
- **보안**: 개인 IP 주소 공개 없음
- **핵심 API 집중**: 채팅, 개인 루틴, 알림 API 성능 최적화

---

## 🏗️ **1. 환경 구성**

### **시스템 요구사항**
```
💻 하드웨어:
- CPU: 4코어 이상 (8코어 권장)
- RAM: 8GB 이상 (16GB 권장)
- 디스크: 10GB 이상 여유 공간

🛠️ 소프트웨어:
- Java: 11 이상
- JMeter: 5.6.3
- Docker: 최신 버전
- Git: 최신 버전
```

### **네트워크 설정**
```
🌐 포트 사용:
- 8080: Spring Boot 애플리케이션
- 3306: MySQL 데이터베이스
- 6379: Redis 캐시
- 3000: Grafana 모니터링
- 9090: Prometheus 메트릭
- 9100: Node Exporter
```

---

## ⚙️ **2. JMeter 설정**

### **단일 서버 고부하 설정**
기존의 분산 설정을 단일 서버 최적화로 변경합니다.

#### **jmeter.properties 설정**
```properties
# 메모리 최적화
-Xms2g -Xmx8g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100

# 스레드 설정
jmeter.threads.max=2000
jmeter.max_pool_size=2000

# 결과 저장 최적화
jmeter.save.saveservice.output_format=csv
jmeter.save.saveservice.thread_counts=true
jmeter.save.saveservice.timestamp_format=ms

# 네트워크 최적화
httpclient.timeout=60000
httpclient.max_connections_per_host=100
httpclient.max_total_connections=1000
```

#### **JVM 메모리 설정**
```bash
# Windows (jmeter.bat 수정)
set HEAP=-Xms2g -Xmx8g -XX:+UseG1GC

# Linux/Mac (jmeter 수정)
HEAP="-Xms2g -Xmx8g -XX:+UseG1GC"
```

---

## 👥 **3. 팀 협업 설정**

### **역할별 파일 담당**
```
📁 performance-test/
├── 👤 팀원1 (채팅 담당)
│   ├── jmeter/scenarios/chat-performance-test.jmx
│   ├── jmeter/test-data/chat-test-data.csv
│   └── optimization/chat-indexes.sql
├── 👤 팀원2 (루틴 담당)  
│   ├── jmeter/scenarios/routine-performance-test.jmx
│   ├── jmeter/test-data/routine-test-data.csv
│   └── optimization/routine-indexes.sql
├── 👤 팀원3 (알림 담당)
│   ├── jmeter/scenarios/notification-performance-test.jmx
│   ├── jmeter/test-data/notification-test-data.csv
│   └── optimization/notification-indexes.sql
└── 👤 Master (통합 관리)
    ├── monitoring/ (전체 시스템)
    ├── scripts/ (실행 스크립트)
    └── reports/ (결과 분석)
```

### **Git 브랜치 전략**
```bash
# 메인 브랜치
main: 안정화된 성능 테스트 환경

# 개발 브랜치  
develop: 통합 작업 브랜치

# 기능 브랜치
feature/performance-test-setup: 테스트환경 셋업
feature/chat-performance: 팀원1 채팅 테스트
feature/routine-performance: 팀원2 루틴 테스트  
feature/notification-performance: 팀원3 알림 테스트
feature/optimization: 성능 최적화 작업
```

---

## 🔧 **4. JMeter 시나리오 작성 가이드**

### **기본 시나리오 구조**
```
📋 Test Plan
├── 🧵 Thread Group (사용자 그룹)
│   ├── Number of Threads: 100-500
│   ├── Ramp-Up Period: 60-120초
│   └── Loop Count: 무한 (Duration 제어)
├── 📊 CSV Data Set Config (테스트 데이터)
├── 🌐 HTTP Request Defaults
├── 🔗 HTTP Cookie Manager
├── 📝 HTTP Header Manager
├── 🎯 HTTP Request Samplers
│   ├── 로그인 API
│   ├── 주요 기능 API
│   └── 로그아웃 API
├── ⏱️ Timers (사용자 대기시간)
├── ✅ Assertions (응답 검증)
└── 📈 Listeners (결과 수집)
    ├── Aggregate Report
    ├── View Results Tree
    └── Summary Report
```

---

## 📊 **5. 모니터링 설정**

### **Grafana 대시보드 패널**
```yaml
📈 대시보드 구성:
├── 🔥 실시간 메트릭
│   ├── Active Threads (활성 스레드)
│   ├── Response Time (응답시간)
│   ├── Throughput (처리량/TPS)
│   └── Error Rate (오류율)
├── 💻 시스템 메트릭  
│   ├── CPU Usage (CPU 사용률)
│   ├── Memory Usage (메모리 사용률)
│   ├── JVM Heap (JVM 힙 메모리)
│   └── Database Connections (DB 커넥션)
├── 🗄️ 데이터베이스 메트릭
│   ├── Query Execution Time (쿼리 실행시간)
│   ├── Connection Pool (커넥션 풀)
│   ├── Slow Query Count (슬로우 쿼리)
│   └── Lock Wait Time (락 대기시간)
└── 📱 애플리케이션 메트릭
    ├── API Response Time by Endpoint
    ├── Request Count by Status Code  
    ├── Cache Hit Rate (캐시 적중률)
    └── Background Job Queue
```

---

### **외부 리소스**
- [Apache JMeter 공식 문서](https://jmeter.apache.org/usermanual/)
- [Spring Boot Actuator 가이드](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Grafana 대시보드 설정](https://grafana.com/docs/grafana/latest/dashboards/)
- [Prometheus 모니터링](https://prometheus.io/docs/introduction/overview/)

---