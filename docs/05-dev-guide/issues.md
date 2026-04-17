# 开发问题记录文档

## 1. PostgreSQL 数据库连接认证失败

**问题描述：**
后端启动时抛出异常：`org.postgresql.util.PSQLException: FATAL: password authentication failed for user "competition"`。即便 `application-dev.yml` 与 `docker-compose.yml` 中的密码配置完全一致，依然无法连接。

**原因分析：**
1. **本地服务冲突**：开发机本地安装并运行了 PostgreSQL 服务（Windows 服务），且同样占用了 `5432` 端口。后端程序优先连接到了本地服务而非 Docker 容器中的数据库。

**解决方案：**
1. **停止本地服务**：进入 Windows 服务管理器，停止并禁用本地的 PostgreSQL 服务，确保 `5432` 端口由 Docker 独占。

---

## 2. Spring Boot 3.5.13 与 Knife4j 版本不兼容

**问题描述：**
启动后端后访问 [Knife4j](http://localhost:8080/doc.html) 报错，控制台抛出：`java.lang.NoSuchMethodError: 'void org.springframework.web.method.ControllerAdviceBean.<init>(java.lang.Object)'`。

**原因分析：**
1. **底层依赖冲突**：Spring Boot 3.4.x 和 3.5.x 升级到了 **Spring Framework 6.2**。
2. **破坏性变更**：Spring 6.2 对内部类 `ControllerAdviceBean` 的构造函数进行了修改。
3. **生态适配延迟**：当前的 `Knife4j 4.4.0` 所依赖的 `springdoc-openapi` 尚未适配 Spring 6.2 的这一变更，导致在反射调用时找不到对应的构造方法。

**解决方案：**
1. **版本降级**：将 Spring Boot 版本从 `3.5.13/3.4.2` 降级到 **`3.3.8`**。
2. **环境清理**：执行 `mvn clean` 清理旧版本编译生成的 `target` 目录，并在 IDE 中刷新 Maven 依赖。
3. **原理确认**：Spring Boot 3.3.x 对应的 Spring Framework 版本为 6.1.x，该版本中 `ControllerAdviceBean` 的 API 与当前 Knife4j 兼容。

---

## 3. Windows 端口显示占用但无进程（Hyper-V/WSL2 端口排除）

**问题描述：**
在启动 Docker 或后端服务时，提示端口（如 5432, 6379, 8080）被占用。但在使用 `netstat -ano` 查询时，找不到任何进程在利用该端口。

**原因分析：**
1. **系统保留范围**：Windows 的 Hyper-V 或 WSL2 机制会自动保留一段动态端口范围（Excluded Port Range）。如果需要的端口恰好落在这些“排除范围”内，系统将禁止任何普通程序（包括 Docker 和 Java）绑定该端口。
2. **WinNAT 服务异常**：Windows 内部的地址转换服务（WinNAT）在系统长时间不重启或网络环境切换后，可能产生错误的端口锁定。

**解决方案：**

1. **检查端口是否被系统保留**：
   以管理员身份打开 **PowerShell**，执行以下命令查看当前被排除的端口范围：
   ```powershell
   netsh interface ipv4 show excludedportrange protocol=tcp
   ```
   *如果在输出列表中看到你的端口（如 5432）包含在起始/结束偏移量之间，说明端口被系统占用了。*

2. **重启 WinNAT 服务释放端口**：
   无需重启电脑，通过重启 WinNAT 服务即可重新初始化端口分配：
   ```powershell
   # 停止网络地址转换服务
   net stop winnat
   
   # 启动网络地址转换服务
   net start winnat
   ```

3. **预防建议**：
   如果某些端口经常被抢占，可以尝试在管理员权限下将特定端口从动态保留范围中彻底剔除（需结合具体系统配置）。但在开发阶段，**重启 winnat 服务**是最快捷的解决方法。

---

**文档说明：**
本日志用于记录项目开发过程中遇到的环境配置与框架兼容性坑点，方便团队成员后续快速定位类似问题。