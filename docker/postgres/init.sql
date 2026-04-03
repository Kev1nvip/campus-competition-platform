-- =============================================
-- 校园学术竞赛管理平台 数据库初始化脚本
-- 数据库：PostgreSQL 16 + PGVector
-- 创建日期：2026-04
-- 说明：首次启动时自动执行，重复执行安全（幂等）
-- =============================================

-- 启用PGVector扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- =============================================
-- 通用触发器函数：自动更新 updated_at 字段
-- =============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- 1. 用户表 sys_user
-- =============================================
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGSERIAL       PRIMARY KEY,
    username    VARCHAR(64)     NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    real_name   VARCHAR(32)     NOT NULL,
    role        VARCHAR(16)     NOT NULL,
    phone       VARCHAR(20)     DEFAULT NULL,
    email       VARCHAR(128)    DEFAULT NULL,
    student_no  VARCHAR(32)     DEFAULT NULL,
    department  VARCHAR(64)     DEFAULT NULL,
    title       VARCHAR(32)     DEFAULT NULL,
    avatar_url  VARCHAR(512)    DEFAULT NULL,
    status      VARCHAR(16)     NOT NULL        DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ     NOT NULL        DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL        DEFAULT NOW(),

    CONSTRAINT chk_sys_user_role
        CHECK (role IN ('STUDENT', 'TEACHER', 'ADMIN')),
    CONSTRAINT chk_sys_user_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

COMMENT ON TABLE  sys_user            IS '用户表';
COMMENT ON COLUMN sys_user.username   IS '用户名，全局唯一';
COMMENT ON COLUMN sys_user.password   IS 'BCrypt加密密码';
COMMENT ON COLUMN sys_user.role       IS '角色：STUDENT/TEACHER/ADMIN';
COMMENT ON COLUMN sys_user.student_no IS '学号，学生必填，全局唯一';
COMMENT ON COLUMN sys_user.title      IS '职称，老师专属，如讲师/副教授/教授';
COMMENT ON COLUMN sys_user.status     IS '账号状态：ACTIVE正常/DISABLED禁用';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username
    ON sys_user(username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_student_no
    ON sys_user(student_no)
    WHERE student_no IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sys_user_role
    ON sys_user(role);

CREATE TRIGGER trg_sys_user_updated_at
    BEFORE UPDATE ON sys_user
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 2. 竞赛表 competition
-- =============================================
CREATE TABLE IF NOT EXISTS competition (
    id                  BIGSERIAL       PRIMARY KEY,
    title               VARCHAR(128)    NOT NULL,
    type                VARCHAR(16)     NOT NULL,
    organizer           VARCHAR(128)    NOT NULL,
    requirement         TEXT            DEFAULT NULL,
    signup_start        TIMESTAMPTZ     NOT NULL,
    signup_end          TIMESTAMPTZ     NOT NULL,
    competition_start   TIMESTAMPTZ     DEFAULT NULL,
    competition_end     TIMESTAMPTZ     DEFAULT NULL,
    has_quota           BOOLEAN         NOT NULL    DEFAULT FALSE,
    max_quota           INT             DEFAULT NULL,
    enrolled_count      INT             NOT NULL    DEFAULT 0,
    min_team_size       INT             DEFAULT NULL,
    max_team_size       INT             DEFAULT NULL,
    max_teach_quota     INT             DEFAULT NULL,
    description         TEXT            DEFAULT NULL,
    attachment_url      VARCHAR(512)    DEFAULT NULL,
    status              VARCHAR(16)     NOT NULL    DEFAULT 'UPCOMING',
    created_by          BIGINT          NOT NULL,
    version             INT             NOT NULL    DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT chk_competition_type
        CHECK (type IN ('INDIVIDUAL', 'TEAM')),
    CONSTRAINT chk_competition_status
        CHECK (status IN ('UPCOMING', 'SIGNING', 'CLOSED',
                          'ONGOING', 'FINISHED', 'OFFLINE')),
    CONSTRAINT chk_competition_quota
        CHECK (has_quota = FALSE OR max_quota IS NOT NULL),
    CONSTRAINT chk_competition_signup_time
        CHECK (signup_end > signup_start),
    CONSTRAINT chk_competition_enrolled
        CHECK (enrolled_count >= 0)
);

COMMENT ON TABLE  competition                  IS '竞赛表';
COMMENT ON COLUMN competition.type             IS '竞赛类型：INDIVIDUAL个人赛/TEAM团队赛';
COMMENT ON COLUMN competition.has_quota        IS '是否有名额限制';
COMMENT ON COLUMN competition.max_quota        IS '名额上限，has_quota为TRUE时必填';
COMMENT ON COLUMN competition.enrolled_count   IS '已报名数量，由Redis计数器维护并定期同步';
COMMENT ON COLUMN competition.min_team_size    IS '最少队伍人数，团队赛必填';
COMMENT ON COLUMN competition.max_team_size    IS '最多队伍人数，团队赛必填';
COMMENT ON COLUMN competition.max_teach_quota  IS '每位老师最多带队数，NULL表示不限制';
COMMENT ON COLUMN competition.created_by       IS '发布人ID，关联sys_user.id';
COMMENT ON COLUMN competition.version          IS '乐观锁版本号，每次更新+1';

CREATE INDEX IF NOT EXISTS idx_competition_status
    ON competition(status);
CREATE INDEX IF NOT EXISTS idx_competition_type
    ON competition(type);
CREATE INDEX IF NOT EXISTS idx_competition_created_by
    ON competition(created_by);
CREATE INDEX IF NOT EXISTS idx_competition_signup_end
    ON competition(signup_end);

CREATE TRIGGER trg_competition_updated_at
    BEFORE UPDATE ON competition
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 3. 个人赛报名表 individual_signup
-- =============================================
CREATE TABLE IF NOT EXISTS individual_signup (
    id              BIGSERIAL       PRIMARY KEY,
    competition_id  BIGINT          NOT NULL,
    student_id      BIGINT          NOT NULL,
    teacher_id      BIGINT          NOT NULL,
    motivation      TEXT            DEFAULT NULL,
    introduction    TEXT            DEFAULT NULL,
    status          VARCHAR(16)     NOT NULL    DEFAULT 'DRAFT',
    reject_reason   TEXT            DEFAULT NULL,
    submitted_at    TIMESTAMPTZ     DEFAULT NULL,
    created_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT chk_individual_signup_status
        CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED',
                          'REJECTED', 'RESUBMITTED'))
);

