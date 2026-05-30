# PolisCraft 实施路线图

基于对 MineColonies + Structurize 源码的深度研究，此文档记录了完整架构和实施计划。

---

## 一、当前已完成的工作

### ✅ 全息蓝图渲染系统
- `Blueprint.java` — 蓝图数据结构 (palette + 3D short array)
- `BlueprintRenderer.java` — VertexBuffer + ChunkBufferBuilderPack 真实块渲染
- `HologramRenderEvents.java` — 客户端直接扫描 BE 渲染
- `HologramRenderer.java` — 蓝图生成器

### ✅ 逐块建造系统
- `BuilderWorkGoal.java` — Builder AI 从 BlueprintIterator 取方块 setBlock
- `BlueprintIterator.java` — X→Z→Y 蛇形扫描，支持 NBT 进度

### ✅ 建筑工具交互
- `BuildingToolItem.java` — 客户端打开 BuildToolScreen
- `BuildToolScreen.java` — 建筑选择界面（Vanilla Screen）
- `BuildToolPlacementMessage.java` — 放置请求网络包
- `NetworkHandler.java` — 通道注册

### ✅ 经济系统
- `ColonyState.java` — 国库管理
- `DailyScheduler.java` — 工资/食物/税收
- `WorkGoal` 系列 — 报酬进国库

### ✅ 现有文件
48 个 Java 文件，0 第三方依赖。
项目位置: `forge-1.20.1-47.4.10-mdk/`

---

## 二、下一步：市民命名系统

### 2.1 JSON 数据文件

**位置**: `src/main/resources/data/poliscraft/citizennames/`

**default.json** (西方风格，3 parts, WESTERN 顺序):
```json
{
  "parts": 3,
  "order": "WESTERN",
  "male_firstname": ["Aaron", "Adam", "Adrian", "Alan", "Albert", "Alexander", "Andrew", "Anthony", "Arthur", "Benjamin", "Blake", "Brandon", "Brian", "Caleb", "Cameron", "Carl", "Charles", "Christopher", "Clark", "Connor", "Daniel", "David", "Dennis", "Derek", "Dominic", "Dylan", "Edward", "Elliot", "Eric", "Ethan", "Evan", "Felix", "Francis", "Frank", "Gabriel", "George", "Gerald", "Gordon", "Grant", "Gregory", "Harold", "Harvey", "Henry", "Howard", "Hugo", "Ian", "Isaac", "Jack", "Jacob", "James", "Jason", "Jasper", "Jeremy", "Jesse", "Joel", "John", "Jonah", "Jonathan", "Joseph", "Joshua", "Julian", "Justin", "Keith", "Kevin", "Kyle", "Lance", "Lawrence", "Leo", "Leonard", "Liam", "Logan", "Louis", "Lucas", "Luke", "Malcolm", "Marcus", "Mark", "Martin", "Matthew", "Michael", "Morgan", "Nathan", "Neil", "Nicholas", "Noah", "Oliver", "Oscar", "Owen", "Patrick", "Paul", "Peter", "Philip", "Ralph", "Raymond", "Richard", "Robert", "Roger", "Ronald", "Russell", "Ryan", "Samuel", "Scott", "Sean", "Sebastian", "Simon", "Stephen", "Steven", "Stuart", "Theodore", "Thomas", "Timothy", "Toby", "Tyler", "Victor", "Vincent", "Walter", "Warren", "Wayne", "William", "Zachary"],
  "female_firstname": ["Abigail", "Ada", "Alexandra", "Alice", "Amanda", "Amber", "Amy", "Andrea", "Angela", "Ann", "Anna", "Audrey", "Barbara", "Beatrice", "Bridget", "Brooke", "Catherine", "Charlotte", "Chloe", "Christina", "Claire", "Clara", "Danielle", "Dawn", "Deborah", "Diana", "Donna", "Dorothy", "Edith", "Eleanor", "Elizabeth", "Ella", "Emily", "Emma", "Erica", "Evelyn", "Faith", "Fiona", "Frances", "Georgia", "Gloria", "Grace", "Hannah", "Harriet", "Hazel", "Heather", "Helen", "Holly", "Hope", "Irene", "Isabel", "Jacqueline", "Jane", "Janet", "Joanna", "Jocelyn", "Josephine", "Joyce", "Judith", "Julia", "Karen", "Katherine", "Kelly", "Kimberly", "Laura", "Lauren", "Leah", "Linda", "Lisa", "Lois", "Louise", "Lucy", "Lydia", "Lynn", "Madeline", "Margaret", "Maria", "Martha", "Mary", "Megan", "Melissa", "Michelle", "Molly", "Nancy", "Natalie", "Nicole", "Olivia", "Pamela", "Patricia", "Paula", "Priscilla", "Rebecca", "Rita", "Roberta", "Rose", "Rosemary", "Ruth", "Samantha", "Sandra", "Sarah", "Sharon", "Sheila", "Shirley", "Sophia", "Stephanie", "Susan", "Tamara", "Teresa", "Tracy", "Valerie", "Veronica", "Victoria", "Virginia", "Wendy", "Yvonne", "Zoe"],
  "surnames": ["Abell", "Adams", "Allen", "Anderson", "Baker", "Baldwin", "Banks", "Barnes", "Bell", "Bennett", "Black", "Brooks", "Brown", "Bryant", "Burns", "Butler", "Campbell", "Carter", "Clark", "Coleman", "Collins", "Cook", "Cooper", "Cox", "Crawford", "Cruz", "Cunningham", "Davis", "Diaz", "Dixon", "Edwards", "Ellis", "Evans", "Fisher", "Foster", "Fox", "Freeman", "Garcia", "Gardner", "Gibson", "Gomez", "Gonzalez", "Gordon", "Graham", "Grant", "Gray", "Green", "Griffin", "Hall", "Hamilton", "Hansen", "Harris", "Harrison", "Hart", "Harvey", "Hawkins", "Hayes", "Henderson", "Henry", "Hernandez", "Hill", "Holland", "Holmes", "Howard", "Hughes", "Hunt", "Hunter", "Jackson", "James", "Jenkins", "Johnson", "Johnston", "Jones", "Jordan", "Kelley", "Kelly", "Kennedy", "Kim", "King", "Knight", "Lane", "Larson", "Lawrence", "Lee", "Lewis", "Long", "Lopez", "Lynch", "MacDonald", "Marshall", "Martin", "Martinez", "Mason", "Matthews", "McCarthy", "McDonald", "Miller", "Mills", "Mitchell", "Moore", "Morgan", "Morris", "Morrison", "Murphy", "Murray", "Myers", "Nelson", "Newman", "Nguyen", "Nichols", "O'Brien", "O'Neal", "Oliver", "Olson", "Owens", "Palmer", "Parker", "Patterson", "Payne", "Perez", "Perkins", "Perry", "Peterson", "Phillips", "Pierce", "Porter", "Powell", "Price", "Ramirez", "Ray", "Reed", "Reyes", "Reynolds", "Rice", "Richards", "Richardson", "Riley", "Rivera", "Roberts", "Robertson", "Robinson", "Rodriguez", "Rogers", "Rose", "Ross", "Russell", "Ryan", "Sanchez", "Sanders", "Scott", "Shaw", "Simmons", "Simpson", "Smith", "Snyder", "Spencer", "Stanley", "Stevens", "Stewart", "Stone", "Sullivan", "Taylor", "Thomas", "Thompson", "Torres", "Tucker", "Turner", "Walker", "Wallace", "Walsh", "Ward", "Warren", "Washington", "Watson", "Watts", "Weaver", "Webb", "Welch", "Wells", "West", "Wheeler", "White", "Williams", "Williamson", "Willis", "Wilson", "Wood", "Wright", "Young"]
}
```

