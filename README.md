# VHM (Villager Harness Mod) - 村民跑步机模组

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.233-blue.svg)](https://neoforged.net/)
[![Create Dependency](https://img.shields.io/badge/Create-6.0.10-orange.svg)](https://modrinth.com/mod/create)

**VHM** 是一个基于 **Minecraft NeoForge 1.21.1** 的 Create（机械动力）附属模组，将村民转化为工业动力源。通过将村民"困"在特制的跑步机上，利用其奔跑动作产生旋转动能（应力/SU），为整个工厂提供源源不断的动力。

---

##  核心特性

### 🏃 三路分离渲染架构
- **静止层**：外壳、挡板、控制台采用 Create 原版安山合金材质，由 Minecraft 原生 BlockState 渲染
- **转动层**：传动轴托管给 Create 引擎，根据实时 RPM 物理旋转
- **滚动层**：履带表面通过 UV 坐标偏移实现无级变速滚动动画

### 🔗 级联网络系统
- **自动串联**：左右两侧传动轴横向并排时自动融为一体，无需额外连接线
- **最大支持**：单个级联电网最多支持 **32 台** 跑步机并联
- **转速合并**：整条网络的最终 RPM 采取 **取最大值 (Max)** 策略
- **应力叠加**：产生的总应力容量采取 **代数求和 (Σ SU)** 策略
- **手动限流**：可设置总输出上限 `SU_max`，作为工业网络的"安全阀"

### 👥 多实体支持
#### 村民 (Villager)
踏入边界后，服务器端强制拦截其移动，将其坐标锁定至方块中心，并触发超频摆腿动画：

| 状态 | 转速倍率 | 应力倍率 | 说明 |
|------|---------|---------|------|
| 常规 | 1x (32 RPM) | 1x (128 SU) | 默认输出 |
| 面包增益 | 2x (64 RPM) | 2x (256 SU) | 喂食面包后持续 600 ticks |
| 害怕状态 | 2x (64 RPM) | 2x (256 SU) | 附近有僵尸等敌对生物 |
| 兴奋状态 | 4x (128 RPM) | 4x (512 SU) | 面包 + 害怕叠加 |

#### 玩家 (Player)
允许站在跑步机上。通过读取玩家的移动键盘输入向量（`player.zza`）：
- **按住 W 键**：触发应力输出与动画
- **松开按键**：整台机器随之静止

### ⚙️ 数值参数
- **基础转速**：32 RPM
- **最大应力输出**：4096 SU
- **基础应力容量**：128 SU/RPM (4096 ÷ 32)
- **零物理纯计算**：彻底杜绝服务器端的物理位移与碰撞箱拉扯，用纯数学公式计算应力网络产出

---

## 📦 依赖要求

| 依赖项 | 版本 | 必需性 |
|--------|------|--------|
| Minecraft | 1.21.1 | ✅ 必需 |
| NeoForge | 21.1.233+ | ✅ 必需 |
| Create (机械动力) | 6.0.10 | ✅ 必需 |
| Flywheel | 1.0.6 | ✅ 必需 (Create 附带) |

⚠️ **重要提示**：本模组属于 **Create**模组的扩展，**必须同时安装 Create 模组才能正常工作**。

---

##  使用方法

### 1. 放置跑步机
从创造模式物品栏或合成表中获取 **Treadmill（跑步机）**，放置在地面上。

### 2. 吸引村民
将村民引导至跑步机附近（半径约 1 格），村民会自动踏上跑步机并被锁定坐标。

### 3. 连接动力系统
- 跑步机两侧的传动轴会自动与相邻跑步机的传动轴融合
- 使用 Create 的 **Shaft（传动轴）**、**Cogwheel（齿轮）**、**Belt（传送带）** 等组件连接至你的工厂
- 使用 **Stressometer（应力表）** 查看当前输出的应力值

### 4. 提升效率
- **喂食面包**：右键点击村民给予面包，使其进入"面包增益"状态（持续 30 秒）
- **制造威胁**：在跑步机周围放置僵尸、骷髅等敌对生物，使村民进入"害怕状态"
- **双重增益**：同时满足以上两个条件可获得 **4 倍** 输出

### 5. 级联优化
将多台跑步机横向并排摆放，形成级联网络：
- 所有跑步机的传动轴会自动同步为 **最高 RPM**
- 总应力 = 各台跑步机应力之和
- 适合大规模工业化部署

---

## ️ 开发指南

### 环境配置
```bash
# 克隆仓库
git clone https://github.com/yourusername/VHM.git
cd VHM

# 构建项目
./gradlew build

# 运行客户端测试
./gradlew runClient

# 运行服务端测试
./gradlew runServer
```

### 项目结构
```
src/main/java/MengySmod/vhm/
── Vhm.java                    # 主模组类，注册方块/物品/事件
├── VhmBlocks.java              # 方块注册
├── VhmItems.java               # 物品注册
├── VhmBlockEntities.java       # 方块实体注册
├── treadmill/
│   ├── TreadmillBlock.java     # 跑步机方块逻辑（碰撞箱、方向）
│   ├── TreadmillBlockEntity.java # 核心逻辑：应力计算、实体处理、级联网络
│   └── TreadmillMount.java     # 玩家挂载管理
├── event/
│   └── TreadmillEvents.java    # 事件监听（实体 Tick、交互）
└── client/
    ├── VhmClient.java          # 客户端初始化
    ├── TreadmillRenderer.java  # 渲染器（传动轴旋转、传送带 UV 滚动）
    ├── TreadmillVisual.java    # Flywheel 可视化支持
    ├── ClientAnimHandler.java  # 村民动画处理（原地超频摆腿）
    ├── VhmPartialModels.java   # 局部模型注册
    └── VhmSpriteShifts.java    # 纹理偏移配置
```

### 关键 API

#### TreadmillBlockEntity
```java
// 获取本地生成的转速（固定 32 RPM）
public float getLocalGeneratedSpeed()

// 获取传送带速度倍率（1x/2x/4x）
public float getBeltSpeedMultiplier()

// 获取本地应力容量
public float getLocalStressCapacity()

// 判断实体是否在传送带上
public boolean isEntityOnBelt(LivingEntity entity)
```

#### TreadmillNetworkHelper
```java
// 收集级联网络中的所有跑步机
public static List<TreadmillBlockEntity> collectCascade(Level level, BlockPos pos, Axis axis)

// 计算网络最大 RPM（取最大值）
public static float networkMaxRpm(List<TreadmillBlockEntity> cascade)

// 计算网络总应力容量（代数求和）
public static float networkTotalCapacity(List<TreadmillBlockEntity> cascade, float networkRpm)

// 应用手动限流
public static float applyManualCap(List<TreadmillBlockEntity> cascade, float rawTotal)
```

### 日志调试
所有关键方法都添加了日志输出，方便调试：
```bash
# 查看 VHM 相关日志
grep -i "treadmill" .minecraft/logs/latest.log

# 搜索错误信息
grep -i "\[Treadmill.*Error\]" .minecraft/logs/latest.log
```

---

## 🎨 资源文件结构
```
src/main/resources/assets/
├── vhm/
│   ├── blockstates/
│   │   └── treadmill.json        # 四方向 Facing 配置
│   ├── models/block/
│   │   ├── treadmill_block.json  # 静态外壳（底座、侧板、控制台）
│   │   └── treadmill_belt.json   # 传送带表面（UV 动画）
│   ├── models/item/
│   │   ── treadmill.json        # 物品模型
│   ├── textures/block/           # （已移除，改用 Create 原版材质）
│   └── lang/
│       ├── zh_cn.json            # 中文翻译
│       ── en_us.json            # 英文翻译
── create/textures/block/        # Create 原版材质引用
    ├── andesite_casing.png       # 安山合金外壳
    ├── andesite_encased_cogwheel_side.png  # 侧板纹理
    ├── belt.png                  # 传送带橡胶纹理
    ├── controls.png              # 控制面板纹理
    └── factory_panel.png         # 工厂面板纹理
```

---

## 📝 更新日志

### v1.0-SNAPSHOT (当前版本)
- ✅ 实现三路分离渲染架构（静止层、转动层、滚动层）
- ✅ 实现级联网络系统（最大 32 台并联）
- ✅ 实现村民坐标锁定与超频摆腿动画
- ✅ 实现玩家站立检测与键盘输入响应
- ✅ 实现四种村民状态（常规、面包、害怕、兴奋）
- ✅ 重构模型，使用 Create 原版材质风格
- ✅ 修复传动轴残影问题（使用正确的 shaft BlockState）
- ✅ 添加完整日志系统用于调试
- ✅ 所有关键方法添加异常捕获防止游戏崩溃

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 提交 Issue
- 描述清楚复现步骤
- 提供游戏日志（`.minecraft/logs/latest.log`）
- 附上截图或视频（如有）

### 提交 PR
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 许可证

本项目采用 **All Rights Reserved** 许可证。未经作者许可，禁止商业用途。

---

## 🙏 致谢

- [Create Team](https://modrinth.com/mod/create) - 提供了强大的动力学引擎和精美的材质
- [NeoForge](https://neoforged.net/) - 现代化的模组加载器
- [Blockbench](https://blockbench.net/) - 优秀的 3D 建模工具

---

## 📧 联系方式

- **作者**：Mengy
- **项目主页**：[GitHub Repository](https://github.com/MengyVn/VHM)
- **问题反馈**：[Issues](https://github.com/MengyVn/VHM/issues)

---


