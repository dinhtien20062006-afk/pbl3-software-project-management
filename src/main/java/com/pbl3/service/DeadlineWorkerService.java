package com.pbl3.service;

import com.pbl3.entity.NotificationType;
import com.pbl3.entity.Task;
import com.pbl3.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.pbl3.entity.User;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeadlineWorkerService {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    // Chạy vào 7:00 sáng mỗi ngày
    @Scheduled(cron = "0 0 7 * * *")
    public void checkDeadlines() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        
        // Giả sử bạn viết thêm hàm này trong TaskRepository
        List<Task> upcomingTasks = taskRepository.findAllByDeadlineBetween(
            LocalDateTime.now(), tomorrow
        );

       for (Task task : upcomingTasks) {
    User worker = task.getAssignee();
    if (worker != null) {
        notificationService.sendNotification(
            worker, 
            "Sắp đến hạn công việc!", 
            "Task '" + task.getTaskName() + "' sẽ hết hạn vào " + task.getDeadline(),
            NotificationType.DEADLINE_REMINDER
            );
        }
    }
        }
    }