-- 테스트용 사이트 데이터 추가
-- 먼저 admin 테이블에서 ID 1인 관리자가 있는지 확인하고 없으면 추가
INSERT IGNORE INTO admins (id, username, email, name, password, role, is_active, created_at, updated_at) 
VALUES (1, 'testadmin', 'test@example.com', '테스트 관리자', '$2a$10$dummy.hash.for.test', 'ADMIN', true, NOW(), NOW());

-- 테스트 사이트들 추가 (admin ID 1이 소유)
INSERT IGNORE INTO sites (owner_id, site_name, domain, site_key, created_at, updated_at, is_active) VALUES
(1, '커뮤니티 사이트', 'community.example.com', 'community-site', NOW(), NOW(), true),
(1, '온라인 쇼핑몰', 'shop.example.com', 'shop-site', NOW(), NOW(), true),
(1, '개인 블로그', 'blog.example.com', 'blog-site', NOW(), NOW(), true);

-- 테스트 페이지들 추가
INSERT IGNORE INTO site_pages (site_id, page_id, page_name, page_description, page_type, comment_count, last_activity_at, created_at, updated_at, is_active) VALUES
('community-site', '/board/free', '자유게시판', '자유롭게 이야기하는 공간', 'BOARD', 142, NOW(), NOW(), NOW(), true),
('community-site', '/board/notice', '공지사항', '중요한 공지사항', 'BOARD', 28, NOW(), NOW(), NOW(), true),
('shop-site', '/product/list', '상품목록', '전체 상품 목록', 'PRODUCT', 64, NOW(), NOW(), NOW(), true),
('shop-site', '/product/reviews', '상품후기', '상품 후기 게시판', 'BOARD', 89, NOW(), NOW(), NOW(), true),
('blog-site', '/posts', '블로그 글', '블로그 포스트', 'ARTICLE', 25, NOW(), NOW(), NOW(), true);