**chinese.json** (中文风格，2 parts, EASTERN 顺序):
```json
{
  "parts": 2,
  "order": "EASTERN",
  "male_firstname": ["Tienchen", "Yuxuan", "Haoran", "Weiming", "Xiaolong", "Junwei", "Zhiyuan", "Kaiwen", "Mingyang", "Jianhua", "Lei", "Wei", "Qiang", "Yong", "Peng", "Jun", "Bin", "Hui", "Yang", "Feng", "Chao", "Dong", "Gang", "Hao", "Long", "Ming", "Ning", "Ping", "Tao", "Xin"],
  "female_firstname": ["Yinuo", "Xinyi", "Yuhan", "Zixuan", "Mengyao", "Xue", "Ting", "Lina", "Yan", "Fang", "Hong", "Jing", "Lan", "Mei", "Qing", "Rong", "Shuang", "Wan", "Xia", "Yue", "Zhen", "Ai", "Bao", "Chun", "Dan", "Fen", "Hua", "Jia", "Li", "Min", "Na", "Ping", "Qian", "Rui", "Shu", "Tian", "Wen", "Xian", "Ying", "Yun", "Yuan"],
  "surnames": ["Li", "Wang", "Zhang", "Liu", "Chen", "Yang", "Zhao", "Huang", "Zhou", "Wu", "Xu", "Sun", "Hu", "Zhu", "Gao", "Lin", "He", "Guo", "Ma", "Luo", "Liang", "Song", "Zheng", "Xie", "Han", "Tang", "Feng", "Dong", "Cheng", "Cai", "Peng", "Pan", "Yuan", "Yu", "Deng", "Xu", "Fu", "Shen", "Zeng", "Peng", "Lu", "Su", "Lu", "Jia", "Ding", "Xue", "Ye", "Yan", "Yu", "Pan", "Du", "Dai", "Xia", "Tian", "Fan", "Fang"]
}
```

### 2.2 Java 代码

#### CitizenNameFile.java
**位置**: `com.poliscraft.colony.citizen.CitizenNameFile`