COMMENT ON TABLE  individual_signup               IS '个人赛报名表';
COMMENT ON COLUMN individual_signup.competition_id IS '竞赛ID，关联competition.id';
COMMENT ON COLUMN individual_signup.student_id     IS '学生ID，关联sys_user.id';
COMMENT ON COLUMN individual_signup.teacher_id     IS '指导老师ID，关联sys_user.id';
COMMENT ON COLUMN individual_signup.status         IS '报名状态：DRAFT/PENDING/APPROVED/REJECTED/RESUBMITTED';
COMMENT ON COLUMN individual_signup.reject_reason  IS '驳回原因，管理员填写';
COMMENT ON COLUMN individual_signup.submitted_at   IS '提交管理员审核的时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_individual_signup
    ON individual_signup(competition_id, student_id);
CREATE INDEX IF NOT EXISTS idx_individual_signup_student
    ON individual_signup(student_id);
CREATE INDEX IF NOT EXISTS idx_individual_signup_teacher
    ON individual_signup(teacher_id);
CREATE INDEX IF NOT EXISTS idx_individual_signup_status
    ON individual_signup(status);

CREATE TRIGGER trg_individual_signup_updated_at
    BEFORE UPDATE ON individual_signup
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 4. 队伍表 team
-- =============================================
CREATE TABLE IF NOT EXISTS team (
    id                  BIGSERIAL       PRIMARY KEY,
    competition_id      BIGINT          NOT NULL,
    team_name           VARCHAR(64)     NOT NULL,
    leader_id           BIGINT          NOT NULL,
    teacher_id          BIGINT          DEFAULT NULL,
    teacher_confirmed   BOOLEAN         NOT NULL    DEFAULT FALSE,
    member_count        INT             NOT NULL    DEFAULT 1,
    status              VARCHAR(16)     NOT NULL    DEFAULT 'FORMING',
    created_at          TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT chk_team_status
        CHECK (status IN ('FORMING', 'FULL', 'SUBMITTED',
                          'APPROVED', 'REJECTED', 'DISMISSED')),
    CONSTRAINT chk_team_member_count
        CHECK (member_count >= 1)
);

