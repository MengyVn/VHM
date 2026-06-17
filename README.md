# VHM（村民快乐机）

VHM 是一个基于 NeoForge 与 Create 的 Minecraft 1.21.1 模组，核心内容是一台可被玩家或村民驱动的跑步机

## 功能
- 玩家上机后可按 `W` 发电，冲刺可提升输出
- 村民可自动上机运行，面包与僵尸都会影响输出倍率
- 支持工程师眼镜查看应力状态
- 支持 Create 的应力网络
- 自定义创造模式分类：`机械动力:你跑不过我你信吗`

### 局内效果

<img src="src/main/resources/portal/HappyVillage1.png" width="260" />
<img src="src/main/resources/portal/HappyVillage2.png" width="260" />
<img src="src/main/resources/portal/HappyVillage3.png" width="260" />

## 安装

1. 安装 Minecraft `1.21.1`
2. 安装 NeoForge `21.1.x`
3. 安装 Create `6.0.10`
4. 将本模组 jar 放入 `mods` 文件夹

## 运行项目

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
请确保目录包含libs依赖用于构建
```
—— libs
  |—— compile
       |—— flywheel-neoforge-1.21.1-1.0.6.jar
       |—— ponder-neoforge-1.0.82+mc1.21.1.jar
  |—— create-1.21.1-6.0.10.jar
  
```  
## 玩法

- 右键跑步机即可上机
- 按住 `W` 开始发电
- 冲刺可以提高输出
- `Shift` 可下机
- 给村民喂面包可以提高输出
- 村民周围有僵尸时也会提高输出

## 合成配方

跑步机采用竖向合成：
传动轴+安山机壳+传送带
- `create:shaft`
- `create:andesite_casing`
- `create:belt_connector`

## 挖掘与掉落

- 跑步机可被斧头和镐子快速挖掘
- 破坏后会掉落自身

## 创造模式分类

- 分类名：`机械动力:你跑不过我你信吗`
- 图标：跑步机 3D 物品图标
- 分类内仅包含跑步机一个物品

## 资源说明

- 跑步机支持工程师眼镜的应力提示
- 跑步机接入 Create 的机械动力网络
- 跑步机相关数值仍可能继续调优

## 开发信息

- 模组 ID：`vhm`
- 适用版本：Minecraft `1.21.1`
- Loader：NeoForge `21.1.x`
- Create：`6.0.10`

## 联系

- **作者**：Mengy
- **项目主页**：[GitHub Repository](https://github.com/MengyVn/VHM)
- **问题反馈**：[Issues](https://github.com/MengyVn/VHM/issues)