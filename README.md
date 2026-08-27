# Clue Frame Claim

**Clue Frame Claim** 是一个面向 **Minecraft Java 1.21.10 + Fabric** 的服务端权威模组。带有实体标签 `clue_frame` 的物品展示框（包括发光物品展示框）不能通过普通左键或右键取走物品；玩家必须持续按住右键 **3 秒**，才能领取其中的展示物品。

## 功能说明

| 行为 | 结果 |
| --- | --- |
| 玩家对带 `clue_frame` 标签的展示框左键 | 操作被拦截，展示框不会被破坏，也不会掉落物品。 |
| 玩家短按右键或中途松开右键 | 领取取消，进度清零。 |
| 玩家持续对准并按住右键 3 秒 | 准星右侧进度条填满；展示物品转移至玩家背包，展示框随即清空。 |
| 玩家距离超过约 5 格、移动准星或目标被清空 | 服务端取消领取并清除进度条。 |
| 背包无空间 | 剩余物品在玩家脚下掉落，避免物品丢失。 |

> 进度由服务端逐 tick 验证，客户端只负责上报“正在按住右键的目标”。因此，修改客户端进度条或伪造单次请求都无法直接领取物品。

## 安装

将下列两个 JAR 文件放入客户端和服务端同一个 Minecraft 实例的 `mods` 目录，然后启动 **Minecraft Java 1.21.10**：

| 组件 | 要求 |
| --- | --- |
| Minecraft | `1.21.10` |
| Java | `21` 或更高 |
| Fabric Loader | `0.19.3` 或更高 |
| Fabric API | `0.138.4+1.21.10` |
| 本模组 | `clue-frame-claim-1.0.0.jar` |

Fabric 官方模板的 `1.21.10` 分支提供了本项目所采用的 Loader、Fabric API、Loom 与 Java 版本基准。[1]

## 给展示框添加标签

先放置物品展示框并放入要作为线索奖励的物品。面向目标展示框后，执行下面的原版命令即可：

```mcfunction
/tag @e[type=minecraft:item_frame,sort=nearest,limit=1] add clue_frame
```

对于发光物品展示框，请使用：

```mcfunction
/tag @e[type=minecraft:glow_item_frame,sort=nearest,limit=1] add clue_frame
```

可使用以下命令检查标签是否成功：

```mcfunction
/tag @e[type=minecraft:item_frame,sort=nearest,limit=1] list
```

如需恢复原版交互，只需移除该标签：

```mcfunction
/tag @e[type=minecraft:item_frame,sort=nearest,limit=1] remove clue_frame
```

## 构建源码

项目使用官方 Fabric Gradle 模板结构。安装 Java 21 后，在项目根目录执行：

```bash
./gradlew build
```

可安装 JAR 输出至：

```text
build/libs/clue-frame-claim-1.0.0.jar
```

Fabric 的网络通信指南要求服务端验证实体标识、类型与玩家距离；本项目的领取逻辑遵循这一模式。[2]

## 许可证

本项目以 **CC0-1.0** 许可证发布。

## 参考资料

[1] [FabricMC/fabric-example-mod：1.21.10 分支](https://github.com/FabricMC/fabric-example-mod/tree/1.21.10)

[2] [Fabric Documentation：Networking](https://docs.fabricmc.net/develop/networking)