COMMENT ON TABLE  team                       IS '队伍表';
COMMENT ON COLUMN team.competition_id        IS '关联竞赛ID，关联competition.id';
COMMENT ON COLUMN team.leader_id             IS '队长ID，关联sys_user.id';
COMMENT ON COLUMN team.teacher_id            IS '指导老师ID，关联sys_user.id，可为NULL';
COMMENT ON COLUMN team.teacher_confirmed     IS '老师是否已确认带队，TRUE后才能发组队招募帖';
COMMENT ON COLUMN team.member_count          IS '当前成员数量，含队长';
COMMENT ON COLUMN team.status                IS '队伍状态：FORMING/FULL/SUBMITTED/APPROVED/REJECTED/DISMISSED';

CREATE INDEX IF NOT EXISTS idx_team_competition
    ON team(competition_id);
CREATE INDEX IF NOT EXISTS idx_team_leader
    ON team(leader_id);
CREATE INDEX IF NOT EXISTS idx_team_teacher
    ON team(teacher_id);
CREATE INDEX IF NOT EXISTS idx_team_status
    ON team(status);

CREATE TRIGGER trg_team_updated_at
    BEFORE UPDATE ON team
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 5. 队伍成员表 team_member
-- =============================================
CREATE TABLE IF NOT EXISTS team_member (
    id          BIGSERIAL       PRIMARY KEY,
    team_id     BIGINT          NOT NULL,
    student_id  BIGINT          NOT NULL,
    role        VARCHAR(16)     NOT NULL    DEFAULT 'MEMBER',
    joined_at   TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    created_at  TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT chk_team_member_role
        CHECK (role IN ('LEADER', 'MEMBER'))
);

COMMENT ON TABLE  team_member            IS '队伍成员表，记录用户和队伍的多对多关系';
COMMENT ON COLUMN team_member.team_id    IS '队伍ID，关联team.id';
COMMENT ON COLUMN team_member.student_id IS '学生ID，关联sys_user.id';
COMMENT ON COLUMN team_member.role       IS '成员角色：LEADER队长/MEMBER队员';
COMMENT ON COLUMN team_member.joined_at  IS '加入队伍时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_team_member
    ON team_member(team_id, student_id);
CREATE INDEX IF NOT EXISTS idx_team_member_team
    ON team_member(team_id);
CREATE INDEX IF NOT EXISTS idx_team_member_student
    ON team_member(student_id);

CREATE TRIGGER trg_team_member_updated_at
    BEFORE UPDATE ON team_member
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 6. 团队赛报名表 team_signup
-- =============================================
CREATE TABLE IF NOT EXISTS team_signup (
    id              BIGSERIAL       PRIMARY KEY,
    competition_id  BIGINT          NOT NULL,
    team_id         BIGINT          NOT NULL,
    teacher_id      BIGINT          NOT NULL,
    status          VARCHAR(16)     NOT NULL    DEFAULT 'PENDING',
    reject_reason   TEXT            DEFAULT NULL,
    submitted_at    TIMESTAMPTZ     DEFAULT NULL,
    created_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT chk_team_signup_status
        CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED',
                          'REJECTED', 'RESUBMITTED'))
);

COMMENT ON TABLE  team_signup               IS '团队赛报名表，一支队伍对应一条报名记录';
COMMENT ON COLUMN team_signup.competition_id IS '竞赛ID，关联competition.id';
COMMENT ON COLUMN team_signup.team_id        IS '队伍ID，关联team.id';
COMMENT ON COLUMN team_signup.teacher_id     IS '指导老师ID，关联sys_user.id';
COMMENT ON COLUMN team_signup.status         IS '报名状态，与individual_signup保持一致';
COMMENT ON COLUMN team_signup.reject_reason  IS '驳回原因，管理员填写';
COMMENT ON COLUMN team_signup.submitted_at   IS '提交管理员审核的时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_team_signup
    ON team_signup(competition_id, team_id);
