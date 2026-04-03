package com.pbl3.service;

import com.pbl3.dto.request.TaskCreateRequest;
import com.pbl3.dto.response.ShowTaskResponse;
import com.pbl3.entity.Project;
import com.pbl3.entity.Task;
import com.pbl3.entity.TaskPriority;
import com.pbl3.entity.TaskStatus;
import com.pbl3.repository.ProjectRepository;
import com.pbl3.repository.TaskRepository;
import org.springframework.stereotype.Service;
import com.pbl3.dto.request.TaskUpdateRequest;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }
    public class AppException extends RuntimeException {
    // Bạn có thể thêm errorCode tại đây nếu muốn
    public AppException(String message) {
        super(message);
    }
    }

    // Lấy danh sách Task của một Project
    public List<ShowTaskResponse> getTasksByProjectId(Long projectId) {
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        
        return tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Hàm phụ trợ để map từ Entity sang DTO
    private ShowTaskResponse mapToResponse(Task task) {
        return ShowTaskResponse.builder()
                .id(task.getId())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .status(task.getStatus().name())     // Chuyển Enum sang String
                .priority(task.getPriority().name()) // Chuyển Enum sang String
                .deadLine(task.getDeadline())
                .build();
    }
    public Task createTask(TaskCreateRequest request) {
        // 1. Tìm Project theo ID gửi lên
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new AppException("Không tìm thấy dự án với ID: " + request.getProjectId()));

        // 2. Build Entity từ Request
        Task task = Task.builder()
                .taskName(request.getTaskName())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .project(project)
                // Convert String từ Request sang Enum trong Entity
                .priority(TaskPriority.valueOf(request.getPriority().toUpperCase()))
                .status(TaskStatus.valueOf(request.getStatus().toUpperCase()))
                .build();

        // 3. Lưu vào DB
        return taskRepository.save(task);
    }
    public void deleteTask(Long taskId) {
        // 1. Kiểm tra xem Task có tồn tại không
        if (!taskRepository.existsById(taskId)) {
            throw new AppException("Không tìm thấy task với ID: " + taskId);
        }
        // 2. Xóa Task
        taskRepository.deleteById(taskId);
    }

    public ShowTaskResponse updateTask(Long taskId, TaskUpdateRequest request) {
        // 1. Tìm Task theo ID
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException("Không tìm thấy task với ID: " + taskId));

        // 2. Cập nhật thông tin từ TaskUpdateRequest
        // Sử dụng helper method để tránh lỗi nếu Frontend gửi String sai định dạng Enum
        task.setTaskName(request.getTaskName());
        task.setDescription(request.getDescription());
        task.setStartDate(request.getStartDate());
        task.setDeadline(request.getDeadline());
        
        // Cập nhật Enum an toàn
        if (request.getPriority() != null) {
            task.setPriority(TaskPriority.valueOf(request.getPriority().toUpperCase()));
        }
        
        if (request.getStatus() != null) {
            task.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));
        }

        // 3. Lưu vào DB và trả về DTO (ShowTaskResponse)
        Task savedTask = taskRepository.save(task);
        return mapToResponse(savedTask); 
    }
}