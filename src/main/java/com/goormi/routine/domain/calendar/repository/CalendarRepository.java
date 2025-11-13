package com.goormi.routine.domain.calendar.repository;

import com.goormi.routine.domain.calendar.entity.UserCalendar;
import com.goormi.routine.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarRepository extends JpaRepository<UserCalendar, Long> {

    Optional<UserCalendar> findByUserId(Long userId);
    Optional<UserCalendar> findByUserIdAndActiveTrue(Long userId);

    boolean existsByUser(User user);
    boolean existsByUserIdAndActiveTrue(Long userId);
}