CREATE INDEX IF NOT EXISTS idx_team_signup_team
    ON team_signup(team_id);
CREATE INDEX IF NOT EXISTS idx_team_signup_teacher
    ON team_signup(teacher_id);
CREATE INDEX IF NOT EXISTS idx_team_signup_status
    ON team_signup(status);

CREATE TRIGGER trg_team_signup_updated_at
    BEFORE UPDATE ON team_signup
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 7. 老师招募帖表 teacher_recruitment
-- =============================================
CREATE TABLE IF NOT EXISTS teacher_recruitment (
    id              BIGSERIAL       PRIMARY KEY,
    competition_id  BIGINT          NOT NULL,
    teacher_id      BIGINT          NOT NULL,
    recruit_count   INT             NOT NULL,
    current_count   INT             NOT NULL    DEFAULT 0,
    requirement     TEXT            DEFAULT NULL,
    deadline        TIMESTAMPTZ     DEFAULT NULL,
    status          VARCHAR(16)     NOT NULL    DEFAULT 'OPEN',
    created_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT chk_teacher_recruitment_status
        CHECK (status IN ('OPEN', 'FULL', 'CLOSED')),
    CONSTRAINT chk_teacher_recruitment_count
        CHECK (recruit_count >= 1 AND current_count >= 0)
);

COMMENT ON TABLE  teacher_recruitment                IS '老师招募帖表';
COMMENT ON COLUMN teacher_recruitment.competition_id IS '关联竞赛ID，关联competition.id';
COMMENT ON COLUMN teacher_recruitment.teacher_id     IS '发布老师ID，关联sys_user.id';
COMMENT ON COLUMN teacher_recruitment.recruit_count  IS '招募人数上限';
COMMENT ON COLUMN teacher_recruitment.current_count  IS '当前已加入人数';
COMMENT ON COLUMN teacher_recruitment.status         IS '状态：OPEN招募中/FULL已满/CLOSED已关闭';

CREATE INDEX IF NOT EXISTS idx_teacher_recruitment_competition
    ON teacher_recruitment(competition_id);
