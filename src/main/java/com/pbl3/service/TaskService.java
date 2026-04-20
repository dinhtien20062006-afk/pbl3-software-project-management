package com.pbl3.service;

import com.pbl3.dto.request.*;
import com.pbl3.dto.response.ShowTaskResponse;
import com.pbl3.entity.*;
import com.pbl3.repository.*;
import lombok.RequiredArgsConstructor;
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
    private final NotificationService notificationService;

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

    public List<ShowTaskResponse> findTasksByStatus(String status) {
    try {        TaskStatus s = TaskStatus.valueOf(status.toUpperCase());
        return taskRepository.findByStatus(s).stream()
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

    // 1. Cập nhật các trường thông thường
    if (request.getTaskName() != null) task.setTaskName(request.getTaskName());
    if (request.getDescription() != null) task.setDescription(request.getDescription());
    if (request.getStartDate() != null) task.setStartDate(request.getStartDate());
    if (request.getDeadline() != null) task.setDeadline(request.getDeadline());
    
    // 2. Cập nhật Enum (nếu có)
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

    // 3. Chỉ lưu vào DB một lần duy nhất
    Task updatedTask = taskRepository.save(task);
    ShowTaskResponse response = mapToResponse(updatedTask);

    // 4. Gửi thông báo (nếu có assignee)
    if (task.getAssignee() != null) {
        notificationService.sendNotification(
            task.getAssignee(),
            "Task đã được cập nhật",
            "Task " + updatedTask.getTaskName() + " vừa thay đổi trạng thái hoặc thông tin.",
            NotificationType.TASK_UPDATED
        );
    }

    return response;
}
    public void submitTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_EXISTED));

        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            task.setStatus(TaskStatus.PENDING_APPROVAL);
            taskRepository.save(task);

            // Gửi thông báo vào DB để member/leader thấy
            notificationService.sendNotification(
                task.getAssignee(), // Người được giao hoặc chủ dự án
                "Task đã được nộp", 
                "Task " + task.getTaskName() + " vừa được chuyển sang trạng thái chờ duyệt.",
                NotificationType.DEADLINE_REMINDER // Hoặc loại bạn tự định nghĩa
            );
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
