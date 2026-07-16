IF COL_LENGTH('dbo.users', 'is_demo') IS NULL
BEGIN
    EXEC sys.sp_executesql N'
        ALTER TABLE dbo.users
            ADD is_demo BIT NOT NULL CONSTRAINT df_users_is_demo DEFAULT 0;
    ';
END;

EXEC sys.sp_executesql N'
    UPDATE dbo.users
    SET is_demo = 1
    WHERE email LIKE ''%@demo.pe'';
';

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_users_is_demo' AND object_id = OBJECT_ID('dbo.users'))
BEGIN
    EXEC sys.sp_executesql N'CREATE INDEX ix_users_is_demo ON dbo.users(is_demo);';
END;
