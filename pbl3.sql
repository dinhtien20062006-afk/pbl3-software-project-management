		-- 1. Tạo Database
CREATE DATABASE IF NOT EXISTS project_management_db;
USE project_management_db;

-- 2. Bảng Vai trò hệ thống (Role cho Spring Security)
CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE -- ADMIN, PROJECT_MANAGER, MEMBER
);

-- 3. Bảng Người dùng
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Lưu BCrypt hash
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    avatar_url VARCHAR(255),
    role_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE SET NULL
);

-- 4. Bảng Dự án
CREATE TABLE projects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) DEFAULT 'PLANNING', -- PLANNING, ONGOING, COMPLETED, ON_HOLD
    git_repository_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Bảng Trung gian: Thành viên dự án (Bổ sung project_role)
CREATE TABLE project_members (
    project_id INT,
    user_id INT,
    project_role VARCHAR(50), -- Developer, Tester, Lead, v.v.
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 6. Bảng Công việc (Task)
CREATE TABLE tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(20), -- LOW, MEDIUM, HIGH, URGENT
    status VARCHAR(50) DEFAULT 'TODO', -- TODO, IN_PROGRESS, REVIEW, DONE
    deadline DATETIME,
    project_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- 7. Bảng Trung gian: Phân công Task (Một task có thể có nhiều người làm)
CREATE TABLE task_assignments (
    task_id INT,
    user_id INT,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, user_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 8. Bảng Bình luận (Comment)
CREATE TABLE comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    task_id INT,
    user_id INT,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- 10. Bảng Thông báo
CREATE TABLE notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    title VARCHAR(255),
    content TEXT,
    type VARCHAR(50), -- TASK_ASSIGNED, DEADLINE_REMINDER, SYSTEM
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 11. Chèn dữ liệu mẫu cho Roles (Dùng cho thiết lập ban đầu)
INSERT INTO roles (role_name) VALUES ('ADMIN'), ('PROJECT_MANAGER'), ('MEMBER');
