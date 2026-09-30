# Search Stronghold
[English](https://github.com/yinlin12121/search-stronghold/blob/main/READMEen.md) | [简体中文](https://github.com/yinlin12121/search-stronghold/edit/main/README.md)

模组使用DeepSeek制作 [查看提示词](https://github.com/yinlin12121/search-stronghold/blob/main/tishici.txt)

一个 **纯客户端** 的 Fabric 模组：扔出两颗末影之眼，自动算出末地要塞的坐标。

- Minecraft：**26.1.2/26.2/26.3**（Mojang 官方映射，无混淆）
- Fabric Loader：0.19.5+
- Java：25
- **After installing the mod, enter "@language English" in the chat bar to switch the mod to English.(Version v1.0 does not have this feature.)**
## 使用方法

1. 在聊天栏输入 `@start`（这条消息只在本地被拦截，**不会发送到服务器**）。
2. 聊天栏提示 `扔出第一颗末影之眼`，此时扔出一颗末影之眼。
3. 聊天栏提示 `更换位置，扔出第二个末影之眼`，走到至少 32 格以外，再扔一颗。
4. 计算完成后聊天栏输出：

```
已计算
末影之眼1   z=0.3x+125.4
末影之眼2   z=-0.9x+487.1
要塞的坐标 （1234，-567）
要塞在下界的对应坐标（154，-71）
```

计算结束后模组自动回到休眠状态并清空所有数据；计算过程中随时可以输入 `@stop` 强行结束。

## 语言切换

模组默认使用中文。在聊天栏输入下面的指令即可切换提示语言（这条消息同样只在本地被拦截，**不会发送到服务器**）：

| 指令 | 切换后的提示 |
| --- | --- |
| `@language Chinese` | 语言已切换为中文 |
| `@language English` | Language has been switched to English. |

语言名不区分大小写，`@language english`、`@language CHINESE` 都可以。切换语言只影响之后新产生的提示，已经输出的提示不会被改写；计算状态、已经记录的坐标都不受影响。

只要消息看起来是 `@language` 指令，就会被本地拦截，**不会发送到服务器**：语言名写错（例如 `@language French`）或者没写语言名（`@language`）时，消息会被静默丢弃，语言保持不变，也没有任何提示。而 `@languageEnglish` 这样没有空格分隔的文本不是指令，会作为普通聊天原样发送。

切换为英语后，模组的全部提示如下：

| 情况 | 提示 |
| --- | --- |
| 输入 `@start` | Throw the first eye of ender |
| 第一颗记录完成 | Change position and throw the second eye of ender |
| 半径 16 格内有其他正在运动的末影之眼 | Failed to get coordinates; there are other eyes of ender nearby |
| 末影之眼向下飞 | The stronghold is already nearby |
| 两次扔出位置不足 32 格 | Need to go farther away (beyond 32 blocks) |
| 两条直线平行，或交点距离超过 20000 格 | Calculation failed; the stronghold is too far away |
| 其他无法获取坐标/无法计算的情况 | Calculation failed |
| 输入 `@stop` | End calculation |

英语模式下的计算结果输出为：

```
Calculated
Eye of Ender 1   z=0.3x+125.4
Eye of Ender 2   z=-0.9x+487.1
Stronghold coordinates (1234, -567)
Stronghold coordinates in the nether (154, -71)

## 原理

末影之眼被扔出后会朝着最近的末地要塞水平漂移，其平面（xz）轨迹是一条线段。
记录同一颗末影之眼在两个不同时刻的 `(x, z)` 坐标，即可得到这颗末影之眼所在直线
的一次函数表达式 `z = kx + b`。玩家换一个位置再扔一颗，两条直线的交点就是要塞坐标。

## 状态与提示

模组只有两种状态：**休眠**（不采集任何坐标、不参与任何游戏行为）和**计算状态**
（输入 `@start` 之后）。下列情况会在聊天栏给出提示：

| 情况 | 提示 | 是否退出计算状态 |
| --- | --- | --- |
| 输入 `@start` | 扔出第一颗末影之眼 | 否 |
| 第一颗记录完成 | 更换位置，扔出第二个末影之眼 | 否 |
| 半径 16 格内有其他正在运动的末影之眼 | 获取坐标失败，周围有其他末影之眼 | 否（等干扰消失后会重新提示） |
| 末影之眼向下飞（y 减小，说明已在要塞脚下） | 要塞已在附近 | 否 |
| 两次扔出位置不足 32 格 | 需前往更远的距离（32格之外） | 否（走远后自动继续） |
| 两条直线平行，或交点距离超过 20000 格 | 计算失败，要塞距离过远 | 是 |
| 其他无法获取坐标/无法计算的情况 | 计算失败 | 是 |
| 输入 `@stop` | 结束计算 | 是 |
| 退出世界/退出游戏 | （无提示） | 是，且不保存任何数据 |

计算状态中再次输入 `@start`、休眠状态中输入 `@stop` 都会被忽略，且没有任何提示。

## 精度

- 末影之眼坐标以 `double` 全精度记录，内部计算不做任何取整。
- 输出时两条直线保留 1 位小数，交点坐标取整。
- 会校验采样点是否落在同一条直线上，避免混入其他末影之眼的数据。

## 下界坐标

末地要塞只生成在主世界，而下界与主世界的水平坐标是 8:1 的关系。
因此计算得到的要塞坐标 `(x, z)` 除以 8 就是玩家在下界需要前往的对应坐标，
模组会在输出要塞坐标的下一行直接给出：

```
要塞的坐标 （1234，-567）
要塞在下界的对应坐标（154，-71）
```

换算与游戏中的传送门一致：结果向下取整，因此负坐标也是向下取整
（例如 `-567 ÷ 8 = -70.875`，取 `-71`）。
该换算只对水平方向（x、z）有意义，下界的 y 坐标与要塞无关，因此不做换算。

## 多人模式

聊天输入在 `ChatScreen.handleChatInput` 的 `HEAD` 就被取消，不会生成任何聊天数据包；
所有提示都通过 `ChatHud.addClientSystemMessage` 在本地显示。
模组不注册任何网络通道、物品或方块，因此可以在多人服务器中直接使用。

## 主要文件

```
src/client/java/com/example/client/
├── SearchStrongholdClient.java        客户端入口
├── StrongholdSearcher.java            状态机、交点计算、聊天栏提示
├── EyeTracker.java                    单颗末影之眼的坐标采样与直线拟合
├── Trajectory.java                    直线 z = kx + b 的数据与格式化
└── mixin/
    ├── ChatScreenMixin.java           拦截 @start / @stop（不发送到服务器）
    └── ClientLevelMixin.java          每客户端刻的检测 + 退出世界时清空数据
```
