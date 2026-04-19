package com.pbl3.service;

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
    //Xap xep task theo priority
    public List<ShowTaskResponse> getAllTasksSortedByPriority() {
    return taskRepository.findAll().stream()
            .sorted((t1, t2) -> t2.getPriority().compareTo(t1.getPriority())) // Sắp xếp giảm dần
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    // Lấy danh sách Task cua 1 nguoi duoc giao
    public List<ShowTaskResponse> getTasksByAssignee(Long userId) {
    // Giả sử bạn đã viết findByAssigneeId trong TaskRepository
    List<Task> tasks = taskRepository.findByAssigneeId(userId);
    return tasks.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<ShowTaskResponse> getTasksByPriority(String priority) {
    try {
        TaskPriority p = TaskPriority.valueOf(priority.toUpperCase());
        return taskRepository.findByPriority(p).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    } catch (IllegalArgumentException e) {
        throw new AppException(ErrorCode.INVALID_KEY);
        }
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
    public void submitTask(Long taskId) {
    Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

    // Chỉ cho phép gửi khi đang ở trạng thái xử lý
    if (task.getStatus() == TaskStatus.IN_PROGRESS) {
        task.setStatus(TaskStatus.PENDING_APPROVAL);
        taskRepository.save(task);

        // Gửi thông báo WebSocket cho Manager
        if (task.getProject().getManager() != null) {
            messagingTemplate.convertAndSendToUser(
                task.getProject().getManager().getUsername(),
                "/queue/notifications",
                "Member đã hoàn thành Task: " + task.getTaskName() + ". Đang chờ bạn duyệt!"
            );
        }
    }
}

    @Transactional
    public void reviewTask(Long taskId, boolean approved) {
    Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

    if (approved) {
        task.setStatus(TaskStatus.DONE);
    } else {
        task.setStatus(TaskStatus.IN_PROGRESS); // Trả về để làm lại
    }
    
    taskRepository.save(task);

    // Thông báo lại cho Member biết kết quả
    if (task.getAssignee() != null) {
        messagingTemplate.convertAndSendToUser(
            task.getAssignee().getUsername(),
            "/queue/notifications",
            approved ? "Task của bạn đã được duyệt!" : "Task của bạn bị từ chối, vui lòng kiểm tra lại."
        );
    }
}
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
    @Transactional
    public void updateComment(Long commentId, String newContent) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY)); // Hoặc COMMENT_NOT_EXISTED
        
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        comment.setContent(newContent);
        commentRepository.save(comment);
    }

    // 2. Xóa Comment
    @Transactional
    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        commentRepository.deleteById(commentId);
    }

    // --- DỌN DẸP VÀ TỐI ƯU CÁC HÀM SHOW ---

    // Lấy chi tiết 1 Task (Hàm Show Task)
    public ShowTaskResponse getTaskById(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));
        return mapToResponse(task);
    }

    // 1. Lấy tất cả bình luận của một Task
    public List<Comment> getCommentsByTaskId(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new AppException(ErrorCode.TASK_NOT_EXISTED);
        }
        // Giả sử trong CommentRepository bạn có hàm findByTask_Id(Long taskId)
        return commentRepository.findByTask_Id(taskId);
    }

    // 2. Lấy bình luận của User
    public List<Comment> showCommentsByUserId(Long userId) {
        // Hãy chắc chắn CommentRepository có hàm này
        return commentRepository.findByUser_Id(userId);
    }

    // 3. FIX LỖI: Hàm này trước đó trả về List<Task> sai kiểu, 
    // nếu bạn muốn lấy task thì nên đặt tên là getTaskById
    public Task getTaskByIdRaw(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));
    }

    // 4. Hàm đếm status
    public Long countByStatus(Long projectId, String status) {
        return taskRepository.countByStatus(projectId, status);
    }
}
