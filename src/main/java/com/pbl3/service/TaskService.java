package com.pbl3.service;

import com.pbl3.entity.Comment;
import com.pbl3.dto.request.*;
import com.pbl3.dto.response.ShowTaskResponse;
import com.pbl3.entity.*;
import com.pbl3.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pbl3.exception.AppException;
import com.pbl3.exception.ErrorCode;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final CommentRepository commentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Lấy danh sách Task của một Project
    public List<ShowTaskResponse> getTasksByProjectId(Long projectId) {
        // Kiểm tra project có tồn tại không trước khi lấy task
        if (!projectRepository.existsById(projectId)) {
            throw new AppException(ErrorCode.PROJECT_NOT_EXISTED);
        }
        
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        return tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ShowTaskResponse mapToResponse(Task task) {
        return ShowTaskResponse.builder()
                .id(task.getId())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .deadLine(task.getDeadline())
                .build();
    }

    public Task createTask(TaskCreateRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_EXISTED));

        try {
            Task task = Task.builder()
                    .taskName(request.getTaskName())
                    .description(request.getDescription())
                    .deadline(request.getDeadline())
                    .project(project)
                    .priority(TaskPriority.valueOf(request.getPriority().toUpperCase()))
                    .status(TaskStatus.valueOf(request.getStatus().toUpperCase()))
                    .build();

            return taskRepository.save(task);
        } catch (IllegalArgumentException e) {
            // Lỗi khi String gửi lên không khớp với Enum Status/Priority
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    public void deleteTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new AppException(ErrorCode.TASK_NOT_EXISTED);
        }
        taskRepository.deleteById(taskId);
    }

    public ShowTaskResponse updateTask(Long taskId, TaskUpdateRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        task.setTaskName(request.getTaskName());
        task.setDescription(request.getDescription());
        task.setStartDate(request.getStartDate());
        task.setDeadline(request.getDeadline());
        
        try {
            if (request.getPriority() != null) {
                task.setPriority(TaskPriority.valueOf(request.getPriority().toUpperCase()));
            }
            if (request.getStatus() != null) {
                task.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));
            }
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        return mapToResponse(taskRepository.save(task)); 
    }

    @Transactional
    public void addComment(CommentRequest request) {
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        // Giả sử bạn muốn kiểm tra nội dung comment không được trống
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .task(task)
                .build();
        commentRepository.save(comment);

        // WebSocket Notify
        String topic = "/topic/task/" + request.getTaskId();
        messagingTemplate.convertAndSend(topic, "Người dùng " + request.getUserName() + " vừa bình luận.");

        if (task.getProject().getManager() != null) {
            String ownerUsername = task.getProject().getManager().getUsername();
            messagingTemplate.convertAndSendToUser(
                ownerUsername, 
                "/queue/notifications", 
                "Task '" + task.getTaskName() + "' có bình luận mới."
            );
        }
    }
}