CREATE INDEX IF NOT EXISTS idx_teacher_recruitment_teacher
    ON teacher_recruitment(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_recruitment_status
    ON teacher_recruitment(status);

CREATE TRIGGER trg_teacher_recruitment_updated_at
    BEFORE UPDATE ON teacher_recruitment
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 8. 学生组队招募帖表 team_recruitment
-- =============================================
CREATE TABLE IF NOT EXISTS team_recruitment (
    id              BIGSERIAL       PRIMARY KEY,
    competition_id  BIGINT          NOT NULL,
    team_id         BIGINT          NOT NULL,
    leader_id       BIGINT          NOT NULL,
    recruit_count   INT             NOT NULL,
    current_count   INT             NOT NULL    DEFAULT 0,
    requirement     TEXT            DEFAULT NULL,
    deadline        TIMESTAMPTZ     DEFAULT NULL,
    status          VARCHAR(16)     NOT NULL    DEFAULT 'OPEN',
    created_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT chk_team_recruitment_status
        CHECK (status IN ('OPEN', 'FULL', 'CLOSED')),
    CONSTRAINT chk_team_recruitment_count
        CHECK (recruit_count >= 1 AND current_count >= 0)
);

COMMENT ON TABLE  team_recruitment                IS '学生组队招募帖表';
COMMENT ON COLUMN team_recruitment.competition_id IS '关联竞赛ID，关联competition.id';
COMMENT ON COLUMN team_recruitment.team_id        IS '关联队伍ID，关联team.id';
COMMENT ON COLUMN team_recruitment.leader_id      IS '队长ID，关联sys_user.id';
COMMENT ON COLUMN team_recruitment.recruit_count  IS '还需要几人';
COMMENT ON COLUMN team_recruitment.current_count  IS '当前已申请加入人数';
COMMENT ON COLUMN team_recruitment.status         IS '状态：OPEN招募中/FULL已满/CLOSED已关闭';

CREATE INDEX IF NOT EXISTS idx_team_recruitment_competition
    ON team_recruitment(competition_id);
CREATE INDEX IF NOT EXISTS idx_team_recruitment_team
    ON team_recruitment(team_id);
CREATE INDEX IF NOT EXISTS idx_team_recruitment_leader
    ON team_recruitment(leader_id);
CREATE INDEX IF NOT EXISTS idx_team_recruitment_status
    ON team_recruitment(status);

CREATE TRIGGER trg_team_recruitment_updated_at
    BEFORE UPDATE ON team_recruitment
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 9. 申请记录表 apply_record
-- =============================================
CREATE TABLE IF NOT EXISTS apply_record (
    id              BIGSERIAL       PRIMARY KEY,
    type            VARCHAR(32)     NOT NULL,
    applicant_id    BIGINT          NOT NULL,
    receiver_id     BIGINT          NOT NULL,
    biz_id          BIGINT          NOT NULL,
    introduction    TEXT            DEFAULT NULL,
    motivation      TEXT            DEFAULT NULL,
    status          VARCHAR(16)     NOT NULL    DEFAULT 'PENDING',
    reject_reason   TEXT            DEFAULT NULL,
    created_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT chk_apply_record_type
        CHECK (type IN ('INDIVIDUAL_GUIDE', 'TEAM_GUIDE',
                        'TEACHER_RECRUIT_APPLY',
                        'TEAM_RECRUIT_APPLY', 'TEAM_INVITE')),
    CONSTRAINT chk_apply_record_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

COMMENT ON TABLE  apply_record               IS '申请记录表，统一记录所有类型申请';
COMMENT ON COLUMN apply_record.type          IS '申请类型：INDIVIDUAL_GUIDE/TEAM_GUIDE/TEACHER_RECRUIT_APPLY/TEAM_RECRUIT_APPLY/TEAM_INVITE';
COMMENT ON COLUMN apply_record.applicant_id  IS '申请发起人ID，关联sys_user.id';
COMMENT ON COLUMN apply_record.receiver_id   IS '申请接收人ID，关联sys_user.id';
COMMENT ON COLUMN apply_record.biz_id        IS '关联业务ID，含义由type决定';
COMMENT ON COLUMN apply_record.status        IS '申请状态：PENDING待处理/APPROVED已同意/REJECTED已拒绝';

CREATE INDEX IF NOT EXISTS idx_apply_record_type_biz
    ON apply_record(type, biz_id);
CREATE INDEX IF NOT EXISTS idx_apply_record_applicant
    ON apply_record(applicant_id);
CREATE INDEX IF NOT EXISTS idx_apply_record_receiver
    ON apply_record(receiver_id);
CREATE INDEX IF NOT EXISTS idx_apply_record_status
    ON apply_record(status);

CREATE TRIGGER trg_apply_record_updated_at
    BEFORE UPDATE ON apply_record
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 10. 报名审核记录表 signup_audit
-- =============================================
CREATE TABLE IF NOT EXISTS signup_audit (
    id              BIGSERIAL       PRIMARY KEY,
    biz_type        VARCHAR(16)     NOT NULL,
    biz_id          BIGINT          NOT NULL,
    auditor_id      BIGINT          NOT NULL,
    result          VARCHAR(16)     NOT NULL,
    reject_reason   TEXT            DEFAULT NULL,
    created_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT chk_signup_audit_biz_type
        CHECK (biz_type IN ('INDIVIDUAL', 'TEAM')),
    CONSTRAINT chk_signup_audit_result
        CHECK (result IN ('APPROVED', 'REJECTED'))
);

COMMENT ON TABLE  signup_audit               IS '报名审核记录表，记录管理员每次审核操作';
COMMENT ON COLUMN signup_audit.biz_type      IS '业务类型：INDIVIDUAL个人赛/TEAM团队赛';
COMMENT ON COLUMN signup_audit.biz_id        IS 'INDIVIDUAL→individual_signup.id，TEAM→team_signup.id';
COMMENT ON COLUMN signup_audit.auditor_id    IS '审核人ID，关联sys_user.id';
COMMENT ON COLUMN signup_audit.result        IS '审核结果：APPROVED通过/REJECTED驳回';
COMMENT ON COLUMN signup_audit.reject_reason IS '驳回原因';

CREATE INDEX IF NOT EXISTS idx_signup_audit_biz
    ON signup_audit(biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_signup_audit_auditor
    ON signup_audit(auditor_id);

-- signup_audit 无需 updated_at，审核记录不可修改

-- =============================================
-- 11. 获奖记录表 award_record
-- =============================================
CREATE TABLE IF NOT EXISTS award_record (
    id               BIGSERIAL       PRIMARY KEY,
    competition_id   BIGINT          NOT NULL,
    submitter_id     BIGINT          NOT NULL,
    biz_type         VARCHAR(16)     NOT NULL,
    biz_id           BIGINT          NOT NULL,
    award_level      VARCHAR(32)     NOT NULL,
    award_name       VARCHAR(128)    NOT NULL,
    certificate_url  VARCHAR(512)    NOT NULL,
    award_date       DATE            NOT NULL,
    status           VARCHAR(16)     NOT NULL    DEFAULT 'PENDING',
    created_at       TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT chk_award_record_biz_type
        CHECK (biz_type IN ('INDIVIDUAL', 'TEAM')),
    CONSTRAINT chk_award_record_award_level
        CHECK (award_level IN ('NATIONAL_FIRST', 'NATIONAL_SECOND',
                               'NATIONAL_THIRD', 'PROVINCIAL_FIRST',
                               'PROVINCIAL_SECOND', 'PROVINCIAL_THIRD',
                               'OTHER')),
    CONSTRAINT chk_award_record_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

COMMENT ON TABLE  award_record                  IS '获奖记录表';
COMMENT ON COLUMN award_record.competition_id   IS '竞赛ID，关联competition.id';
COMMENT ON COLUMN award_record.submitter_id     IS '提交人ID，关联sys_user.id';
COMMENT ON COLUMN award_record.biz_type         IS '报名类型：INDIVIDUAL/TEAM';
COMMENT ON COLUMN award_record.biz_id           IS 'INDIVIDUAL→individual_signup.id，TEAM→team_signup.id';
COMMENT ON COLUMN award_record.award_level      IS '奖项等级枚举';
COMMENT ON COLUMN award_record.certificate_url  IS '证书图片相对路径，如/uploads/certificates/2024/01/uuid.jpg';
COMMENT ON COLUMN award_record.status           IS '状态：PENDING待审核/APPROVED已确认/REJECTED已驳回';

CREATE UNIQUE INDEX IF NOT EXISTS uk_award_record
    ON award_record(biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_award_record_competition
    ON award_record(competition_id);
CREATE INDEX IF NOT EXISTS idx_award_record_submitter
    ON award_record(submitter_id);
CREATE INDEX IF NOT EXISTS idx_award_record_status
    ON award_record(status);

CREATE TRIGGER trg_award_record_updated_at
    BEFORE UPDATE ON award_record
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 12. 获奖审核记录表 award_audit
-- =============================================
CREATE TABLE IF NOT EXISTS award_audit (
    id               BIGSERIAL       PRIMARY KEY,
    award_record_id  BIGINT          NOT NULL,
    auditor_id       BIGINT          NOT NULL,
    result           VARCHAR(16)     NOT NULL,
    reject_reason    TEXT            DEFAULT NULL,
    created_at       TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT chk_award_audit_result
        CHECK (result IN ('APPROVED', 'REJECTED'))
);

COMMENT ON TABLE  award_audit                   IS '获奖审核记录表';
COMMENT ON COLUMN award_audit.award_record_id   IS '关联获奖记录ID，关联award_record.id';
COMMENT ON COLUMN award_audit.auditor_id        IS '审核人ID，关联sys_user.id';
COMMENT ON COLUMN award_audit.result            IS '审核结果：APPROVED通过/REJECTED驳回';

CREATE INDEX IF NOT EXISTS idx_award_audit_record
    ON award_audit(award_record_id);

-- award_audit 无需 updated_at，审核记录不可修改

-- =============================================
-- 13. 消息通知表 sys_notification
-- =============================================
CREATE TABLE IF NOT EXISTS sys_notification (
    id          BIGSERIAL       PRIMARY KEY,
    receiver_id BIGINT          NOT NULL,
    type        VARCHAR(32)     NOT NULL,
    title       VARCHAR(128)    NOT NULL,
    content     TEXT            NOT NULL,
    related_id  BIGINT          DEFAULT NULL,
    is_read     BOOLEAN         NOT NULL    DEFAULT FALSE,
    created_at  TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT chk_sys_notification_type
        CHECK (type IN (
            'APPLY_RECEIVED',
            'APPLY_APPROVED',
            'APPLY_REJECTED',
            'TEAM_INVITE',
            'AUDIT_SUBMITTED',
            'AUDIT_APPROVED',
            'AUDIT_REJECTED',
            'RESUBMIT_REQUIRED',
            'AWARD_SUBMITTED',
            'AWARD_APPROVED',
            'AWARD_REJECTED'
        ))
);

COMMENT ON TABLE  sys_notification             IS '消息通知表，由RabbitMQ消费者写入';
COMMENT ON COLUMN sys_notification.receiver_id IS '接收人ID，关联sys_user.id';
COMMENT ON COLUMN sys_notification.type        IS '通知类型枚举';
COMMENT ON COLUMN sys_notification.related_id  IS '关联业务ID，前端可跳转到对应页面';
COMMENT ON COLUMN sys_notification.is_read     IS '是否已读，默认FALSE';

CREATE INDEX IF NOT EXISTS idx_notification_receiver_read
    ON sys_notification(receiver_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notification_created_at
    ON sys_notification(created_at DESC);

-- sys_notification 无需 updated_at，通知只有已读状态变更
-- 已读状态更新通过 UPDATE SET is_read=TRUE 直接操作，不需要触发器

-- =============================================
-- 14. AI知识库文档表 rag_document
-- =============================================
CREATE TABLE IF NOT EXISTS rag_document (
    id               BIGSERIAL       PRIMARY KEY,
    doc_name         VARCHAR(128)    NOT NULL,
    competition_name VARCHAR(128)    NOT NULL,
    chunk_index      INT             NOT NULL,
    content          TEXT            NOT NULL,
    embedding        vector(1024)    DEFAULT NULL,
    category         VARCHAR(32)     DEFAULT NULL,
    created_at       TIMESTAMPTZ     NOT NULL    DEFAULT NOW()
);

COMMENT ON TABLE  rag_document                  IS 'AI知识库文档表，存储竞赛介绍文档分块和向量';
COMMENT ON COLUMN rag_document.doc_name         IS '文档名称，如蓝桥杯介绍';
COMMENT ON COLUMN rag_document.competition_name IS '对应竞赛名称';
COMMENT ON COLUMN rag_document.chunk_index      IS '分块序号，同一文档从0开始递增';
COMMENT ON COLUMN rag_document.content          IS '分块后的原始文本内容';
COMMENT ON COLUMN rag_document.embedding        IS '向量表示，维度1024，对应BGE-M3模型';
COMMENT ON COLUMN rag_document.category         IS '竞赛类别标签，如算法/数学/创新';

CREATE INDEX IF NOT EXISTS idx_rag_document_name
    ON rag_document(doc_name);
CREATE INDEX IF NOT EXISTS idx_rag_document_category
    ON rag_document(category);

-- HNSW向量索引（在数据插入后创建效率更高）
-- 首次数据入库完成后执行：
-- CREATE INDEX idx_rag_embedding ON rag_document
--     USING hnsw (embedding vector_cosine_ops)
--     WITH (m = 16, ef_construction = 64);

-- =============================================
-- 初始化数据
-- =============================================

-- 插入管理员账号
-- 用户名：admin
-- 密码：admin123456（BCrypt加密）
INSERT INTO sys_user (
    username,
    password,
    real_name,
    role,
    department,
    status
) VALUES (
    'admin',
    '$2a$10$X.HCHxi1rvMuMWjMdpBnfuEeOVPvqCHiWERDKOBWVqP7lXAlQiAYi',
    '系统管理员',
    'ADMIN',
    '教务处',
    'ACTIVE'
) ON CONFLICT (username) DO NOTHING;

-- =============================================
-- 验证脚本（可选，执行后确认建表成功）
-- =============================================

-- 查看所有已创建的表
-- SELECT tablename FROM pg_tables
-- WHERE schemaname = 'public'
-- ORDER BY tablename;

-- 查看表数量（应为14）
-- SELECT COUNT(*) FROM pg_tables
-- WHERE schemaname = 'public';