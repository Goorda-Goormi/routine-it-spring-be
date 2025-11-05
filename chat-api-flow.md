# 채팅 시스템 API 사용 가이드

## 케이스 1: 그룹 리더 (그룹 생성자)

1. **그룹 생성**: `POST /groups`
   - 그룹과 채팅방 자동 생성
   - 리더 자동 등록

2. **WebSocket 연결**: `/ws` 엔드포인트

3. **채팅 시작**:
   - 입장: `/app/chat.enter/{roomId}`
   - 메시지: `/app/chat.send/{roomId}`

## 케이스 2: 일반 사용자 (그룹 참여자)

1. **그룹 목록 조회**: `GET /groups`

2. **그룹 참여**: `POST /groups/{groupId}/join` **[2025-08-31 미구현]**

3. **채팅방 조회**: `GET /api/chat/rooms/group/{groupId}`

4. **채팅방 참여**: `POST /api/chat/rooms/{roomId}/join`

5. **WebSocket 연결**: `/ws` 엔드포인트

6. **채팅 시작**:
   - 입장: `/app/chat.enter/{roomId}`
   - 메시지: `/app/chat.send/{roomId}`

## 이모지 리액션

### REST API
- **추가**: `POST /api/chat/messages/{messageId}/reactions` `{"emoji": "👍"}`
- **제거**: `DELETE /api/chat/messages/{messageId}/reactions/{emoji}`
- **조회**: `GET /api/chat/messages/{messageId}/reactions/summary`

### WebSocket (실시간)
- **추가**: `/app/chat.reaction.add/{roomId}` `{"messageId": 1, "emoji": "👍"}`
- **제거**: `/app/chat.reaction.remove/{roomId}` `{"messageId": 1, "emoji": "👍"}`
- **구독**: `/topic/room/{roomId}/reactions`

### 메시지 조회 시 reactions 필드 포함
- `GET /api/chat/rooms/{roomId}/messages` → `reactions: [{emoji: "👍", count: 5, userIds: [1,2,3]}]`

## 주요 차이점

- **리더**: 그룹 생성 → 바로 채팅 가능
- **일반 사용자**: 그룹 찾기 → 그룹 참여 → 채팅방 참여 → 채팅 가능