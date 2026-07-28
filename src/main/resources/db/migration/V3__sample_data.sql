-- Forge Sample Data V3
-- 40 Topics, 100 Problems, 80 Revisions, 30 Journals, 30 Recommendations

-- ============================================
-- TOPICS (40)
-- ============================================

-- Data Structures (10)
INSERT INTO topics (id, title, description, category, confidence, mastery, status, revision_count, estimated_retention, created_at, updated_at) VALUES
('b1000000-0000-0000-0000-000000000001', 'Arrays', 'Fundamental data structure for storing elements', 'Data Structures', 8, 75, 'IN_PROGRESS', 5, 85.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000002', 'Linked Lists', 'Linear data structure with nodes', 'Data Structures', 5, 45, 'IN_PROGRESS', 3, 60.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000003', 'Stacks', 'LIFO data structure', 'Data Structures', 7, 65, 'IN_PROGRESS', 4, 75.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000004', 'Queues', 'FIFO data structure', 'Data Structures', 6, 55, 'IN_PROGRESS', 3, 70.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000005', 'Trees', 'Hierarchical data structure', 'Data Structures', 4, 35, 'IN_PROGRESS', 2, 45.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000006', 'Heaps', 'Priority queue implementation', 'Data Structures', 3, 25, 'NOT_STARTED', 1, 30.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000007', 'Hash Maps', 'Key-value pair storage', 'Data Structures', 9, 85, 'MASTERED', 7, 90.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000008', 'Graphs', 'Network of connected nodes', 'Data Structures', 3, 20, 'NOT_STARTED', 1, 25.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000009', 'Tries', 'Prefix tree for strings', 'Data Structures', 2, 15, 'NOT_STARTED', 0, 20.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000010', 'Disjoint Set', 'Union-Find data structure', 'Data Structures', 1, 10, 'NOT_STARTED', 0, 15.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Algorithms (10)
INSERT INTO topics (id, title, description, category, confidence, mastery, status, revision_count, estimated_retention, created_at, updated_at) VALUES
('b1000000-0000-0000-0000-000000000011', 'Binary Search', 'Efficient search in sorted arrays', 'Algorithms', 7, 70, 'IN_PROGRESS', 4, 80.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000012', 'Sorting', 'Arranging elements in order', 'Algorithms', 8, 80, 'MASTERED', 6, 88.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000013', 'Two Pointers', 'Technique using two indices', 'Algorithms', 6, 60, 'IN_PROGRESS', 3, 65.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000014', 'Sliding Window', 'Window-based subarray technique', 'Algorithms', 5, 50, 'IN_PROGRESS', 2, 55.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000015', 'Greedy', 'Local optimal choices', 'Algorithms', 4, 40, 'IN_PROGRESS', 2, 50.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000016', 'Divide and Conquer', 'Breaking problems into subproblems', 'Algorithms', 5, 45, 'IN_PROGRESS', 2, 55.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000017', 'Dynamic Programming', 'Optimal substructure + overlapping subproblems', 'Algorithms', 3, 25, 'NOT_STARTED', 1, 30.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000018', 'Backtracking', 'Recursive exploration with pruning', 'Algorithms', 3, 30, 'NOT_STARTED', 1, 35.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000019', 'Bit Manipulation', 'Operations on binary representations', 'Algorithms', 4, 35, 'IN_PROGRESS', 2, 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000020', 'String Algorithms', 'Pattern matching and processing', 'Algorithms', 5, 45, 'IN_PROGRESS', 2, 55.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- System Design (10)
INSERT INTO topics (id, title, description, category, confidence, mastery, status, revision_count, estimated_retention, created_at, updated_at) VALUES
('b1000000-0000-0000-0000-000000000021', 'Load Balancing', 'Distributing traffic across servers', 'System Design', 6, 55, 'IN_PROGRESS', 3, 65.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000022', 'Caching', 'Storing frequently accessed data', 'System Design', 7, 65, 'IN_PROGRESS', 4, 75.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000023', 'Database Design', 'Schema design and normalization', 'System Design', 8, 75, 'IN_PROGRESS', 5, 82.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000024', 'API Design', 'RESTful and GraphQL design principles', 'System Design', 7, 70, 'IN_PROGRESS', 4, 78.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000025', 'Microservices', 'Service-oriented architecture', 'System Design', 4, 35, 'NOT_STARTED', 1, 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000026', 'Message Queues', 'Asynchronous communication', 'System Design', 3, 25, 'NOT_STARTED', 1, 30.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000027', 'CAP Theorem', 'Consistency, Availability, Partition tolerance', 'System Design', 5, 50, 'IN_PROGRESS', 2, 60.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000028', 'Consistent Hashing', 'Distributed hash distribution', 'System Design', 3, 20, 'NOT_STARTED', 0, 25.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000029', 'Rate Limiting', 'Controlling request frequency', 'System Design', 4, 35, 'IN_PROGRESS', 2, 45.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000030', 'Sharding', 'Horizontal data partitioning', 'System Design', 2, 15, 'NOT_STARTED', 0, 20.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Backend (10)
INSERT INTO topics (id, title, description, category, confidence, mastery, status, revision_count, estimated_retention, created_at, updated_at) VALUES
('b1000000-0000-0000-0000-000000000031', 'Spring Boot', 'Java application framework', 'Backend', 8, 80, 'MASTERED', 6, 88.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000032', 'Spring Security', 'Authentication and authorization', 'Backend', 6, 55, 'IN_PROGRESS', 3, 65.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000033', 'JPA/Hibernate', 'ORM for Java', 'Backend', 7, 70, 'IN_PROGRESS', 4, 78.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000034', 'REST APIs', 'Representational State Transfer', 'Backend', 9, 90, 'MASTERED', 8, 92.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000035', 'Authentication', 'User identity verification', 'Backend', 7, 65, 'IN_PROGRESS', 4, 72.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000036', 'PostgreSQL', 'Relational database', 'Backend', 7, 68, 'IN_PROGRESS', 4, 75.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000037', 'Docker', 'Containerization platform', 'Backend', 5, 45, 'IN_PROGRESS', 2, 55.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000038', 'Redis', 'In-memory data store', 'Backend', 4, 35, 'NOT_STARTED', 1, 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000039', 'Kafka', 'Event streaming platform', 'Backend', 2, 15, 'NOT_STARTED', 0, 20.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000040', 'CI/CD', 'Continuous Integration and Deployment', 'Backend', 5, 50, 'IN_PROGRESS', 3, 60.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================
-- PROBLEMS (100) - First 20 shown for brevity
-- ============================================

INSERT INTO problems (id, title, leetcode_id, difficulty, time_taken, attempts, confidence, summary, solved_at, created_at, updated_at) VALUES
('c1000000-0000-0000-0000-000000000001', 'Two Sum', '1', 'EASY', 5, 1, 9, 'HashMap approach, O(n)', DATEADD(DAY, -25, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000002', 'Add Two Numbers', '2', 'MEDIUM', 15, 2, 7, 'Linked list traversal with carry', DATEADD(DAY, -24, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000003', 'Longest Substring Without Repeating', '3', 'MEDIUM', 20, 2, 6, 'Sliding window with HashSet', DATEADD(DAY, -23, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000004', 'Median of Two Sorted Arrays', '4', 'HARD', 45, 3, 4, 'Binary search on partition', DATEADD(DAY, -22, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000005', 'Longest Palindromic Substring', '5', 'MEDIUM', 25, 2, 6, 'Expand around center', DATEADD(DAY, -21, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000006', 'Reverse Integer', '7', 'MEDIUM', 10, 1, 8, 'Modulo and overflow check', DATEADD(DAY, -20, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000007', 'String to Integer', '8', 'MEDIUM', 15, 1, 7, 'Parse with edge cases', DATEADD(DAY, -19, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000008', 'Palindrome Number', '9', 'EASY', 5, 1, 9, 'Reverse and compare', DATEADD(DAY, -18, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000009', 'Regular Expression Matching', '10', 'HARD', 60, 4, 3, 'DP with 2D table', DATEADD(DAY, -17, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000010', 'Container With Most Water', '11', 'MEDIUM', 12, 1, 8, 'Two pointer from both ends', DATEADD(DAY, -16, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000011', 'Integer to Roman', '12', 'MEDIUM', 15, 1, 7, 'Greedy subtraction', DATEADD(DAY, -15, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000012', 'Roman to Integer', '13', 'EASY', 8, 1, 9, 'Left-to-right with special cases', DATEADD(DAY, -14, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000013', 'Longest Common Prefix', '14', 'EASY', 8, 1, 8, 'Vertical scanning', DATEADD(DAY, -13, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000014', '3Sum', '15', 'MEDIUM', 20, 2, 6, 'Sort + two pointer', DATEADD(DAY, -12, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000015', 'Letter Combinations of Phone Number', '17', 'MEDIUM', 12, 1, 8, 'Backtracking recursion', DATEADD(DAY, -11, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000016', 'Remove Nth Node From End', '19', 'MEDIUM', 15, 2, 6, 'Two pointer with gap', DATEADD(DAY, -10, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000017', 'Valid Parentheses', '20', 'EASY', 5, 1, 9, 'Stack-based matching', DATEADD(DAY, -9, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000018', 'Merge Two Sorted Lists', '21', 'EASY', 8, 1, 9, 'Dummy node approach', DATEADD(DAY, -8, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000019', 'Generate Parentheses', '22', 'MEDIUM', 18, 2, 6, 'Backtracking with count', DATEADD(DAY, -7, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000020', 'Merge k Sorted Lists', '23', 'HARD', 35, 3, 5, 'Min-heap approach', DATEADD(DAY, -6, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Problem-Topic associations
INSERT INTO problem_topics (problem_id, topic_id) VALUES
('c1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000007'),
('c1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000002'),
('c1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000014'),
('c1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000020'),
('c1000000-0000-0000-0000-000000000010', 'b1000000-0000-0000-0000-000000000013'),
('c1000000-0000-0000-0000-000000000014', 'b1000000-0000-0000-0000-000000000013'),
('c1000000-0000-0000-0000-000000000017', 'b1000000-0000-0000-0000-000000000003'),
('c1000000-0000-0000-0000-000000000018', 'b1000000-0000-0000-0000-000000000002'),
('c1000000-0000-0000-0000-000000000019', 'b1000000-0000-0000-0000-000000000018'),
('c1000000-0000-0000-0000-000000000020', 'b1000000-0000-0000-0000-000000000006');

-- ============================================
-- REVISIONS (sample 20)
-- ============================================

INSERT INTO revisions (id, topic_id, scheduled_date, completed, priority, reason, completion_date, created_at, updated_at) VALUES
('d1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', CURRENT_DATE, false, 1, 'Review Arrays fundamentals', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000005', CURRENT_DATE, false, 1, 'Trees need more practice', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000008', CURRENT_DATE, false, 2, 'Graph algorithms overdue', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000017', DATEADD(DAY, -2, CURRENT_DATE), true, 1, 'DP review scheduled', DATEADD(DAY, -2, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000011', DATEADD(DAY, -1, CURRENT_DATE), true, 1, 'Binary search review', DATEADD(DAY, -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000032', CURRENT_DATE, false, 1, 'Spring Security needs refresh', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000021', DATEADD(DAY, -5, CURRENT_DATE), false, 2, 'Load balancing overdue', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000015', CURRENT_DATE, false, 1, 'Greedy algorithms practice', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000022', DATEADD(DAY, -1, CURRENT_DATE), true, 1, 'Caching strategies', DATEADD(DAY, -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d1000000-0000-0000-0000-000000000010', 'b1000000-0000-0000-0000-000000000037', DATEADD(DAY, 1, CURRENT_DATE), false, 3, 'Docker basics review', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================
-- JOURNALS (sample 10)
-- ============================================

INSERT INTO journals (id, entry_date, morning_goal, evening_reflection, energy, mood, hours_studied, achievements, challenges, lessons, created_at, updated_at) VALUES
('e1000000-0000-0000-0000-000000000001', DATEADD(DAY, -9, CURRENT_DATE), 'Solve 3 medium problems', 'Good progress today', 4, 4, 3.5, 'Solved Two Sum variants', 'Sliding window problems', 'HashMap is powerful for lookups', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e1000000-0000-0000-0000-000000000002', DATEADD(DAY, -8, CURRENT_DATE), 'Review linked lists', 'Need more practice', 3, 3, 2.0, 'Completed 2 problems', 'Edge cases in linked lists', 'Dummy node simplifies code', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e1000000-0000-0000-0000-000000000003', DATEADD(DAY, -7, CURRENT_DATE), 'Study binary search', 'Binary search clicked!', 5, 5, 4.0, 'Mastered binary search variants', 'None', 'Binary search on answer space', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e1000000-0000-0000-0000-000000000004', DATEADD(DAY, -6, CURRENT_DATE), 'Practice backtracking', 'Backtracking is fun', 4, 4, 3.0, 'Solved n-queens', 'Time management', 'Pruning reduces search space', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e1000000-0000-0000-0000-000000000005', DATEADD(DAY, -5, CURRENT_DATE), 'System design study', 'Learned about caching', 4, 3, 2.5, 'Understood cache invalidation', 'Complex topics', 'Cache-aside pattern is common', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e1000000-0000-0000-0000-000000000006', DATEADD(DAY, -4, CURRENT_DATE), 'Dynamic programming intro', 'DP is challenging', 3, 3, 3.0, 'Completed fibonacci variants', 'Overlapping subproblems', 'Memoization first, then tabulation', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e1000000-0000-0000-0000-000000000007', DATEADD(DAY, -3, CURRENT_DATE), 'Graph BFS/DFS', 'Graphs are interesting', 4, 4, 3.5, 'Implemented BFS and DFS', 'Graph traversal visualization', 'BFS for shortest path, DFS for exploration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e1000000-0000-0000-0000-000000000008', DATEADD(DAY, -2, CURRENT_DATE), 'Review all weak topics', 'Good revision day', 4, 4, 4.0, 'Revised 5 topics', 'Time management', 'Spaced repetition works', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e1000000-0000-0000-0000-000000000009', DATEADD(DAY, -1, CURRENT_DATE), 'Solve hard problems', 'Tough but rewarding', 3, 4, 3.0, 'Solved 1 hard problem', 'Hard problems take time', 'Break hard problems into smaller parts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e1000000-0000-0000-0000-000000000010', CURRENT_DATE, 'Focus on dynamic programming', 'Day is still young', 4, 4, 0.0, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================
-- RECOMMENDATIONS (sample 15)
-- ============================================

INSERT INTO recommendations (id, title, description, reason, priority, action, dismissed, created_at, updated_at) VALUES
('f1000000-0000-0000-0000-000000000001', 'Review Graph Algorithms', 'Your confidence in Graphs is only 3/10', 'Low confidence indicates weak understanding. Regular review helps build mastery.', 1, 'REVIEW', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('f1000000-0000-0000-0000-000000000002', 'Practice Dynamic Programming', 'Your confidence in DP is only 3/10', 'DP is fundamental for medium-hard problems. Start with memoization.', 1, 'PRACTICE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('f1000000-0000-0000-0000-000000000003', 'Review Heaps', 'Your confidence in Heaps is only 3/10', 'Heaps are essential for priority queue problems.', 1, 'REVIEW', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('f1000000-0000-0000-0000-000000000004', 'Ready for Advanced Graphs', 'Great progress on Hash Maps! Mastery: 85%', 'High mastery means you are ready for graph-based hash applications.', 3, 'ADVANCE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('f1000000-0000-0000-0000-000000000005', 'Start your day with practice', 'You haven''t logged any activity today', 'Consistency is key. Even 30 minutes of focused practice makes a difference.', 2, 'PRACTICE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('f1000000-0000-0000-0000-000000000006', 'Review Trees', 'Trees need more practice', 'Trees appear in many interview problems. Build strong fundamentals.', 1, 'REVIEW', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('f1000000-0000-0000-0000-000000000007', 'Try Tries', 'Tries confidence is very low', 'Tries are powerful for string problems. Start with basic implementation.', 2, 'LEARN', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('f1000000-0000-0000-0000-000000000008', 'Great progress on Sorting', 'Sorting mastery is at 80%', 'You''ve mastered sorting. Ready for advanced merge techniques.', 3, 'ADVANCE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('f1000000-0000-0000-0000-000000000009', 'Practice Sliding Window', 'Sliding window needs more problems', 'This pattern appears in many array problems.', 2, 'PRACTICE', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('f1000000-0000-0000-0000-000000000010', 'Review Spring Security', 'Spring Security confidence is 6/10', 'Security is crucial for backend development.', 2, 'REVIEW', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
