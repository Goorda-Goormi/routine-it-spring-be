package com.goormi.routine.domain.calendar.service;

import com.goormi.routine.domain.personal_routines.domain.PersonalRoutine;
import static com.goormi.routine.domain.calendar.dto.KakaoCalendarDto.*;

public interface CalendarPersonalService {
    String createPersonalSchedule(Long userId, PersonalRoutine personalRoutine);

    void updatePersonalSchedule(Long userId, PersonalRoutine personalRoutine, String eventId);

    void deletePersonalSchedule(String eventId, Long userId);

    GetEventsResponse getPersonalEvents(Long userId);
}
