### 서브 캘린더 생성

- https://developers.kakao.com/docs/latest/ko/talkcalendar/rest-api#calendar-create-sub
    - 유저가 로그인시 db와 서버의 subCalendarId 값을 비교하여 동기화
        - 이미 서브 캘린더가 있다면 새로 생성하지 않도록 함.
        - name = "routine-it for group",  color = "LIME", reminder = 10

### 서브 캘린더 삭제

- https://developers.kakao.com/docs/latest/ko/talkcalendar/rest-api#calendar-delete-sub
    - 미적용 (일정 기록이 남음. 개별적으로 톡캘린더에서 삭제 가능)
    - 삭제 후 로그인시 다시 생성됨

### 일정 생성

- https://developers.kakao.com/docs/latest/ko/talkcalendar/rest-api#common-event-create
    - 유저별 서브 캘린더에 일정 생성 (그룹)
        - 그룹 가입 후 그룹멤버의 상태가 JOINED일때 생성됨
        - 그룹이름과 그룹설명, alarmTime, authDays(ex “0101010” == 월수금이 실행날짜)과 연동
        - 일정의 반복 주기는 3개월.
    - 유저별 내캘린더(기본)에 일정 생성 (개인)
        - 개인 루틴 생성시 같이 생성
        - 루틴이름과 루틴설명, startTime, repeatDays(ex “0101010” == 월수금이 실행날짜)과 연동
        - 일정의 반복 주기는 3개월.


### 일정 수정

- https://developers.kakao.com/docs/latest/ko/talkcalendar/rest-api#common-event-modify
    - 그룹 정보나 개인루틴 업데이트 될 때 그에 맞게 일정이 업데이트  됨
        - recur_update_type = “THIS_AND_FOLLOWING”

### 일정 삭제

- https://developers.kakao.com/docs/latest/ko/talkcalendar/rest-api#common-event-delete
    - 유저가 그룹에 나가 그룹멤버 상태가 JOINED가 아닌경우 일정이 삭제됨
    - 유저가 가입된 그룹이 삭제된 경우 일정이 삭제됨
    - 개인 루틴 삭제 시