高仿抖音“投稿”页面 (Android Demo)

这是一个基于 Android 原生开发 (Java) 的高仿抖音投稿界面 Demo。实现了图片九宫格拖拽排序、富文本编辑（#话题/@用户）、模拟 AI 文案生成以及地理位置获取等核心功能。 


📱 功能演示
演示视频：
https://bruh-clips.com/clips/bd7de8b7

1. 图片管理

九宫格展示：支持最多选择 9 张图片。

拖拽排序：长按图片可自由拖拽调整顺序 (ItemTouchHelper)。

添加与删除：末尾自动显示“+”号，点击右上角删除图片。

智能防呆：禁止拖拽“+”号按钮，禁止将图片覆盖到“+”号上。

2. 富文本编辑

话题与用户：点击底部工具栏插入 #话题 (黄色高亮) 和 @用户 (蓝色高亮)。

整体删除：支持 Atomic Deletion，删除话题或用户名时会自动整体删除，模拟原生输入体验。

字数统计：实时统计字数，超过 2000 字时变红并弹窗提示。

3. 模拟 AI 智能配文

异步模拟：点击“AI配文”按钮，模拟网络请求延迟 (1.5s)。

自动填充：自动生成一段生活感悟文案，并自动附带相关 #话题 标签。

4. 位置服务 (LBS)

精准定位：使用 LocationManagerCompat 获取经纬度。

逆地理编码：通过 Geocoder 将坐标自动转换为“城市 · 区县”格式 (如：北京市 · 朝阳区)。

模拟器优化：针对模拟器 GPS 缓存为空的情况，增加了主动请求单次定位的逻辑 (requestSingleUpdate)。 

 

🛠️ 技术栈

语言：Java (JDK 11)

UI 组件：

RecyclerView + GridLayoutManager (网格布局)

ConstraintLayout (复杂界面布局)

SpannableString (富文本高亮)

图片加载：Glide 4.16.0

系统服务：LocationManager (GPS), Geocoder

多线程：Handler (UI更新), Thread (耗时操作)

📂 项目结构

com.example.tiktok_publish_demo_202511
├── ImageAdapter.java      // 图片列表适配器 (处理多 Type 视图、拖拽逻辑)
├── PostActivity.java      // 主页面 (处理定位、AI逻辑、富文本交互)
└── ...
 

🚀 如何运行

克隆项目到本地：

git clone [https://github.com/kramQAQ/TikTok-Publish-Demo.git](https://github.com/kramQAQ/TikTok-Publish-Demo.git)


使用 Android Studio Iguana 或更新版本打开项目。

等待 Gradle Sync 完成 (已配置阿里云镜像，下载速度快)。

连接真机或使用模拟器 (推荐 API 34+)。

点击 Run 运行。
 

 
Created by Kram | 2025