```java
package com.poliscraft.colony.citizen;

import java.util.List;

/**
 * 市民命名文件 — 从 JSON 加载
 * 参考 MineColonies CitizenNameFile
 */
public class CitizenNameFile {
    public enum NameOrder { EASTERN, WESTERN }
    
    private int parts;              // 姓名部分数 (2 或 3)
    private NameOrder order;        // 命名顺序
    private List<String> maleFirstNames;
    private List<String> femaleFirstNames;
    private List<String> surnames;
    
    // 构造 + getter
}
```

#### CitizenNameListener.java
**位置**: `com.poliscraft.colony.citizen.CitizenNameListener`

参考 MineColonies 的 `SimpleJsonResourceReloadListener`，监听 `citizennames` 路径：
```java
package com.poliscraft.colony.citizen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
// 使用 SimpleJsonResourceReloadListener 监听 "citizennames" 路径
// 解析 JSON 为 Map<String, CitizenNameFile>
// 在 Mod 构造时注册
```

#### CitizenData.java
**位置**: `com.poliscraft.colony.citizen.CitizenData`

核心数据类，含名字生成逻辑：
```java
public class CitizenData {
    private int id;
    private String name;
    private boolean female;
    private int textureId;      // 0-255
    private String textureSuffix; // "_a", "_b", "_d", "_w"
    
    // 名字生成 (参考 MineColonies CitizenData.generateName)
    public static String generateName(Random rand, boolean female, CitizenNameFile nameFile) {
        // 1. 根据性别取 firstName
        // 2. 随机取姓氏
        // 3. 如果 parts==3 加中间名首字母
        // 4. 按 EASTERN/WESTERN 顺序组装
        // 5. 检查重名
    }
    
    public void initForNewCitizen(Random rand, CitizenNameFile nameFile) {
        // 随机性别
        // 随机 textureSuffix
        // 调用 generateName
    }
}
```

#### CitizenManager.java
**位置**: `com.poliscraft.colony.managers.CitizenManager`

市民生成调度（在 Colony tick 中调用）：
```java
public class CitizenManager {
    private ColonyState colony;
    private Map<Integer, CitizenData> citizens;
    private int respawnInterval;
    
    public void onColonyTick() {
        // 检查是否有市政厅
        // 检查当前市民数 < 初始数量
        // 检查 respawnInterval
        // createAndRegisterCivilianData()
        // 性别平衡
    }
    
    public CitizenData createAndRegisterCivilianData() {
        // 复用空缺ID
        // new CitizenData(id)
        // initForNewCitizen()
        // 存入citizens map
    }
}
```

---

## 三、下一步：施工围栏

参考 MineColonies `BlockConstructionTape`:
- 一个纯装饰方块，无 BE，无碰撞箱
- `replaceable()` 可被其他方块替换
- `noCollission()` 无碰撞
- `noLootTable()` 无掉落
- 在 Zone 的 perimeter 上放置

**实现位置**:
- `core/blocks/BlockConstructionTape.java` — 围栏方块
- `core/ConstructionTapeHelper.java` — 放置/移除工具方法
- 在 `BuildToolPlacementMessage.handle()` 和 `startConstruction()` 中调用

---

## 四、远期计划

### Phase A: 建筑方块体系
- 每个建筑类型有对应的实际方块（BlockTownHall, BlockFarm 等）
- 类似 MineColonies `AbstractBlockHut` 的继承体系
- 放置时放实际方块，而非不可见标记

### Phase B: Job/Module 系统
- `JobEntry` 注册表 (Forge DeferredRegister)
- `AbstractJob` 基类，`JobBuilder`, `JobFarmer` 等
- `BuildingModule` 体系（WorkerModule, LivingModule）
- 自动/手动分配市民

### Phase C: 市民 GUI
- 雇佣窗口 WindowHireCitizen
- 市民信息窗口
- 建筑管理窗口（进度/材料/工人）

---

## 五、关键参考源码位置

MineColonies 源码: `D:/Users/qq274/Desktop/开发文件/模组开发/都市起源/参考/minecolonies-version-main/`
Structurize 源码: `D:/Users/qq274/Desktop/开发文件/模组开发/都市起源/参考/Structurize-source/`
BlockUI 源码: `D:/Users/qq274/Desktop/开发文件/模组开发/都市起源/参考/BlockUI-source/`

关键参考文件:
- `CitizenData.java` → `core/colony/CitizenData.java`
- `CitizenNameListener.java` → `core/datalistener/CitizenNameListener.java`
- `CitizenNameFile.java` → `api/colony/CitizenNameFile.java`
- `CitizenManager.java` → `core/colony/managers/CitizenManager.java`
- `BlockConstructionTape.java` → `core/blocks/decorative/BlockConstructionTape.java`
- `AbstractBlockHut.java` → `api/blocks/AbstractBlockHut.java`
- `SurvivalHandler.java` → `core/placementhandlers/main/SurvivalHandler.java`
- `WindowHireWorker.java` → `core/client/gui/WindowHireWorker.java`
